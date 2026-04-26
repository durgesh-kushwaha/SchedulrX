package com.examscheduler.constraints;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.examscheduler.model.Exam;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.TimeSlot;

class MaxStudentExamsPerDayConstraintTest {

    @Test
    void rejectsCandidateWhenDailyLoadWouldExceedLimit() {
        MaxStudentExamsPerDayConstraint constraint = new MaxStudentExamsPerDayConstraint(
            Map.of(
                1, Set.of(101),
                2, Set.of(101),
                3, Set.of(101)
            ),
            2
        );

        ScheduledExam existingA = scheduledExam(1, 1, 9, 11);
        ScheduledExam existingB = scheduledExam(2, 2, 12, 14);
        ScheduledExam candidate = scheduledExam(3, 3, 15, 17);

        assertFalse(constraint.isSatisfied(candidate, List.of(existingA, existingB)));
    }

    @Test
    void acceptsCandidateWhenDailyLoadStaysWithinLimit() {
        MaxStudentExamsPerDayConstraint constraint = new MaxStudentExamsPerDayConstraint(
            Map.of(
                1, Set.of(101),
                2, Set.of(101)
            ),
            2
        );

        ScheduledExam existing = scheduledExam(1, 1, 9, 11);
        ScheduledExam candidate = scheduledExam(2, 2, 15, 17);

        assertTrue(constraint.isSatisfied(candidate, List.of(existing)));
    }

    private ScheduledExam scheduledExam(int examId, int slotId, int startHour, int endHour) {
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setSubjectCode("SUB" + examId);
        exam.setEnrollmentCount(30);

        TimeSlot slot = new TimeSlot(
            slotId,
            LocalDate.of(2026, 5, 10),
            LocalTime.of(startHour, 0),
            LocalTime.of(endHour, 0)
        );

        Room room = new Room(1, "A-101", 80, false, "A");
        return new ScheduledExam(exam, slot, room);
    }
}
