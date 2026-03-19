package com.examscheduler.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.examscheduler.model.Exam;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.Teacher;
import com.examscheduler.model.TimeSlot;

class ConstraintValidationServiceTest {

    private final ConstraintValidationService service = new ConstraintValidationService();

    @Test
    void returnsViolationForTeacherOverlap() {
        Teacher teacher = new Teacher(10, "T", "CS", "t@x");

        Exam examA = new Exam();
        examA.setId(1);
        examA.setTeacher(teacher);
        examA.setEnrollmentCount(20);

        Exam examB = new Exam();
        examB.setId(2);
        examB.setTeacher(teacher);
        examB.setEnrollmentCount(20);

        TimeSlot slot = new TimeSlot(1, LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), LocalTime.of(12, 0));
        Room room = new Room(1, "A", 40, false, "B");

        ScheduledExam existing = new ScheduledExam(examA, slot, room);
        ScheduledExam candidate = new ScheduledExam(examB, slot, room);

        String violation = service.firstViolation(candidate, List.of(existing), Map.of(1, Set.of(101), 2, Set.of(102)));

        assertNotNull(violation);
    }

    @Test
    void returnsNullWhenValid() {
        Teacher teacherA = new Teacher(10, "A", "CS", "a@x");
        Teacher teacherB = new Teacher(11, "B", "CS", "b@x");

        Exam examA = new Exam();
        examA.setId(1);
        examA.setTeacher(teacherA);
        examA.setEnrollmentCount(20);

        Exam examB = new Exam();
        examB.setId(2);
        examB.setTeacher(teacherB);
        examB.setEnrollmentCount(20);

        TimeSlot slotA = new TimeSlot(1, LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), LocalTime.of(12, 0));
        TimeSlot slotB = new TimeSlot(2, LocalDate.of(2026, 5, 10), LocalTime.of(14, 0), LocalTime.of(17, 0));
        Room room = new Room(1, "A", 40, false, "B");

        ScheduledExam existing = new ScheduledExam(examA, slotA, room);
        ScheduledExam candidate = new ScheduledExam(examB, slotB, room);

        String violation = service.firstViolation(candidate, List.of(existing), Map.of(1, Set.of(101), 2, Set.of(102)));

        assertNull(violation);
    }
}
