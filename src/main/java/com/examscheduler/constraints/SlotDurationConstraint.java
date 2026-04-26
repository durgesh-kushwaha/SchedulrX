package com.examscheduler.constraints;

import java.util.List;

import com.examscheduler.model.ScheduledExam;

public class SlotDurationConstraint implements Constraint {

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        if (candidate == null || candidate.getExam() == null || candidate.getSlot() == null) {
            return true;
        }
        return candidate.getSlot().durationMinutes() >= candidate.getExam().getDurationMinutes();
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        String code = candidate != null && candidate.getExam() != null
            ? candidate.getExam().getSubjectCode()
            : "UNKNOWN";
        long slotMinutes = candidate != null && candidate.getSlot() != null
            ? candidate.getSlot().durationMinutes()
            : 0L;
        int examMinutes = candidate != null && candidate.getExam() != null
            ? candidate.getExam().getDurationMinutes()
            : 0;
        return "Exam '" + code + "' needs " + examMinutes + " minutes but the selected slot only offers " + slotMinutes + " minutes";
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.HARD;
    }

    @Override
    public String getName() {
        return "SlotDuration";
    }
}
