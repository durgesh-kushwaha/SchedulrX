package com.examscheduler.api.dto;

public record PlanningRoomDto(
    Integer id,
    String name,
    Integer capacity,
    Boolean hasProjector,
    Boolean hasComputers,
    String building,
    String seatingType
) {
}
