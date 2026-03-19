package com.examscheduler.api.dto;

import java.util.List;

public record GenerateScheduleResponse(
    String message,
    int totalExams,
    int scheduledExams,
    int unplacedExams,
    List<ScheduleRowResponse> rows
) {
}
