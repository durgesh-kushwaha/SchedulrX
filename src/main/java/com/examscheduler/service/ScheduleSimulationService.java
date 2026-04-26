package com.examscheduler.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.examscheduler.api.dto.ScheduleAlternativeResponse;
import com.examscheduler.api.dto.ScheduleRowResponse;
import com.examscheduler.api.dto.ScheduleSimulationRequest;
import com.examscheduler.api.dto.ScheduleSimulationResponse;
import com.examscheduler.constraints.Constraint;
import com.examscheduler.constraints.CoreMorningPreferenceConstraint;
import com.examscheduler.constraints.MaxStudentExamsPerDayConstraint;
import com.examscheduler.constraints.MinGapConstraint;
import com.examscheduler.constraints.NoStudentOverlapConstraint;
import com.examscheduler.constraints.NoTeacherOverlapConstraint;
import com.examscheduler.constraints.RoomCapacityConstraint;
import com.examscheduler.dao.ExamDAO;
import com.examscheduler.dao.RoomDAO;
import com.examscheduler.dao.SlotDAO;
import com.examscheduler.model.Exam;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.model.TimeSlot;
import com.examscheduler.util.ScheduleConfig;

@Service
public class ScheduleSimulationService {

    private final ExamDAO examDAO = new ExamDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final SlotDAO slotDAO = new SlotDAO();

    public ScheduleSimulationResponse simulate(ScheduleSimulationRequest request, String actorUsername) throws SQLException {
        ScheduleSimulationRequest safeRequest = request == null
            ? new ScheduleSimulationRequest(null, null, null, null, null, null)
            : request;

        int requestedAlternatives = normalizeAlternativeCount(safeRequest.alternatives());
        int minGapMinutes = safeRequest.minGapMinutes() == null
            ? ScheduleConfig.MIN_GAP_MINUTES
            : Math.max(0, safeRequest.minGapMinutes());

        Strategy strategy = Strategy.from(safeRequest.strategy());
        Set<Integer> blockedRoomIds = normalizeIds(safeRequest.blockedRoomIds());
        Set<Integer> blockedSlotIds = normalizeIds(safeRequest.blockedSlotIds());
        Set<LocalDate> blockedDates = normalizeDates(safeRequest.blockedDates());

        List<Exam> exams = examDAO.findAllWithEnrollmentCount();
        if (exams.isEmpty()) {
            throw new IllegalArgumentException("Simulation failed: no exams available");
        }

        List<TimeSlot> slots = slotDAO.findAll().stream()
            .filter(slot -> !blockedSlotIds.contains(slot.getId()))
            .filter(slot -> slot.getExamDate() == null || !blockedDates.contains(slot.getExamDate()))
            .toList();
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("Simulation failed: all slots are blocked or unavailable");
        }

