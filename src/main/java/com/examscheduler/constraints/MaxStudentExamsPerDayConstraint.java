package com.examscheduler.constraints;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.examscheduler.model.ScheduledExam;

/**
 * SOFT CONSTRAINT - Avoid heavy same-day exam load for students.
 */
public class MaxStudentExamsPerDayConstraint implements Constraint {

    private final Map<Integer, Set<Integer>> studentExamMap;
    private final int maxExamsPerDay;

    public MaxStudentExamsPerDayConstraint(Map<Integer, Set<Integer>> studentExamMap, int maxExamsPerDay) {
        this.studentExamMap = studentExamMap;
        this.maxExamsPerDay = Math.max(1, maxExamsPerDay);
    }

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        if (candidate == null || candidate.getExam() == null || candidate.getSlot() == null) {
            return true;
        }

        Set<Integer> candidateStudents = studentExamMap.get(candidate.getExam().getId());
        if (candidateStudents == null || candidateStudents.isEmpty()) {
            return true;
        }

        LocalDate examDate = candidate.getSlot().getExamDate();
        if (examDate == null) {
            return true;
        }

        Map<Integer, Integer> sameDayCounts = new HashMap<>();

        for (ScheduledExam existing : currentSchedule) {
            if (existing == null || !existing.isScheduled() || existing.getExam() == null || existing.getSlot() == null) {
                continue;
            }
            if (!examDate.equals(existing.getSlot().getExamDate())) {
                continue;
            }

            Set<Integer> existingStudents = studentExamMap.get(existing.getExam().getId());
            if (existingStudents == null || existingStudents.isEmpty()) {
                continue;
            }

            for (Integer studentId : candidateStudents) {
                if (existingStudents.contains(studentId)) {
                    sameDayCounts.put(studentId, sameDayCounts.getOrDefault(studentId, 0) + 1);
                }
            }
        }

        for (Integer studentId : candidateStudents) {
            int projected = sameDayCounts.getOrDefault(studentId, 0) + 1;
            if (projected > maxExamsPerDay) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        String code = candidate != null && candidate.getExam() != null
            ? candidate.getExam().getSubjectCode()
            : "UNKNOWN";
        return "Exam '" + code + "' exceeds preferred max exams/day for one or more students";
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.SOFT;
    }

    @Override
    public String getName() {
        return "MaxStudentExamsPerDay";
    }
}
