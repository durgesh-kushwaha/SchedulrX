package com.examscheduler.api.dto;

import java.util.List;

public record PlanningDatasetDto(
    PlanningConfigDto config,
    List<PlanningTeacherDto> teachers,
    List<PlanningRoomDto> rooms,
    List<PlanningSlotDto> slots,
    List<PlanningStudentDto> students,
    List<PlanningExamDto> exams
) {
}