        List<Room> rooms = roomDAO.findAll().stream()
            .filter(room -> !blockedRoomIds.contains(room.getId()))
            .toList();
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("Simulation failed: all rooms are blocked or unavailable");
        }

        Map<Integer, Set<Integer>> studentExamMap = examDAO.buildStudentExamMap();

        List<ScheduleAlternativeResponse> alternatives = buildAlternatives(
            requestedAlternatives,
            exams,
            slots,
            rooms,
            studentExamMap,
            minGapMinutes,
            strategy
        );

        return new ScheduleSimulationResponse(
            "Simulation completed",
            actorUsername,
            strategy.name(),
            minGapMinutes,
            requestedAlternatives,
            alternatives.size(),
            alternatives
        );
    }

    private List<ScheduleAlternativeResponse> buildAlternatives(int requestedAlternatives,
                                                                List<Exam> exams,
                                                                List<TimeSlot> slots,
                                                                List<Room> rooms,
                                                                Map<Integer, Set<Integer>> studentExamMap,
                                                                int minGapMinutes,
                                                                Strategy strategy) {
        List<ScheduleAlternativeResponse> raw = new ArrayList<>();
        Set<String> signatures = new HashSet<>();

        long seedBase = System.nanoTime();
        int maxAttempts = Math.max(requestedAlternatives * 4, requestedAlternatives);

        for (int attempt = 0; attempt < maxAttempts && raw.size() < requestedAlternatives; attempt++) {
            SimulationRunResult run = runSingleAlternative(
                exams,
                slots,
                rooms,
                studentExamMap,
                minGapMinutes,
                strategy,
                seedBase + attempt
            );

            String signature = signature(run.rows());
            if (signatures.add(signature)) {
                raw.add(toAlternativeResponse(raw.size() + 1, run));
            }
        }

        raw.sort(Comparator
            .comparingInt(ScheduleAlternativeResponse::unplacedExams)
            .thenComparingInt(ScheduleAlternativeResponse::softPenaltyScore)
            .thenComparingLong(ScheduleAlternativeResponse::runtimeMs));

        List<ScheduleAlternativeResponse> ranked = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            ScheduleAlternativeResponse alt = raw.get(i);
            ranked.add(new ScheduleAlternativeResponse(
                i + 1,
                alt.totalExams(),
                alt.scheduledExams(),
                alt.unplacedExams(),
                alt.softPenaltyScore(),
                alt.runtimeMs(),
                alt.rows()
            ));
        }

        return ranked;
    }

    private SimulationRunResult runSingleAlternative(List<Exam> exams,
                                                     List<TimeSlot> slots,
                                                     List<Room> rooms,
                                                     Map<Integer, Set<Integer>> studentExamMap,
                                                     int minGapMinutes,
                                                     Strategy strategy,
                                                     long seed) {
        long startedAt = System.currentTimeMillis();

        List<Constraint> hardConstraints = List.of(
            new NoStudentOverlapConstraint(studentExamMap),
            new NoTeacherOverlapConstraint(),
            new RoomCapacityConstraint()
        );

        List<Constraint> softConstraints = List.of(
            new MinGapConstraint(studentExamMap, minGapMinutes),
            new MaxStudentExamsPerDayConstraint(studentExamMap, ScheduleConfig.SOFT_MAX_EXAMS_PER_DAY),
            new CoreMorningPreferenceConstraint()
        );

        List<Exam> orderedExams = new ArrayList<>(exams);
        Collections.shuffle(orderedExams, new Random(seed));
        orderedExams.sort(Comparator
            .comparing((Exam exam) -> exam.getPriority() == Exam.Priority.CORE ? 0 : 1)
            .thenComparingInt(exam -> -exam.getEnrollmentCount()));

        List<ScheduledExam> workingSchedule = new ArrayList<>();
        List<Exam> unresolved = new ArrayList<>();

        for (Exam exam : orderedExams) {
            boolean placed = tryPlace(exam, slots, rooms, workingSchedule, hardConstraints, softConstraints);
            if (!placed) {
                unresolved.add(exam);
            }
        }

        for (Exam exam : unresolved) {
            boolean resolved = strategy == Strategy.HYBRID
                && backtrack(exam, slots, rooms, workingSchedule, hardConstraints);

            if (!resolved) {
                workingSchedule.add(new ScheduledExam(
                    exam,
                    ScheduledExam.Status.UNPLACED,
                    "No valid slot/room combination found under simulation constraints"
                ));
            }
        }

        if (strategy == Strategy.HYBRID) {
            optimizeSchedule(workingSchedule, hardConstraints, softConstraints);
        }

        int softScore = softViolationScore(workingSchedule, softConstraints);
        long runtimeMs = Math.max(0L, System.currentTimeMillis() - startedAt);

        workingSchedule.sort(scheduledExamComparator());
        return new SimulationRunResult(List.copyOf(workingSchedule), softScore, runtimeMs);
    }

    private boolean tryPlace(Exam exam,
                             List<TimeSlot> slots,
                             List<Room> rooms,
                             List<ScheduledExam> current,
                             List<Constraint> hardConstraints,
                             List<Constraint> softConstraints) {
        for (TimeSlot slot : slots) {
            for (Room room : rooms) {
                if (!room.canFit(exam.getEnrollmentCount())) {
                    continue;
                }

                ScheduledExam candidate = new ScheduledExam(exam, slot, room);
                if (allConstraintsSatisfied(candidate, current, hardConstraints)
                    && allConstraintsSatisfied(candidate, current, softConstraints)) {
                    current.add(candidate);
                    return true;
                }
            }
        }

        for (TimeSlot slot : slots) {
            for (Room room : rooms) {
                if (!room.canFit(exam.getEnrollmentCount())) {
                    continue;
                }

                ScheduledExam candidate = new ScheduledExam(exam, slot, room);
                if (allConstraintsSatisfied(candidate, current, hardConstraints)) {
                    current.add(candidate);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean backtrack(Exam exam,
                              List<TimeSlot> slots,
                              List<Room> rooms,
                              List<ScheduledExam> current,
                              List<Constraint> hardConstraints) {
        for (TimeSlot slot : slots) {
            for (Room room : rooms) {
                if (!room.canFit(exam.getEnrollmentCount())) {
                    continue;
                }

                ScheduledExam candidate = new ScheduledExam(exam, slot, room);
                if (allConstraintsSatisfied(candidate, current, hardConstraints)) {
                    current.add(candidate);
                    return true;
                }
            }
        }
        return false;
    }

    private void optimizeSchedule(List<ScheduledExam> schedule,
                                  List<Constraint> hardConstraints,
                                  List<Constraint> softConstraints) {
        for (int iteration = 0; iteration < ScheduleConfig.MAX_OPTIMIZATION_ITERATIONS; iteration++) {
            boolean improved = false;

            for (int i = 0; i < schedule.size(); i++) {
                for (int j = i + 1; j < schedule.size(); j++) {
                    ScheduledExam a = schedule.get(i);
                    ScheduledExam b = schedule.get(j);

                    if (!a.isScheduled() || !b.isScheduled()) {
                        continue;
                    }
                    if (a.getRoom().getCapacity() < b.getExam().getEnrollmentCount()) {
                        continue;
                    }
                    if (b.getRoom().getCapacity() < a.getExam().getEnrollmentCount()) {
                        continue;
                    }

                    int before = softViolationScore(schedule, softConstraints);

                    TimeSlot originalSlotA = a.getSlot();
                    Room originalRoomA = a.getRoom();
                    TimeSlot originalSlotB = b.getSlot();
                    Room originalRoomB = b.getRoom();

                    a.setSlot(originalSlotB);
                    a.setRoom(originalRoomB);
                    b.setSlot(originalSlotA);
                    b.setRoom(originalRoomA);

                    boolean valid = allConstraintsSatisfied(a, schedule, hardConstraints)
                        && allConstraintsSatisfied(b, schedule, hardConstraints);
                    int after = valid ? softViolationScore(schedule, softConstraints) : Integer.MAX_VALUE;

                    if (after < before) {
                        improved = true;
                    } else {
                        a.setSlot(originalSlotA);
                        a.setRoom(originalRoomA);
                        b.setSlot(originalSlotB);
                        b.setRoom(originalRoomB);
                    }
                }
            }

            if (!improved) {
                break;
            }
        }
    }

    private boolean allConstraintsSatisfied(ScheduledExam candidate,
                                            List<ScheduledExam> current,
                                            List<Constraint> constraints) {
        for (Constraint constraint : constraints) {
            if (!constraint.isSatisfied(candidate, current)) {
                return false;
            }
        }
        return true;
    }

    private int softViolationScore(List<ScheduledExam> schedule, List<Constraint> softConstraints) {
        int score = 0;
        for (ScheduledExam row : schedule) {
            if (!row.isScheduled()) {
                continue;
            }
            for (Constraint constraint : softConstraints) {
                if (!constraint.isSatisfied(row, schedule)) {
                    score++;
                }
            }
        }
        return score;
    }

    private ScheduleAlternativeResponse toAlternativeResponse(int rank, SimulationRunResult run) {
        int scheduled = (int) run.rows().stream().filter(ScheduledExam::isScheduled).count();
        int total = run.rows().size();
        int unplaced = Math.max(0, total - scheduled);

        return new ScheduleAlternativeResponse(
            rank,
            total,
            scheduled,
            unplaced,
            run.softPenaltyScore(),
            run.runtimeMs(),
            toRows(run.rows())
        );
    }

    private List<ScheduleRowResponse> toRows(List<ScheduledExam> rows) {
        return rows.stream().map(this::toRow).toList();
    }

    private ScheduleRowResponse toRow(ScheduledExam se) {
        String teacher = se.getExam() != null && se.getExam().getTeacher() != null
            ? se.getExam().getTeacher().getName()
            : "TBD";

        String examDate = se.getSlot() != null && se.getSlot().getExamDate() != null
            ? se.getSlot().getExamDate().toString()
            : "-";

        String startTime = se.getSlot() != null && se.getSlot().getStartTime() != null
            ? se.getSlot().getStartTime().toString()
            : "-";

        String endTime = se.getSlot() != null && se.getSlot().getEndTime() != null
            ? se.getSlot().getEndTime().toString()
            : "-";

        String room = se.getRoom() != null ? se.getRoom().getName() : "-";

        return new ScheduleRowResponse(
            se.getExam() != null ? se.getExam().getId() : 0,
            se.getExam() != null ? se.getExam().getSubjectCode() : "UNKNOWN",
            se.getExam() != null ? se.getExam().getSubjectName() : "Unknown Subject",
            teacher,
            examDate,
            startTime,
            endTime,
            room,
            se.getStatus().name(),
            se.getConflictReason() == null ? "" : se.getConflictReason()
        );
    }

    private String signature(List<ScheduledExam> rows) {
        return rows.stream()
            .sorted(Comparator.comparingInt(se -> se.getExam() != null ? se.getExam().getId() : 0))
            .map(se -> {
                int examId = se.getExam() != null ? se.getExam().getId() : 0;
                int slotId = se.getSlot() != null ? se.getSlot().getId() : -1;
                int roomId = se.getRoom() != null ? se.getRoom().getId() : -1;
                return examId + ":" + slotId + ":" + roomId + ":" + se.getStatus().name();
            })
            .collect(Collectors.joining("|"));
    }

    private Comparator<ScheduledExam> scheduledExamComparator() {
        return Comparator
            .comparing((ScheduledExam se) -> se.getSlot() != null ? se.getSlot().getExamDate() : null,
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(se -> se.getSlot() != null ? se.getSlot().getStartTime() : null,
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(se -> se.getExam() != null ? se.getExam().getId() : 0);
    }

    private int normalizeAlternativeCount(Integer alternatives) {
        int value = alternatives == null ? ScheduleConfig.DEFAULT_SIMULATION_ALTERNATIVES : alternatives;
        value = Math.max(1, value);
        return Math.min(value, ScheduleConfig.MAX_SIMULATION_ALTERNATIVES);
    }

    private Set<Integer> normalizeIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
            .filter(Objects::nonNull)
            .filter(id -> id > 0)
            .collect(Collectors.toSet());
    }

    private Set<LocalDate> normalizeDates(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        Set<LocalDate> dates = new HashSet<>();
        for (String raw : values) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                dates.add(LocalDate.parse(raw.trim()));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Invalid blocked date '" + raw + "'. Expected format: yyyy-MM-dd");
            }
        }
        return dates;
    }

    private enum Strategy {
        HYBRID,
        GREEDY_ONLY;

        static Strategy from(String value) {
            if (value == null || value.isBlank()) {
                return HYBRID;
            }
            try {
                return Strategy.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported strategy '" + value + "'. Use HYBRID or GREEDY_ONLY");
            }
        }
    }

    private record SimulationRunResult(
        List<ScheduledExam> rows,
        int softPenaltyScore,
        long runtimeMs
    ) {
    }
}
