package com.examscheduler.api.dto;

import java.util.List;

public record AnalyticsOverviewResponse(
    long totalExams,
    long scheduledExams,
    long unplacedExams,
    List<NamedCount> roomUtilization,
    List<NamedCount> teacherLoad
) {
    public record NamedCount(String name, long count) {}
}
