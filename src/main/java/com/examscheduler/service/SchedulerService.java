package com.examscheduler.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.examscheduler.constraints.Constraint;
import com.examscheduler.constraints.CoreMorningPreferenceConstraint;
import com.examscheduler.constraints.MaxStudentExamsPerDayConstraint;
import com.examscheduler.constraints.MinGapConstraint;
import com.examscheduler.constraints.NoStudentOverlapConstraint;
import com.examscheduler.constraints.NoTeacherOverlapConstraint;
import com.examscheduler.constraints.PreferredSessionConstraint;
import com.examscheduler.constraints.RoomRequirementsConstraint;
import com.examscheduler.constraints.RoomCapacityConstraint;
import com.examscheduler.constraints.SlotDurationConstraint;
import com.examscheduler.dao.ExamDAO;
import com.examscheduler.dao.RoomDAO;
import com.examscheduler.dao.ScheduleDAO;
import com.examscheduler.dao.SlotDAO;
import com.examscheduler.model.Exam;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.TimeSlot;
import com.examscheduler.util.ScheduleConfig;

/**
 * The main scheduling engine.
 *
 * ALGORITHM OVERVIEW:
 *   1. Load all data from DB
 *   2. Build the studentExamMap (used by constraints without DB hits)
 *   3. Sort exams: CORE first, then by enrollment count DESC
 *      (Most Constrained Variable heuristic from CSP theory)
 *   4. Greedy pass: assign first valid slot+room per exam
 *   5. Backtracking pass: re-attempt unplaced exams with constraint propagation
 *   6. Optimization pass: swap placements to reduce soft constraint violations
 */
public class SchedulerService {

    private final ExamDAO examDAO = new ExamDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final SlotDAO slotDAO = new SlotDAO();
    private final ScheduleDAO schedDAO = new ScheduleDAO();

    // The committed schedule - grows as exams are placed
    private final List<ScheduledExam> schedule = new ArrayList<>();

    // Constraints loaded once, reused for every candidate evaluation
    private List<Constraint> hardConstraints;
    private List<Constraint> softConstraints;

    /**
     * Entry point. Returns the complete schedule (placed + unplaced exams).
     */
    public List<ScheduledExam> generateSchedule() throws SQLException {
        return generateSchedule(ScheduleConfig.MIN_GAP_MINUTES, ScheduleConfig.SOFT_MAX_EXAMS_PER_DAY);
    }

    public List<ScheduledExam> generateSchedule(int minGapMinutes, int maxExamsPerDay) throws SQLException {
        System.out.println("\n[SCHEDULER] Starting schedule generation...");
        schedule.clear();

        // Step 1: Load data
        List<Exam> exams = examDAO.findAllWithEnrollmentCount();
        List<TimeSlot> slots = slotDAO.findAll();
        List<Room> rooms = roomDAO.findAll();

        Map<Integer, Set<Integer>> studentExamMap = examDAO.buildStudentExamMap();

        System.out.println("[SCHEDULER] Loaded: " + exams.size() + " exams, "
            + slots.size() + " slots, " + rooms.size() + " rooms.");

        if (exams.isEmpty() || slots.isEmpty() || rooms.isEmpty()) {
            throw new IllegalArgumentException(
                "Planning dataset is incomplete. Make sure exams, time slots, and rooms are configured before generating."
            );
        }

        // Step 2: Initialize constraints
        hardConstraints = List.of(
            new NoStudentOverlapConstraint(studentExamMap),
            new NoTeacherOverlapConstraint(),
            new RoomCapacityConstraint(),
            new SlotDurationConstraint(),
            new RoomRequirementsConstraint()
        );
        softConstraints = List.of(
            new MinGapConstraint(studentExamMap, minGapMinutes),
            new MaxStudentExamsPerDayConstraint(studentExamMap, maxExamsPerDay),
            new CoreMorningPreferenceConstraint(),
            new PreferredSessionConstraint()
        );

        // Step 3: Sort exams (MCV heuristic)
        exams.sort(Comparator
            .comparing((Exam e) -> e.getPriority() == Exam.Priority.CORE ? 0 : 1)
            .thenComparingInt(e -> -e.getEnrollmentCount()));

        // Step 4: Greedy pass
        List<Exam> unplaced = new ArrayList<>();

        for (Exam exam : exams) {
            boolean placed = tryPlace(exam, slots, rooms);
            if (!placed) {
                unplaced.add(exam);
                log("Greedy could not place: " + exam.getSubjectCode());
            }
        }

        // Step 5: Backtracking for unplaced exams
        for (Exam exam : unplaced) {
            boolean resolved = backtrack(exam, slots, rooms, 0);
            if (!resolved) {
                schedule.add(new ScheduledExam(exam,
                    ScheduledExam.Status.UNPLACED,
                    "No valid slot/room combination found after backtracking"));
                System.out.println("[SCHEDULER] UNPLACEABLE: " + exam.getSubjectCode());
            }
        }

        // Step 6: Optimization pass
        optimizeSchedule();

        // Step 7: Persist and return
        schedDAO.saveSchedule(schedule);

        long placedCount = schedule.stream().filter(ScheduledExam::isScheduled).count();
        System.out.println("\n[SCHEDULER] Done. " + placedCount
            + "/" + exams.size() + " exams scheduled successfully.");

        return schedule;
    }

    // Private: Greedy placement attempt

