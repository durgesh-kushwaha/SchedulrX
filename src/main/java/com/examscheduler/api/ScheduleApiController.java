package com.examscheduler.api;

import java.sql.SQLException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examscheduler.api.dto.GenerateScheduleResponse;
import com.examscheduler.api.dto.PagedResponse;
import com.examscheduler.api.dto.ScheduleOverrideRequest;
import com.examscheduler.api.dto.ScheduleOverrideResponse;
import com.examscheduler.api.dto.ScheduleRowResponse;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.service.ScheduleApiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleApiController {

    private final ScheduleApiService scheduleApiService;

    public ScheduleApiController(ScheduleApiService scheduleApiService) {
        this.scheduleApiService = scheduleApiService;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateScheduleResponse> generate(Authentication authentication) throws SQLException {
        String actor = authentication != null ? authentication.getName() : "system";
        List<ScheduledExam> result = scheduleApiService.generate(actor);
        List<ScheduleRowResponse> rows = result.stream().map(this::toRow).toList();

        int scheduled = (int) result.stream().filter(ScheduledExam::isScheduled).count();
        int unplaced = result.size() - scheduled;

        return ResponseEntity.ok(new GenerateScheduleResponse(
            "Schedule generated successfully",
            result.size(),
            scheduled,
            unplaced,
            rows
        ));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ScheduleRowResponse>> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String teacher,
        @RequestParam(required = false) String subject,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) throws SQLException {
        PagedResponse<ScheduledExam> result = scheduleApiService.list(status, teacher, subject, page, size);
        List<ScheduleRowResponse> rows = result.items().stream().map(this::toRow).toList();
        return ResponseEntity.ok(new PagedResponse<>(rows, result.page(), result.size(), result.total()));
    }

    @PostMapping("/override")
    public ResponseEntity<ScheduleOverrideResponse> overrideSchedule(
        @Valid @RequestBody ScheduleOverrideRequest request,
        Authentication authentication
    ) throws SQLException {
        String actor = authentication != null ? authentication.getName() : "system";
        ScheduledExam row = scheduleApiService.overrideSchedule(request, actor);
        return ResponseEntity.ok(new ScheduleOverrideResponse("Override applied successfully", toRow(row)));
    }

    private ScheduleRowResponse toRow(ScheduledExam se) {
        String teacher = se.getExam() != null && se.getExam().getTeacher() != null
            ? se.getExam().getTeacher().getName()
            : "TBD";

        String examDate = se.getSlot() != null && se.getSlot().getExamDate() != null
            ? se.getSlot().getExamDate().toString()
            : "-";

        String startTime = se.getSlot() != null && se.getSlot().getStartTime() != null
            ? se.getSlot().getStartTime().toString()
            : "-";

        String endTime = se.getSlot() != null && se.getSlot().getEndTime() != null
            ? se.getSlot().getEndTime().toString()
            : "-";

        String room = se.getRoom() != null ? se.getRoom().getName() : "-";

        return new ScheduleRowResponse(
            se.getExam() != null ? se.getExam().getId() : 0,
            se.getExam() != null ? se.getExam().getSubjectCode() : "UNKNOWN",
            se.getExam() != null ? se.getExam().getSubjectName() : "Unknown Subject",
            teacher,
            examDate,
            startTime,
            endTime,
            room,
            se.getStatus().name(),
            se.getConflictReason() == null ? "" : se.getConflictReason()
        );
    }
}
