package com.examscheduler.api.dto;

public record PlanningConfigDto(
    String institutionName,
    String termName,
    Integer minGapMinutes,
    Integer maxExamsPerDay
) {
}
