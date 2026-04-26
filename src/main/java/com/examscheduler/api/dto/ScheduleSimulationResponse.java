package com.examscheduler.api.dto;

import java.util.List;

public record ScheduleSimulationResponse(
    String message,
    String requestedBy,
    String strategy,
    int minGapMinutes,
    int requestedAlternatives,
    int generatedAlternatives,
    List<ScheduleAlternativeResponse> alternatives
) {
}
