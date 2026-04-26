package com.examscheduler.api.dto;

import java.util.List;

public record PlanningExamDto(
    Integer id,
    String subjectName,
    String subjectCode,
    Integer durationMinutes,
    String priority,
    Integer teacherId,
    String department,
    String examType,
    Boolean requiresProjector,
    Boolean requiresComputers,
    String preferredSession,
    Integer difficultyLevel,
    List<Integer> studentIds
) {
}
