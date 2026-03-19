package com.examscheduler.dao;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.examscheduler.model.Exam;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.Teacher;
import com.examscheduler.model.TimeSlot;

/**
 * Persists and retrieves scheduling data in MongoDB.
 */
public class ScheduleDAO extends BaseDAO {

    public record NamedCount(String name, long count) {}
    public record AuditLogRow(long id, String actionType, String actionDetails, String entityType,
                              String entityId, String actorUsername, Instant createdAt) {}

    public void saveSchedule(List<ScheduledExam> schedule) throws SQLException {
        try {
            collection("scheduled_exam").deleteMany(new Document());

            List<Document> docs = new ArrayList<>();
            for (ScheduledExam se : schedule) {
                Document row = new Document()
                    .append("id", nextSequence("scheduled_exam"))
                    .append("examId", se.getExam() == null ? 0 : se.getExam().getId())
                    .append("status", se.getStatus().name());

                if (se.isScheduled() && se.getSlot() != null && se.getRoom() != null) {
                    row.append("slotId", se.getSlot().getId());
                    row.append("roomId", se.getRoom().getId());
                } else {
                    row.append("slotId", null);
                    row.append("roomId", null);
                }

                docs.add(row);
            }

            if (!docs.isEmpty()) {
                collection("scheduled_exam").insertMany(docs);
            }

            System.out.println("[DAO] Schedule saved: " + schedule.size() + " records.");
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to save schedule in MongoDB", ex);
        }
    }

    public List<ScheduledExam> findAllScheduledExams() throws SQLException {
        return findScheduledExams(null, null, null, null, null);
    }

