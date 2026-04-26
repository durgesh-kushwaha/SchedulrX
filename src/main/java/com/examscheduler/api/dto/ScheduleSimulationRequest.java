package com.examscheduler.api.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ScheduleSimulationRequest(
    @Min(1) @Max(10) Integer alternatives,
    @Min(0) Integer minGapMinutes,
    @Pattern(regexp = "^(?i)(HYBRID|GREEDY_ONLY)?$", message = "strategy must be HYBRID or GREEDY_ONLY")
    String strategy,
    @Size(max = 200) List<@Min(1) Integer> blockedRoomIds,
    @Size(max = 200) List<@Min(1) Integer> blockedSlotIds,
    @Size(max = 200) List<String> blockedDates
) {
}