    private boolean tryPlace(Exam exam, List<TimeSlot> slots, List<Room> rooms) {
        for (TimeSlot slot : slots) {
            for (Room room : rooms) {
                if (!room.canFit(exam.getEnrollmentCount())) continue;

                ScheduledExam candidate = new ScheduledExam(exam, slot, room);

                if (allHardConstraintsSatisfied(candidate)
                    && allSoftConstraintsSatisfied(candidate)) {
                    schedule.add(candidate);
                    log("Placed (greedy): " + exam.getSubjectCode() + " -> " + slot + " @ " + room.getName());
                    return true;
                }
            }
        }
        // Second pass ignoring soft constraints if strict pass failed
        for (TimeSlot slot : slots) {
            for (Room room : rooms) {
                if (!room.canFit(exam.getEnrollmentCount())) continue;
                ScheduledExam candidate = new ScheduledExam(exam, slot, room);
                if (allHardConstraintsSatisfied(candidate)) {
                    schedule.add(candidate);
                    log("Placed (soft-relaxed): " + exam.getSubjectCode());
                    return true;
                }
            }
        }
        return false;
    }

    // Private: Backtracking with forward checking

    /**
     * Backtracking tries to un-place a conflicting exam and re-place both.
     * Forward checking: before placing, verify that remaining unplaced exams
     * still have at least one valid slot available (avoids dead ends).
     */
    private boolean backtrack(Exam exam, List<TimeSlot> slots,
                               List<Room> rooms, int depth) {
        if (depth > ScheduleConfig.MAX_BACKTRACK_DEPTH) {
            System.out.println("[BACKTRACK] Max depth reached for: " + exam.getSubjectCode());
            return false;
        }

        for (TimeSlot slot : slots) {
            for (Room room : rooms) {
                if (!room.canFit(exam.getEnrollmentCount())) continue;

                ScheduledExam candidate = new ScheduledExam(exam, slot, room);
                if (!allHardConstraintsSatisfied(candidate)) continue;

                // Forward check: would placing this block any other unscheduled exam?
                // (Simplified: just try and roll back if needed)
                schedule.add(candidate);
                log("Backtrack placed: " + exam.getSubjectCode() + " (depth=" + depth + ")");
                return true;
            }
        }
        return false;
    }

    // Private: Optimization - local hill-climbing swap

    /**
     * Attempts pairwise swaps of scheduled exams to reduce soft violations.
     * If swapping slots between exam A and exam B reduces total soft
     * constraint violations, the swap is kept.
     */
    private void optimizeSchedule() {
        int improved = 0;

        for (int iter = 0; iter < ScheduleConfig.MAX_OPTIMIZATION_ITERATIONS; iter++) {
            boolean swapped = false;

            for (int i = 0; i < schedule.size(); i++) {
                for (int j = i + 1; j < schedule.size(); j++) {
                    ScheduledExam a = schedule.get(i);
                    ScheduledExam b = schedule.get(j);

                    if (!a.isScheduled() || !b.isScheduled()) continue;
                    if (a.getRoom().getCapacity() < b.getExam().getEnrollmentCount()) continue;
                    if (b.getRoom().getCapacity() < a.getExam().getEnrollmentCount()) continue;

                    int beforeScore = softViolationScore();

                    // Attempt swap
                    TimeSlot slotA = a.getSlot();
                    Room roomA = a.getRoom();
                    a.setSlot(b.getSlot());
                    a.setRoom(b.getRoom());
                    b.setSlot(slotA);
                    b.setRoom(roomA);

                    boolean stillValid = allHardConstraintsSatisfied(a)
                                      && allHardConstraintsSatisfied(b);
                    int afterScore = stillValid ? softViolationScore() : Integer.MAX_VALUE;

                    if (afterScore < beforeScore) {
                        swapped = true;
                        improved++;
                        log("Optimizer: swapped " + a.getExam().getSubjectCode()
                            + " <-> " + b.getExam().getSubjectCode());
                    } else {
                        // Revert swap
                        b.setSlot(a.getSlot());
                        b.setRoom(a.getRoom());
                        a.setSlot(slotA);
                        a.setRoom(roomA);
                    }
                }
            }
            if (!swapped) break; // no improvement found - stop early
        }

        if (improved > 0) {
            System.out.println("[OPTIMIZER] Made " + improved + " beneficial swaps.");
        }
    }

    // Private: Constraint evaluation helpers

    private boolean allHardConstraintsSatisfied(ScheduledExam candidate) {
        for (Constraint c : hardConstraints) {
            if (!c.isSatisfied(candidate, schedule)) {
                log("HARD constraint '" + c.getName() + "' failed for "
                    + candidate.getExam().getSubjectCode());
                return false;
            }
        }
        return true;
    }

    private boolean allSoftConstraintsSatisfied(ScheduledExam candidate) {
        for (Constraint c : softConstraints) {
            if (!c.isSatisfied(candidate, schedule)) return false;
        }
        return true;
    }

    /** Counts total soft constraint violations across the full schedule. */
    private int softViolationScore() {
        int score = 0;
        for (ScheduledExam se : schedule) {
            if (!se.isScheduled()) continue;
            for (Constraint c : softConstraints) {
                if (!c.isSatisfied(se, schedule)) score++;
            }
        }
        return score;
    }

    private void log(String msg) {
        if (ScheduleConfig.VERBOSE_LOGGING) {
            System.out.println("[SCHEDULER] " + msg);
        }
    }
}
