package com.examscheduler.api.dto;

public record ScheduleRowResponse(
    int examId,
    String subjectCode,
    String subjectName,
    String teacher,
    String examDate,
    String startTime,
    String endTime,
    String room,
    String status,
    String conflictReason
) {
}
