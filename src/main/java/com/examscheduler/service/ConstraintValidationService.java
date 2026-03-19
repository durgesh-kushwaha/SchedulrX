package com.examscheduler.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.examscheduler.constraints.Constraint;
import com.examscheduler.constraints.NoStudentOverlapConstraint;
import com.examscheduler.constraints.NoTeacherOverlapConstraint;
import com.examscheduler.constraints.RoomCapacityConstraint;
import com.examscheduler.model.ScheduledExam;

public class ConstraintValidationService {

    public String firstViolation(ScheduledExam candidate,
                                 List<ScheduledExam> currentSchedule,
                                 Map<Integer, Set<Integer>> studentExamMap) {
        List<Constraint> hardConstraints = List.of(
            new NoStudentOverlapConstraint(studentExamMap),
            new NoTeacherOverlapConstraint(),
            new RoomCapacityConstraint()
        );

        for (Constraint constraint : hardConstraints) {
            if (!constraint.isSatisfied(candidate, currentSchedule)) {
                return constraint.getViolationMessage(candidate, currentSchedule);
            }
        }
        return null;
    }
}
