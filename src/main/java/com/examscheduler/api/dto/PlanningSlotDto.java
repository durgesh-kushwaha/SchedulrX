package com.examscheduler.api.dto;

public record PlanningSlotDto(
    Integer id,
    String label,
    String examDate,
    String startTime,
    String endTime
) {
}
