package com.examscheduler.constraints;

import java.util.List;

import com.examscheduler.model.ScheduledExam;

/**
 * Contract that every constraint must implement.
 *
 * This is the CSP (Constraint Satisfaction Problem) interface.
 * Each constraint is independent and testable in isolation.
 *
 * HARD constraints -> violation causes backtracking.
 * SOFT constraints -> violation adds a penalty score used by the optimizer.
 */
public interface Constraint {

    enum ConstraintType { HARD, SOFT }

    /**
     * @param candidate       the exam placement being evaluated
     * @param currentSchedule all already-committed placements
     * @return true if placing candidate does NOT violate this constraint
     */
    boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule);

    /** Human-readable explanation of WHY the constraint was violated. */
    String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> currentSchedule);

    ConstraintType getType();

    /** Short name used in logs and reports */
    String getName();
}