    public List<ScheduledExam> findScheduledExams(String status,
                                                  String teacherLike,
                                                  String subjectLike,
                                                  Integer page,
                                                  Integer size) throws SQLException {
        try {
            List<ScheduledExam> rows = loadAllMappedRows();
            rows = filterRows(rows, status, teacherLike, subjectLike);
            rows.sort(scheduleSortComparator());

            if (page == null || size == null) {
                return rows;
            }

            int from = Math.max(0, page * size);
            int to = Math.min(rows.size(), from + size);
            if (from >= rows.size()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(rows.subList(from, to));
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to read schedule rows from MongoDB", ex);
        }
    }

    public long countScheduledExams(String status, String teacherLike, String subjectLike) throws SQLException {
        try {
            return filterRows(loadAllMappedRows(), status, teacherLike, subjectLike).size();
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to count schedule rows from MongoDB", ex);
        }
    }

    public ScheduledExam findByExamId(int examId) throws SQLException {
        try {
            Document row = collection("scheduled_exam").find(new Document("examId", examId)).first();
            if (row == null) {
                row = collection("scheduled_exam").find(new Document("exam_id", examId)).first();
            }
            if (row == null) {
                return null;
            }

            Map<Integer, Exam> examMap = buildExamMap();
            Map<Integer, TimeSlot> slotMap = buildSlotMap();
            Map<Integer, Room> roomMap = buildRoomMap();

            return mapScheduledExam(row, examMap, slotMap, roomMap);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to find exam schedule row in MongoDB", ex);
        }
    }

    public void overridePlacement(int examId, int slotId, int roomId) throws SQLException {
        try {
            collection("scheduled_exam").updateOne(
                new Document("examId", examId),
                new Document("$set", new Document("slotId", slotId)
                    .append("roomId", roomId)
                    .append("status", "SCHEDULED"))
            );
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to override placement in MongoDB", ex);
        }
    }

    public void saveOverride(int examId, Integer oldSlotId, Integer oldRoomId,
                             int newSlotId, int newRoomId,
                             String reason, String approvedBy) throws SQLException {
        try {
            Document row = new Document()
                .append("id", nextSequence("exam_override"))
                .append("examId", examId)
                .append("oldSlotId", oldSlotId)
                .append("oldRoomId", oldRoomId)
                .append("newSlotId", newSlotId)
                .append("newRoomId", newRoomId)
                .append("reason", reason)
                .append("approvedBy", approvedBy)
                .append("createdAt", Date.from(Instant.now()));
            collection("exam_override").insertOne(row);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to persist override in MongoDB", ex);
        }
    }

    public void saveAuditLog(Long scheduleRunId,
                             String actionType,
                             String actionDetails,
                             String entityType,
                             String entityId,
                             String actorUsername) throws SQLException {
        try {
            Document row = new Document()
                .append("id", nextSequence("schedule_audit_log"))
                .append("scheduleRunId", scheduleRunId)
                .append("actionType", actionType)
                .append("actionDetails", actionDetails)
                .append("entityType", entityType)
                .append("entityId", entityId)
                .append("actorUsername", actorUsername)
                .append("createdAt", Date.from(Instant.now()));
            collection("schedule_audit_log").insertOne(row);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to persist audit log in MongoDB", ex);
        }
    }

    public List<AuditLogRow> findAuditLogs(int page, int size) throws SQLException {
        try {
            List<Document> docs = new ArrayList<>();
            for (Document d : collection("schedule_audit_log").find()) {
                docs.add(d);
            }
            docs.sort(Comparator.comparing(this::createdAt).reversed());

            int from = Math.max(0, page * size);
            int to = Math.min(docs.size(), from + size);
            if (from >= docs.size()) {
                return new ArrayList<>();
            }

            List<AuditLogRow> rows = new ArrayList<>();
            for (Document d : docs.subList(from, to)) {
                rows.add(new AuditLogRow(
                    getLong(d, "id", 0L),
                    firstString(d, "actionType", "action_type"),
                    firstString(d, "actionDetails", "action_details"),
                    firstString(d, "entityType", "entity_type"),
                    firstString(d, "entityId", "entity_id"),
                    firstString(d, "actorUsername", "actor_username"),
                    createdAt(d)
                ));
            }
            return rows;
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to read audit logs from MongoDB", ex);
        }
    }

    public long countAuditLogs() throws SQLException {
        try {
            return collection("schedule_audit_log").countDocuments();
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to count audit logs from MongoDB", ex);
        }
    }

    public Long createScheduleRun(String status, String triggerUsername) throws SQLException {
        try {
            long id = nextSequence("schedule_run");
            Document row = new Document()
                .append("id", id)
                .append("status", status)
                .append("triggerUsername", triggerUsername)
                .append("startedAt", Date.from(Instant.now()));
            collection("schedule_run").insertOne(row);
            return id;
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to create schedule run in MongoDB", ex);
        }
    }

    public void completeScheduleRun(long id,
                                    String status,
                                    int hardConflicts,
                                    int softPenalty,
                                    int unplaced) throws SQLException {
        try {
            collection("schedule_run").updateOne(
                new Document("id", id),
                new Document("$set", new Document("finishedAt", Date.from(Instant.now()))
                    .append("status", status)
                    .append("hardConflicts", hardConflicts)
                    .append("softPenalty", softPenalty)
                    .append("unplacedExams", unplaced))
            );
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to complete schedule run in MongoDB", ex);
        }
    }

    public List<NamedCount> roomUtilization() throws SQLException {
        try {
            Map<Integer, Room> roomMap = buildRoomMap();
            Map<String, Long> counts = new HashMap<>();

            for (Document row : collection("scheduled_exam").find()) {
                String status = firstString(row, "status");
                if (!"SCHEDULED".equalsIgnoreCase(status)) {
                    continue;
                }
                int roomId = firstInt(row, "roomId", "room_id");
                Room room = roomMap.get(roomId);
                if (room != null) {
                    counts.merge(room.getName(), 1L, Long::sum);
                }
            }

            return sortNamedCounts(counts);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to compute room utilization from MongoDB", ex);
        }
    }

    public List<NamedCount> teacherLoad() throws SQLException {
        try {
            Map<Integer, Exam> examMap = buildExamMap();
            Map<String, Long> counts = new HashMap<>();

            for (Document row : collection("scheduled_exam").find()) {
                String status = firstString(row, "status");
                if (!"SCHEDULED".equalsIgnoreCase(status)) {
                    continue;
                }
                int examId = firstInt(row, "examId", "exam_id");
                Exam exam = examMap.get(examId);
                if (exam != null && exam.getTeacher() != null) {
                    counts.merge(exam.getTeacher().getName(), 1L, Long::sum);
                }
            }

            return sortNamedCounts(counts);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to compute teacher load from MongoDB", ex);
        }
    }

    public long totalExams() throws SQLException {
        try {
            return collection("exam").countDocuments();
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to count exams from MongoDB", ex);
        }
    }

    public long scheduledExams() throws SQLException {
        try {
            return collection("scheduled_exam").countDocuments(new Document("status", "SCHEDULED"));
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to count scheduled exams from MongoDB", ex);
        }
    }

    public long unplacedExams() throws SQLException {
        try {
            long total = collection("scheduled_exam").countDocuments();
            long scheduled = scheduledExams();
            return Math.max(0L, total - scheduled);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to count unplaced exams from MongoDB", ex);
        }
    }

    private List<ScheduledExam> loadAllMappedRows() {
        Map<Integer, Exam> examMap = buildExamMap();
        Map<Integer, TimeSlot> slotMap = buildSlotMap();
        Map<Integer, Room> roomMap = buildRoomMap();

        List<ScheduledExam> rows = new ArrayList<>();
        for (Document row : collection("scheduled_exam").find()) {
            rows.add(mapScheduledExam(row, examMap, slotMap, roomMap));
        }
        return rows;
    }

    private List<ScheduledExam> filterRows(List<ScheduledExam> rows,
                                           String status,
                                           String teacherLike,
                                           String subjectLike) {
        String normalizedStatus = normalize(status);
        String normalizedTeacher = normalize(teacherLike);
        String normalizedSubject = normalize(subjectLike);

        List<ScheduledExam> out = new ArrayList<>();
        for (ScheduledExam row : rows) {
            if (normalizedStatus != null && !row.getStatus().name().equalsIgnoreCase(normalizedStatus)) {
                continue;
            }

            String teacher = row.getExam() != null && row.getExam().getTeacher() != null
                ? row.getExam().getTeacher().getName()
                : "";
            if (normalizedTeacher != null && !containsIgnoreCase(teacher, normalizedTeacher)) {
                continue;
            }

            String subjectName = row.getExam() != null ? row.getExam().getSubjectName() : "";
            String subjectCode = row.getExam() != null ? row.getExam().getSubjectCode() : "";
            if (normalizedSubject != null
                && !containsIgnoreCase(subjectName, normalizedSubject)
                && !containsIgnoreCase(subjectCode, normalizedSubject)) {
                continue;
            }

            out.add(row);
        }
        return out;
    }

    private Comparator<ScheduledExam> scheduleSortComparator() {
        return Comparator
            .comparing((ScheduledExam se) -> se.getSlot() != null ? se.getSlot().getExamDate() : null,
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(se -> se.getSlot() != null ? se.getSlot().getStartTime() : null,
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(se -> se.getExam() != null && se.getExam().getSubjectCode() != null
                ? se.getExam().getSubjectCode() : "");
    }

    private Map<Integer, Teacher> buildTeacherMap() {
        Map<Integer, Teacher> map = new HashMap<>();
        for (Document t : collection("teacher").find()) {
            int id = getInt(t, "id", 0);
            map.put(id, new Teacher(
                id,
                firstString(t, "name", "teacher_name"),
                firstString(t, "department"),
                firstString(t, "email")
            ));
        }
        return map;
    }

    private Map<Integer, Exam> buildExamMap() {
        Map<Integer, Teacher> teacherMap = buildTeacherMap();
        Map<Integer, Exam> map = new HashMap<>();
        for (Document e : collection("exam").find()) {
            int id = getInt(e, "id", 0);
            int teacherId = firstInt(e, "teacherId", "teacher_id");

            String priorityRaw = firstString(e, "priority");
            Exam.Priority priority = Exam.Priority.CORE;
            if (priorityRaw != null && !priorityRaw.isBlank()) {
                priority = Exam.Priority.valueOf(priorityRaw.toUpperCase());
            }

            Exam exam = new Exam(
                id,
                firstString(e, "subjectName", "subject_name"),
                firstString(e, "subjectCode", "subject_code"),
                firstInt(e, "durationMinutes", "duration_minutes"),
                priority,
                teacherMap.get(teacherId)
            );
            map.put(id, exam);
        }
        return map;
    }

    private Map<Integer, TimeSlot> buildSlotMap() {
        Map<Integer, TimeSlot> map = new HashMap<>();
        for (Document s : collection("time_slot").find()) {
            int id = getInt(s, "id", 0);
            String date = firstString(s, "examDate", "exam_date");
            String start = firstString(s, "startTime", "start_time");
            String end = firstString(s, "endTime", "end_time");
            map.put(id, new TimeSlot(
                id,
                date == null ? null : LocalDate.parse(date),
                start == null ? null : LocalTime.parse(start),
                end == null ? null : LocalTime.parse(end)
            ));
        }
        return map;
    }

    private Map<Integer, Room> buildRoomMap() {
        Map<Integer, Room> map = new HashMap<>();
        for (Document r : collection("room").find()) {
            int id = getInt(r, "id", 0);
            map.put(id, new Room(
                id,
                firstString(r, "name"),
                firstInt(r, "capacity"),
                firstBoolean(r, "hasProjector", "has_projector"),
                firstString(r, "building")
            ));
        }
        return map;
    }

    private ScheduledExam mapScheduledExam(Document rs,
                                           Map<Integer, Exam> examMap,
                                           Map<Integer, TimeSlot> slotMap,
                                           Map<Integer, Room> roomMap) {
        int examId = firstInt(rs, "examId", "exam_id");
        Exam exam = examMap.get(examId);
        if (exam == null) {
            exam = new Exam();
            exam.setId(examId);
            exam.setSubjectCode("UNKNOWN");
            exam.setSubjectName("Unknown exam");
        }

        String statusRaw = firstString(rs, "status", "se_status");
        ScheduledExam.Status status = ScheduledExam.Status.SCHEDULED;
        if (statusRaw != null && !statusRaw.isBlank()) {
            status = ScheduledExam.Status.valueOf(statusRaw.toUpperCase());
        }

        ScheduledExam se;
        if (status == ScheduledExam.Status.SCHEDULED) {
            int slotId = firstInt(rs, "slotId", "slot_id");
            int roomId = firstInt(rs, "roomId", "room_id");
            TimeSlot slot = slotMap.get(slotId);
            Room room = roomMap.get(roomId);
            se = new ScheduledExam(exam, slot, room);
        } else {
            String reason = firstString(rs, "conflictReason", "conflict_reason");
            if (reason == null || reason.isBlank()) {
                reason = "Unplaced by scheduler";
            }
            se = new ScheduledExam(exam, status, reason);
        }

        se.setId(firstInt(rs, "id", "se_id"));
        return se;
    }

    private List<NamedCount> sortNamedCounts(Map<String, Long> counts) {
        List<NamedCount> rows = new ArrayList<>();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            rows.add(new NamedCount(e.getKey(), e.getValue()));
        }
        rows.sort(Comparator.comparingLong(NamedCount::count).reversed());
        return rows;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean containsIgnoreCase(String text, String search) {
        return text != null && text.toLowerCase().contains(search.toLowerCase());
    }

    private int firstInt(Document doc, String primary, String fallback) {
        Object first = doc.get(primary);
        if (first instanceof Number n) {
            return n.intValue();
        }
        Object second = doc.get(fallback);
        if (second instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private int firstInt(Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private String firstString(Document doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private boolean firstBoolean(Document doc, String primary, String fallback) {
        Object first = doc.get(primary);
        if (first instanceof Boolean b) {
            return b;
        }
        Object second = doc.get(fallback);
        if (second instanceof Boolean b) {
            return b;
        }
        return false;
    }

    private Instant createdAt(Document doc) {
        Object value = doc.get("createdAt");
        if (value == null) {
            value = doc.get("created_at");
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        return Instant.EPOCH;
    }
}
