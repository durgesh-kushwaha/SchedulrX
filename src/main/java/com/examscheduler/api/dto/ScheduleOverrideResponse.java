package com.examscheduler.api.dto;

public record ScheduleOverrideResponse(
    String message,
    ScheduleRowResponse row
) {}
