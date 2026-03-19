package com.examscheduler.controller;

import java.sql.SQLException;
import java.util.List;

import com.examscheduler.model.ScheduledExam;
import com.examscheduler.service.SchedulerService;

/**
 * Thin controller - receives CLI commands, delegates to service layer.
 * Never contains business logic. Never touches the database directly.
 */
public class ScheduleController {

    private final SchedulerService schedulerService = new SchedulerService();

    /**
     * Triggers the full scheduling pipeline and returns results to the caller.
     * @throws SQLException if DB access fails at any point
     */
    public List<ScheduledExam> generateTimetable() throws SQLException {
        return schedulerService.generateSchedule();
    }
}
