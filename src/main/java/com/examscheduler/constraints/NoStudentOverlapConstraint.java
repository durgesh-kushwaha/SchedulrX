package com.examscheduler.constraints;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.examscheduler.model.ScheduledExam;

/**
 * HARD CONSTRAINT - No student should have two exams at the same time.
 *
 * Implementation: For each already-scheduled exam that overlaps in time
 * with the candidate, check if any student is enrolled in BOTH exams.
 *
 * The studentExamMap is pre-built: Map<examId, Set<studentId>>
 * This avoids repeated DB hits during backtracking.
 */
public class NoStudentOverlapConstraint implements Constraint {

    // examId -> set of enrolled studentIds
    // Injected by the SchedulerService before solving begins
    private final Map<Integer, Set<Integer>> studentExamMap;

    public NoStudentOverlapConstraint(Map<Integer, Set<Integer>> studentExamMap) {
        this.studentExamMap = studentExamMap;
    }

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        Set<Integer> candidateStudents = studentExamMap.get(candidate.getExam().getId());
        if (candidateStudents == null || candidateStudents.isEmpty()) return true;

        for (ScheduledExam existing : currentSchedule) {
            // Only check exams whose time slots actually overlap
            if (!existing.getSlot().overlaps(candidate.getSlot())) continue;

            Set<Integer> existingStudents = studentExamMap.get(existing.getExam().getId());
            if (existingStudents == null) continue;

            // Intersection check - any shared student = constraint violated
            for (Integer studentId : candidateStudents) {
                if (existingStudents.contains(studentId)) {
                    return false; // found a clash
                }
            }
        }
        return true;
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        Set<Integer> candidateStudents = studentExamMap.get(candidate.getExam().getId());

        for (ScheduledExam existing : currentSchedule) {
            if (!existing.getSlot().overlaps(candidate.getSlot())) continue;

            Set<Integer> existingStudents = studentExamMap.get(existing.getExam().getId());
            if (existingStudents == null) continue;

            for (Integer studentId : candidateStudents) {
                if (existingStudents.contains(studentId)) {
                    return String.format(
                        "Student ID %d is enrolled in both '%s' and '%s' - both in slot %s",
                        studentId,
                        candidate.getExam().getSubjectCode(),
                        existing.getExam().getSubjectCode(),
                        candidate.getSlot()
                    );
                }
            }
        }
        return "Student overlap detected (details unavailable)";
    }

    @Override public ConstraintType getType() { return ConstraintType.HARD; }
    @Override public String getName() { return "NoStudentOverlap"; }
}
