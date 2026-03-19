package com.examscheduler.constraints;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.examscheduler.model.ScheduledExam;
import com.examscheduler.util.ScheduleConfig;

/**
 * SOFT CONSTRAINT - Students should have a minimum gap between exams.
 *
 * Soft means: if violated, we don't backtrack, but the optimizer
 * will try to fix it in the post-solve improvement pass.
 *
 * Gap is measured as: |slot1.end - slot2.start| on the same date.
 * Cross-day exams automatically satisfy this constraint.
 */
public class MinGapConstraint implements Constraint {

    private final Map<Integer, Set<Integer>> studentExamMap;
    private final int minGapMinutes;

    public MinGapConstraint(Map<Integer, Set<Integer>> studentExamMap) {
        this.studentExamMap = studentExamMap;
        this.minGapMinutes = ScheduleConfig.MIN_GAP_MINUTES;
    }

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        if (minGapMinutes <= 0) return true; // gap checking disabled

        Set<Integer> candidateStudents = studentExamMap.get(candidate.getExam().getId());
        if (candidateStudents == null || candidateStudents.isEmpty()) return true;

        for (ScheduledExam existing : currentSchedule) {
            Set<Integer> existingStudents = studentExamMap.get(existing.getExam().getId());
            if (existingStudents == null) continue;

            // Only care if they share students
            boolean shareStudents = candidateStudents.stream()
                .anyMatch(existingStudents::contains);
            if (!shareStudents) continue;

            // Check gap in both directions (candidate before or after existing)
            long gapAfter = existing.getSlot().minutesGapTo(candidate.getSlot());
            long gapBefore = candidate.getSlot().minutesGapTo(existing.getSlot());

            // If one exam ends right before another starts for shared students
            if ((gapAfter >= 0 && gapAfter < minGapMinutes) ||
                (gapBefore >= 0 && gapBefore < minGapMinutes)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> current) {
        return String.format(
            "Exam '%s' violates minimum %d-minute gap requirement for shared students",
            candidate.getExam().getSubjectCode(), minGapMinutes
        );
    }

    @Override public ConstraintType getType() { return ConstraintType.SOFT; }
    @Override public String getName() { return "MinGap"; }
}
