package com.examscheduler.model;

/**
 * The OUTPUT of the scheduling algorithm.
 * Combines an Exam with its assigned TimeSlot and Room.
 *
 * This is kept separate from Exam intentionally -
 * the algorithm can run multiple times without mutating input data.
 */
public class ScheduledExam {

    public enum Status { SCHEDULED, CONFLICT, UNPLACED }

    private int id; // DB primary key after persistence
    private Exam exam;
    private TimeSlot slot;
    private Room room;
    private Status status;
    private String conflictReason; // populated only if status != SCHEDULED

    public ScheduledExam() {}

    /** Constructor for successfully placed exams */
    public ScheduledExam(Exam exam, TimeSlot slot, Room room) {
        this.exam = exam;
        this.slot = slot;
        this.room = room;
        this.status = Status.SCHEDULED;
    }

    /** Constructor for failed placement */
    public ScheduledExam(Exam exam, Status status, String reason) {
        this.exam = exam;
        this.status = status;
        this.conflictReason = reason;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Exam getExam() { return exam; }
    public void setExam(Exam e) { this.exam = e; }

    public TimeSlot getSlot() { return slot; }
    public void setSlot(TimeSlot s) { this.slot = s; }

    public Room getRoom() { return room; }
    public void setRoom(Room r) { this.room = r; }

    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }

    public String getConflictReason() { return conflictReason; }
    public void setConflictReason(String r) { this.conflictReason = r; }

    public boolean isScheduled() {
        return Status.SCHEDULED.equals(this.status);
    }

    @Override
    public String toString() {
        if (!isScheduled()) {
            return "ScheduledExam{" + exam.getSubjectCode()
                 + " -> " + status + ": " + conflictReason + "}";
        }
        return "ScheduledExam{" + exam.getSubjectCode()
             + " -> " + slot + " in " + room.getName() + "}";
    }
}
