package com.examscheduler.util;

/**
 * Thrown when a hard constraint cannot be satisfied during scheduling.
 * Carries the constraint name so the caller knows exactly what failed.
 */
public class ConstraintViolationException extends RuntimeException {

    private final String constraintName;

    public ConstraintViolationException(String constraintName, String message) {
        super(message);
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    @Override
    public String toString() {
        return "[CONSTRAINT VIOLATION] " + constraintName + ": " + getMessage();
    }
}
