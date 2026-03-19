package com.examscheduler.constraints;

import java.util.List;

import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.Teacher;

/**
 * HARD CONSTRAINT - A teacher cannot invigilate two exams simultaneously.
 * Also checks teacher's pre-marked unavailable slots.
 */
public class NoTeacherOverlapConstraint implements Constraint {

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        Teacher candidateTeacher = candidate.getExam().getTeacher();
        if (candidateTeacher == null) return true; // no teacher assigned, skip

        int slotId = candidate.getSlot().getId();

        // Check pre-marked unavailability
        if (!candidateTeacher.isAvailableFor(slotId)) return false;

        // Check against already-scheduled exams
        for (ScheduledExam existing : currentSchedule) {
            if (!existing.getSlot().overlaps(candidate.getSlot())) continue;

            Teacher existingTeacher = existing.getExam().getTeacher();
            if (existingTeacher == null) continue;

            if (existingTeacher.getId() == candidateTeacher.getId()) {
                return false; // same teacher, overlapping slots
            }
        }
        return true;
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> current) {
        Teacher t = candidate.getExam().getTeacher();
        return String.format(
            "Teacher '%s' is already assigned to another exam in slot %s",
            t != null ? t.getName() : "UNKNOWN",
            candidate.getSlot()
        );
    }

    @Override public ConstraintType getType() { return ConstraintType.HARD; }
    @Override public String getName() { return "NoTeacherOverlap"; }
}
