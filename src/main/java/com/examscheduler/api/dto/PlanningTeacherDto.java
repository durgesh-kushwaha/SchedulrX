package com.examscheduler.api.dto;

import java.util.List;

public record PlanningTeacherDto(
    Integer id,
    String name,
    String department,
    String email,
    List<Integer> unavailableSlotIds
) {
}
