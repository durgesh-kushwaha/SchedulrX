package com.examscheduler.model;

/**
 * Represents one exam to be scheduled.
 * priority drives the ordering in the greedy pass -
 * CORE exams are placed before ELECTIVEs.
 */
public class Exam {

    public enum Priority { CORE, ELECTIVE }

    private int id;
    private String subjectName;
    private String subjectCode;
    private int durationMinutes;
    private Priority priority;
    private Teacher teacher;

    // Loaded separately - how many students are enrolled
    // Used for room selection and greedy sort order
    private int enrollmentCount;

    public Exam() {}

    public Exam(int id, String subjectName, String subjectCode,
                int durationMinutes, Priority priority, Teacher teacher) {
        this.id = id;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.durationMinutes = durationMinutes;
        this.priority = priority;
        this.teacher = teacher;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String n) { this.subjectName = n; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String c) { this.subjectCode = c; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int d) { this.durationMinutes = d; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority p) { this.priority = p; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher t) { this.teacher = t; }

    public int getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(int c) { this.enrollmentCount = c; }

    public boolean isCoreExam() {
        return Priority.CORE.equals(this.priority);
    }

    @Override
    public String toString() {
        return "Exam{id=" + id + ", code='" + subjectCode
             + "', name='" + subjectName
             + "', priority=" + priority
             + ", students=" + enrollmentCount + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exam)) return false;
        return this.id == ((Exam) o).id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
