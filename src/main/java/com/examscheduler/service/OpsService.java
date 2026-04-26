package com.examscheduler.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.examscheduler.api.dto.AnalyticsOverviewResponse;
import com.examscheduler.api.dto.AuditLogResponse;
import com.examscheduler.api.dto.NotificationResponse;
import com.examscheduler.api.dto.PagedResponse;
import com.examscheduler.api.dto.ScheduleOverrideRequest;
import com.examscheduler.dao.ExamDAO;
import com.examscheduler.dao.NotificationDAO;
import com.examscheduler.dao.RoomDAO;
import com.examscheduler.dao.ScheduleDAO;
import com.examscheduler.dao.SlotDAO;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.TimeSlot;
import com.examscheduler.realtime.ScheduleEventPublisher;
import com.examscheduler.util.ConstraintViolationException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class OpsService {

    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final SlotDAO slotDAO = new SlotDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final ConstraintValidationService constraintValidationService = new ConstraintValidationService();

    private final ScheduleEventPublisher eventPublisher;

    public OpsService(ScheduleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public PagedResponse<ScheduledExam> listSchedules(String status,
                                                      String teacher,
                                                      String subject,
                                                      int page,
                                                      int size) throws SQLException {
        List<ScheduledExam> items = scheduleDAO.findScheduledExams(status, teacher, subject, page, size);
        long total = scheduleDAO.countScheduledExams(status, teacher, subject);
        return new PagedResponse<>(items, page, size, total);
    }

    public ScheduledExam overridePlacement(ScheduleOverrideRequest request, String actor) throws SQLException {
        ScheduledExam existing = scheduleDAO.findByExamId(request.examId());
        if (existing == null) {
            throw new IllegalArgumentException("Exam not found in current schedule: " + request.examId());
        }

        TimeSlot targetSlot = slotDAO.findById(request.newSlotId());
        if (targetSlot == null) {
            throw new IllegalArgumentException("Slot not found: " + request.newSlotId());
        }

        Room targetRoom = roomDAO.findById(request.newRoomId());
        if (targetRoom == null) {
            throw new IllegalArgumentException("Room not found: " + request.newRoomId());
        }

        existing.getExam().setEnrollmentCount(resolveEnrollmentCount(existing.getExam().getId()));

        ScheduledExam candidate = new ScheduledExam(existing.getExam(), targetSlot, targetRoom);
        List<ScheduledExam> current = scheduleDAO.findAllScheduledExams();
        List<ScheduledExam> context = new ArrayList<>();
        for (ScheduledExam se : current) {
            if (se.getExam() != null && se.getExam().getId() != request.examId() && se.isScheduled()) {
                context.add(se);
            }
        }

        String violation = constraintValidationService.firstViolation(candidate, context, examDAO.buildStudentExamMap());
        if (violation != null) {
            throw new ConstraintViolationException("MANUAL_OVERRIDE", violation);
        }

        Integer oldSlotId = existing.getSlot() == null ? null : existing.getSlot().getId();
        Integer oldRoomId = existing.getRoom() == null ? null : existing.getRoom().getId();

        scheduleDAO.overridePlacement(request.examId(), request.newSlotId(), request.newRoomId());
        scheduleDAO.saveOverride(request.examId(), oldSlotId, oldRoomId,
            request.newSlotId(), request.newRoomId(), request.reason(), actor);
        scheduleDAO.saveAuditLog(null,
            "OVERRIDE",
            "Exam " + request.examId() + " moved to slot " + request.newSlotId() + " room " + request.newRoomId(),
            "EXAM",
            String.valueOf(request.examId()),
            actor);

        notificationDAO.notifyAllUsers(
            "OVERRIDE",
            "Schedule Override",
            "Exam " + request.examId() + " was manually moved by " + actor
        );

        eventPublisher.publish("schedule.override", "Exam " + request.examId() + " overridden");

        return scheduleDAO.findByExamId(request.examId());
    }

    public PagedResponse<AuditLogResponse> auditLogs(int page, int size) throws SQLException {
        List<AuditLogResponse> items = scheduleDAO.findAuditLogs(page, size).stream()
            .map(r -> new AuditLogResponse(
                r.id(), r.actionType(), r.actionDetails(), r.entityType(), r.entityId(), r.actorUsername(), r.createdAt()
            ))
            .toList();
        long total = scheduleDAO.countAuditLogs();
        return new PagedResponse<>(items, page, size, total);
    }

    public AnalyticsOverviewResponse analyticsOverview() throws SQLException {
        List<AnalyticsOverviewResponse.NamedCount> room = scheduleDAO.roomUtilization().stream()
            .map(r -> new AnalyticsOverviewResponse.NamedCount(r.name(), r.count()))
            .toList();
        List<AnalyticsOverviewResponse.NamedCount> teachers = scheduleDAO.teacherLoad().stream()
            .map(r -> new AnalyticsOverviewResponse.NamedCount(r.name(), r.count()))
            .toList();

        return new AnalyticsOverviewResponse(
            scheduleDAO.totalExams(),
            scheduleDAO.scheduledExams(),
            scheduleDAO.unplacedExams(),
            room,
            teachers
        );
    }

    public byte[] exportCsv() throws SQLException {
        List<ScheduledExam> rows = scheduleDAO.findAllScheduledExams();
        StringBuilder sb = new StringBuilder();
        sb.append("examId,subjectCode,subjectName,teacher,date,start,end,room,status,conflict\n");
        for (ScheduledExam se : rows) {
            String examId = String.valueOf(se.getExam() != null ? se.getExam().getId() : 0);
            String code = se.getExam() != null ? sanitize(se.getExam().getSubjectCode()) : "";
            String name = se.getExam() != null ? sanitize(se.getExam().getSubjectName()) : "";
            String teacher = se.getExam() != null && se.getExam().getTeacher() != null ? sanitize(se.getExam().getTeacher().getName()) : "";
            String date = se.getSlot() != null && se.getSlot().getExamDate() != null ? se.getSlot().getExamDate().toString() : "";
            String start = se.getSlot() != null && se.getSlot().getStartTime() != null ? se.getSlot().getStartTime().toString() : "";
            String end = se.getSlot() != null && se.getSlot().getEndTime() != null ? se.getSlot().getEndTime().toString() : "";
            String room = se.getRoom() != null ? sanitize(se.getRoom().getName()) : "";
            String status = se.getStatus().name();
            String conflict = sanitize(se.getConflictReason() == null ? "" : se.getConflictReason());
            sb.append(String.join(",", examId, csv(code), csv(name), csv(teacher), date, start, end, csv(room), status, csv(conflict))).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf() throws SQLException {
        List<ScheduledExam> rows = scheduleDAO.findAllScheduledExams();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); Document document = new Document()) {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Smart Exam Schedule"));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.addCell("Code");
            table.addCell("Subject");
            table.addCell("Date");
            table.addCell("Time");
            table.addCell("Room");
            table.addCell("Status");

            for (ScheduledExam se : rows) {
                table.addCell(se.getExam() != null ? sanitize(se.getExam().getSubjectCode()) : "-");
                table.addCell(se.getExam() != null ? sanitize(se.getExam().getSubjectName()) : "-");
                table.addCell(se.getSlot() != null && se.getSlot().getExamDate() != null ? se.getSlot().getExamDate().toString() : "-");
                table.addCell(
                    (se.getSlot() != null && se.getSlot().getStartTime() != null ? se.getSlot().getStartTime().toString() : "-")
                        + " - " +
                    (se.getSlot() != null && se.getSlot().getEndTime() != null ? se.getSlot().getEndTime().toString() : "-")
                );
                table.addCell(se.getRoom() != null ? sanitize(se.getRoom().getName()) : "-");
                table.addCell(se.getStatus().name());
            }

            document.add(table);
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException ex) {
            throw new SQLException("Unable to generate PDF export", ex);
        }
    }

    public PagedResponse<NotificationResponse> notificationsForUser(String username, int page, int size) throws SQLException {
        List<NotificationResponse> items = notificationDAO.findByUsername(username, page, size);
        long total = notificationDAO.countByUsername(username);
        return new PagedResponse<>(items, page, size, total);
    }

    public void markNotificationRead(String username, long id) throws SQLException {
        notificationDAO.markRead(id, username);
    }

    public void deleteNotification(String username, long id) throws SQLException {
        notificationDAO.deleteByIdAndUsername(id, username);
    }

    public void broadcastScheduleGenerated(String actor, int total) throws SQLException {
        notificationDAO.notifyAllUsers("SCHEDULE", "Schedule Generated", "A new schedule was generated by " + actor);
        eventPublisher.publish("schedule.generated", "Generated " + total + " rows");
    }

    private int resolveEnrollmentCount(int examId) throws SQLException {
        return examDAO.findAllWithEnrollmentCount().stream()
            .filter(e -> e.getId() == examId)
            .findFirst()
            .map(e -> e.getEnrollmentCount())
            .orElse(0);
    }

    private String sanitize(String value) {
        return value == null ? "" : value;
    }

    private String csv(String value) {
        String safe = value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
