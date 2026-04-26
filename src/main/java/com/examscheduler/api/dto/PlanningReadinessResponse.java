package com.examscheduler.api.dto;

import java.util.List;

public record PlanningReadinessResponse(
    boolean ready,
    int teacherCount,
    int roomCount,
    int slotCount,
    int studentCount,
    int examCount,
    int enrollmentCount,
    List<String> blockingIssues,
    List<String> advisoryNotes
) {
}
