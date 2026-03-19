package com.examscheduler.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ScheduleOverrideRequest(
    @Min(1) int examId,
    @Min(1) int newSlotId,
    @Min(1) int newRoomId,
    @NotBlank String reason
) {}
