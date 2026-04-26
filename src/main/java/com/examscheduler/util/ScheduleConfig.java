package com.examscheduler.util;

/**
 * Central configuration for the scheduling engine.
 * All tunable parameters live here - change one place, affects everything.
 */
public class ScheduleConfig {

    // Minimum gap between two exams FOR THE SAME STUDENT (in minutes).
    // Set to 0 to disable gap constraint entirely.
    public static final int MIN_GAP_MINUTES = 120; // 2 hours

    // Maximum backtracking depth before giving up on an exam placement.
    // Higher = more thorough but slower. 50 is safe for up to ~30 exams.
    public static final int MAX_BACKTRACK_DEPTH = 50;

    // Maximum optimization iterations in the local search pass.
    public static final int MAX_OPTIMIZATION_ITERATIONS = 1000;

    // Soft target: try to keep each student's exam load balanced per day.
    public static final int SOFT_MAX_EXAMS_PER_DAY = 2;

    // Soft preference: CORE exams before this hour (24h format).
    public static final int CORE_MORNING_END_HOUR = 12;

    // Default counts for simulation and alternatives APIs.
    public static final int DEFAULT_SIMULATION_ALTERNATIVES = 3;
    public static final int MAX_SIMULATION_ALTERNATIVES = 10;

    // If true, the solver logs every decision (verbose mode for debugging).
    public static final boolean VERBOSE_LOGGING = false;

    private ScheduleConfig() {} // utility class - no instantiation
}
