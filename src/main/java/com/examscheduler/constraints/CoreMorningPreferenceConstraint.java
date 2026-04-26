package com.examscheduler.constraints;

import java.time.LocalTime;
import java.util.List;

import com.examscheduler.model.Exam;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.util.ScheduleConfig;

/**
 * SOFT CONSTRAINT - Prefer CORE subjects in morning slots.
 *
 * This is a preference only; it does not block scheduling.
 */
public class CoreMorningPreferenceConstraint implements Constraint {

    private final LocalTime morningCutoff = LocalTime.of(ScheduleConfig.CORE_MORNING_END_HOUR, 0);

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        if (candidate == null || candidate.getExam() == null || candidate.getSlot() == null) {
            return true;
        }

        if (candidate.getExam().getPriority() != Exam.Priority.CORE) {
            return true;
        }

        LocalTime startTime = candidate.getSlot().getStartTime();
        if (startTime == null) {
            return true;
        }

        return !startTime.isAfter(morningCutoff);
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        String code = candidate != null && candidate.getExam() != null
            ? candidate.getExam().getSubjectCode()
            : "UNKNOWN";
        return "CORE exam '" + code + "' is outside preferred morning window";
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.SOFT;
    }

    @Override
    public String getName() {
        return "CoreMorningPreference";
    }
}
