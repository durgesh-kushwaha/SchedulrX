package com.examscheduler.model;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TimeSlotTest {

    @Test
    void overlapsReturnsTrueForSameDateIntersectingTimes() {
        TimeSlot a = new TimeSlot(1, LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), LocalTime.of(12, 0));
        TimeSlot b = new TimeSlot(2, LocalDate.of(2026, 5, 10), LocalTime.of(11, 0), LocalTime.of(13, 0));

        assertTrue(a.overlaps(b));
    }

    @Test
    void overlapsReturnsFalseForTouchingBoundaries() {
        TimeSlot a = new TimeSlot(1, LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), LocalTime.of(12, 0));
        TimeSlot b = new TimeSlot(2, LocalDate.of(2026, 5, 10), LocalTime.of(12, 0), LocalTime.of(15, 0));

        assertFalse(a.overlaps(b));
    }
}
