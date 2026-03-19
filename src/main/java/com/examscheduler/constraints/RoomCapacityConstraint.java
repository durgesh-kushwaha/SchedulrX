package com.examscheduler.constraints;

import java.util.List;

import com.examscheduler.model.ScheduledExam;

/**
 * HARD CONSTRAINT - Room capacity must not be exceeded.
 * Also ensures the same room is not double-booked in the same time slot.
 */
public class RoomCapacityConstraint implements Constraint {

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        int enrolled = candidate.getExam().getEnrollmentCount();

        // Rule 1: Room must fit all enrolled students
        if (!candidate.getRoom().canFit(enrolled)) return false;

        // Rule 2: Room must not already be booked at this time
        for (ScheduledExam existing : currentSchedule) {
            if (existing.getRoom().getId() == candidate.getRoom().getId()
                && existing.getSlot().overlaps(candidate.getSlot())) {
                return false; // room double-booked
            }
        }
        return true;
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> current) {
        int enrolled = candidate.getExam().getEnrollmentCount();
        int capacity = candidate.getRoom().getCapacity();

        if (!candidate.getRoom().canFit(enrolled)) {
            return String.format(
                "Room '%s' (capacity %d) is too small for %d students in '%s'",
                candidate.getRoom().getName(), capacity, enrolled,
                candidate.getExam().getSubjectCode()
            );
        }
        return String.format("Room '%s' is already booked in slot %s",
            candidate.getRoom().getName(), candidate.getSlot());
    }

    @Override public ConstraintType getType() { return ConstraintType.HARD; }
    @Override public String getName() { return "RoomCapacity"; }
}
