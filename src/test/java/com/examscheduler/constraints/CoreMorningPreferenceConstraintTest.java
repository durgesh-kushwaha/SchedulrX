package com.examscheduler.constraints;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.examscheduler.model.Exam;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.TimeSlot;

class CoreMorningPreferenceConstraintTest {

    private final CoreMorningPreferenceConstraint constraint = new CoreMorningPreferenceConstraint();

    @Test
    void coreExamInMorningPasses() {
        ScheduledExam candidate = new ScheduledExam(
            exam(1, Exam.Priority.CORE),
            slot(1, 9, 12),
            room(1)
        );

        assertTrue(constraint.isSatisfied(candidate, List.of()));
    }

    @Test
    void coreExamInAfternoonViolatesPreference() {
        ScheduledExam candidate = new ScheduledExam(
            exam(2, Exam.Priority.CORE),
            slot(2, 14, 17),
            room(1)
        );

        assertFalse(constraint.isSatisfied(candidate, List.of()));
    }

    @Test
    void electiveExamInAfternoonPasses() {
        ScheduledExam candidate = new ScheduledExam(
            exam(3, Exam.Priority.ELECTIVE),
            slot(3, 15, 17),
            room(1)
        );

        assertTrue(constraint.isSatisfied(candidate, List.of()));
    }

    private Exam exam(int id, Exam.Priority priority) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setPriority(priority);
        exam.setSubjectCode("SUB" + id);
        exam.setEnrollmentCount(20);
        return exam;
    }

    private TimeSlot slot(int id, int startHour, int endHour) {
        return new TimeSlot(
            id,
            LocalDate.of(2026, 5, 10),
            LocalTime.of(startHour, 0),
            LocalTime.of(endHour, 0)
        );
    }

    private Room room(int id) {
        return new Room(id, "R" + id, 50, false, "A");
    }
}
