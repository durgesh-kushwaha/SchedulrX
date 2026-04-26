package com.examscheduler.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.examscheduler.api.dto.PagedResponse;
import com.examscheduler.api.dto.ScheduleOverrideRequest;
import com.examscheduler.api.dto.ScheduleSimulationRequest;
import com.examscheduler.api.dto.ScheduleSimulationResponse;
import com.examscheduler.dao.ScheduleDAO;
import com.examscheduler.model.ScheduledExam;

@Service
public class ScheduleApiService {

    private final SchedulerService schedulerService = new SchedulerService();
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final OpsService opsService;
    private final ScheduleSimulationService scheduleSimulationService;
    private final PlanningService planningService;

    public ScheduleApiService(OpsService opsService,
                              ScheduleSimulationService scheduleSimulationService,
                              PlanningService planningService) {
        this.opsService = opsService;
        this.scheduleSimulationService = scheduleSimulationService;
        this.planningService = planningService;
    }

    public List<ScheduledExam> generate(String actorUsername) throws SQLException {
        planningService.assertReadyForGeneration();
        PlanningService.SchedulingRules rules = planningService.schedulingRules();
        Long runId = scheduleDAO.createScheduleRun("RUNNING", actorUsername);
        try {
            List<ScheduledExam> rows = schedulerService.generateSchedule(rules.minGapMinutes(), rules.maxExamsPerDay());
            int unplaced = (int) rows.stream().filter(se -> !se.isScheduled()).count();
            if (runId != null) {
                scheduleDAO.completeScheduleRun(runId, "COMPLETED", 0, 0, unplaced);
            }
            scheduleDAO.saveAuditLog(runId, "GENERATE", "Schedule generation completed", "SCHEDULE", "CURRENT", actorUsername);
            opsService.broadcastScheduleGenerated(actorUsername, rows.size());
            return rows;
        } catch (SQLException ex) {
            if (runId != null) {
                scheduleDAO.completeScheduleRun(runId, "FAILED", 0, 0, 0);
            }
            scheduleDAO.saveAuditLog(runId, "GENERATE_FAILED", ex.getMessage(), "SCHEDULE", "CURRENT", actorUsername);
            throw ex;
        }
    }

    public PagedResponse<ScheduledExam> list(String status, String teacher, String subject, int page, int size) throws SQLException {
        return opsService.listSchedules(status, teacher, subject, page, size);
    }

    public ScheduledExam overrideSchedule(ScheduleOverrideRequest request, String actorUsername) throws SQLException {
        return opsService.overridePlacement(request, actorUsername);
    }

    public ScheduleSimulationResponse simulate(ScheduleSimulationRequest request, String actorUsername) throws SQLException {
        return scheduleSimulationService.simulate(request, actorUsername);
    }
}
