package com.examscheduler.api.dto;

import java.util.List;

public record ScheduleAlternativeResponse(
    int rank,
    int totalExams,
    int scheduledExams,
    int unplacedExams,
    int softPenaltyScore,
    long runtimeMs,
    List<ScheduleRowResponse> rows
) {
}
