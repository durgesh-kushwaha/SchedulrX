package com.examscheduler.constraints;

import java.time.LocalTime;
import java.util.List;

import com.examscheduler.model.ScheduledExam;

public class PreferredSessionConstraint implements Constraint {

    private static final LocalTime AFTERNOON_START = LocalTime.NOON;

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        if (candidate == null || candidate.getExam() == null || candidate.getSlot() == null) {
            return true;
        }

        String preferredSession = candidate.getExam().getPreferredSession();
        if (preferredSession == null || preferredSession.isBlank() || candidate.getSlot().getStartTime() == null) {
            return true;
        }

        LocalTime start = candidate.getSlot().getStartTime();
        String normalized = preferredSession.trim().toUpperCase();
        if ("MORNING".equals(normalized)) {
            return start.isBefore(AFTERNOON_START);
        }
        if ("AFTERNOON".equals(normalized) || "EVENING".equals(normalized)) {
            return !start.isBefore(AFTERNOON_START);
        }
        return true;
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        String code = candidate != null && candidate.getExam() != null
            ? candidate.getExam().getSubjectCode()
            : "UNKNOWN";
        String preferred = candidate != null && candidate.getExam() != null
            ? candidate.getExam().getPreferredSession()
            : "preferred";
        return "Exam '" + code + "' was placed outside its preferred " + preferred + " session";
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.SOFT;
    }

    @Override
    public String getName() {
        return "PreferredSession";
    }
}
