package com.examscheduler.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents a block of time on a specific date.
 * The overlaps() method is the heart of conflict detection.
 */
public class TimeSlot {

    private int id;
    private String label;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;

    public TimeSlot() {}

    public TimeSlot(int id, LocalDate examDate, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate d) { this.examDate = d; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime t) { this.startTime = t; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime t) { this.endTime = t; }

    // Core logic

    /**
     * Two slots overlap if they are on the same date AND their time
     * intervals intersect. This is the fundamental predicate used by
     * all overlap constraints.
     *
     * Overlap condition: NOT (this ends before other starts
     *                        OR other ends before this starts)
     */
    public boolean overlaps(TimeSlot other) {
        if (!this.examDate.equals(other.examDate)) return false;
        return !(this.endTime.compareTo(other.startTime) <= 0
              || other.endTime.compareTo(this.startTime) <= 0);
    }

    /**
     * Gap in minutes between this slot ending and another slot starting.
     * Returns a negative value if they overlap.
     */
    public long minutesGapTo(TimeSlot other) {
        if (!this.examDate.equals(other.examDate)) return Long.MAX_VALUE;
        // Gap from this.end to other.start (might be negative if overlapping)
        return ChronoUnit.MINUTES.between(this.endTime, other.startTime);
    }

    public long durationMinutes() {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.MINUTES.between(startTime, endTime));
    }

    @Override
    public String toString() {
        return "TimeSlot{id=" + id + ", date=" + examDate
             + ", " + startTime + "-" + endTime + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        return this.id == ((TimeSlot) o).id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
