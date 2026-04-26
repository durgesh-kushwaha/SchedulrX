package com.examscheduler.api.dto;

public record PlanningDatasetResponse(
    String message,
    PlanningDatasetDto dataset,
    PlanningReadinessResponse readiness
) {
}
