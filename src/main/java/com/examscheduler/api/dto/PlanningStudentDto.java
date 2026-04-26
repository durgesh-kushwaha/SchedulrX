package com.examscheduler.api.dto;

public record PlanningStudentDto(
    Integer id,
    String name,
    String rollNo,
    Integer semester,
    String branch,
    Integer extraTimeMinutes,
    String specialNeedsNotes
) {
}
