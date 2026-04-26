package com.examscheduler.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.examscheduler.api.dto.PlanningConfigDto;
import com.examscheduler.api.dto.PlanningDatasetDto;
import com.examscheduler.api.dto.PlanningDatasetResponse;
import com.examscheduler.api.dto.PlanningExamDto;
import com.examscheduler.api.dto.PlanningReadinessResponse;
import com.examscheduler.api.dto.PlanningRoomDto;
import com.examscheduler.api.dto.PlanningSlotDto;
import com.examscheduler.api.dto.PlanningStudentDto;
import com.examscheduler.api.dto.PlanningTeacherDto;
import com.examscheduler.dao.PlanningDAO;
import com.examscheduler.util.ScheduleConfig;

@Service
public class PlanningService {

    public record SchedulingRules(int minGapMinutes, int maxExamsPerDay) {
    }

    private final PlanningDAO planningDAO = new PlanningDAO();

    public PlanningDatasetResponse currentDataset() throws java.sql.SQLException {
        PlanningDatasetDto dataset = planningDAO.loadDataset();
        return new PlanningDatasetResponse(
            "Planning dataset loaded",
            dataset,
            assess(dataset)
        );
    }

    public PlanningDatasetResponse saveDataset(PlanningDatasetDto dataset, String actorUsername) throws java.sql.SQLException {
        planningDAO.replaceDataset(dataset);
        PlanningDatasetDto saved = planningDAO.loadDataset();
        return new PlanningDatasetResponse(
            "Planning dataset saved by " + actorUsername,
            saved,
            assess(saved)
        );
    }

    public PlanningDatasetResponse starterTemplate() {
        PlanningDatasetDto dataset = buildStarterDataset();
        return new PlanningDatasetResponse(
            "Starter template ready",
            dataset,
            assess(dataset)
        );
    }

    public void assertReadyForGeneration() throws java.sql.SQLException {
        PlanningReadinessResponse readiness = assess(planningDAO.loadDataset());
        if (!readiness.ready()) {
            String details = String.join("; ", readiness.blockingIssues());
            throw new IllegalArgumentException("Planning dataset is incomplete: " + details);
        }
    }

    public SchedulingRules schedulingRules() throws java.sql.SQLException {
        PlanningConfigDto config = planningDAO.loadDataset().config();
        int minGap = config != null && config.minGapMinutes() != null
            ? Math.max(0, config.minGapMinutes())
            : ScheduleConfig.MIN_GAP_MINUTES;
        int maxPerDay = config != null && config.maxExamsPerDay() != null
            ? Math.max(1, config.maxExamsPerDay())
            : ScheduleConfig.SOFT_MAX_EXAMS_PER_DAY;
        return new SchedulingRules(minGap, maxPerDay);
    }

    public PlanningReadinessResponse assess(PlanningDatasetDto dataset) {
        PlanningDatasetDto safe = dataset == null
            ? new PlanningDatasetDto(null, List.of(), List.of(), List.of(), List.of(), List.of())
            : dataset;

        List<String> blocking = new ArrayList<>();
        List<String> advisory = new ArrayList<>();

        List<PlanningTeacherDto> teachers = safeList(safe.teachers());
        List<PlanningRoomDto> rooms = safeList(safe.rooms());
        List<PlanningSlotDto> slots = safeList(safe.slots());
        List<PlanningStudentDto> students = safeList(safe.students());
        List<PlanningExamDto> exams = safeList(safe.exams());

        if (teachers.isEmpty()) blocking.add("Add at least one teacher before generating a schedule.");
        if (rooms.isEmpty()) blocking.add("Add at least one room before generating a schedule.");
        if (slots.isEmpty()) blocking.add("Add at least one time slot before generating a schedule.");
        if (students.isEmpty()) blocking.add("Add students so conflict detection can work.");
        if (exams.isEmpty()) blocking.add("Add exams before generating a schedule.");

        Map<Integer, PlanningTeacherDto> teacherById = teachers.stream()
            .filter(teacher -> teacher.id() != null)
            .collect(Collectors.toMap(PlanningTeacherDto::id, teacher -> teacher, (a, b) -> a));
        Map<Integer, PlanningStudentDto> studentById = students.stream()
            .filter(student -> student.id() != null)
            .collect(Collectors.toMap(PlanningStudentDto::id, student -> student, (a, b) -> a));

        boolean hasProjectorRoom = rooms.stream().anyMatch(room -> Boolean.TRUE.equals(room.hasProjector()));
        boolean hasComputerRoom = rooms.stream().anyMatch(room -> Boolean.TRUE.equals(room.hasComputers()));
        boolean hasAccommodatedStudents = students.stream().anyMatch(student -> safePositive(student.extraTimeMinutes()) > 0);
        Set<Integer> slotIds = slots.stream()
            .map(PlanningSlotDto::id)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());

        int enrollmentCount = 0;
        for (PlanningExamDto exam : exams) {
            String code = blankToFallback(exam.subjectCode(), "Unnamed exam");

            if (exam.teacherId() == null || !teacherById.containsKey(exam.teacherId())) {
                blocking.add("Exam '" + code + "' must be linked to a valid teacher.");
            }

            int duration = safePositive(exam.durationMinutes());
            if (duration <= 0) {
                blocking.add("Exam '" + code + "' must have a positive duration.");
            }

            List<Integer> studentIds = distinctPositiveIds(exam.studentIds());
            enrollmentCount += studentIds.size();
            if (studentIds.isEmpty()) {
                blocking.add("Exam '" + code + "' needs at least one registered student.");
            }

            for (Integer studentId : studentIds) {
                if (!studentById.containsKey(studentId)) {
                    blocking.add("Exam '" + code + "' references unknown student ID " + studentId + ".");
                }
            }

            if (duration > 0 && slots.stream().noneMatch(slot -> slotDurationMinutes(slot) >= duration)) {
                blocking.add("No available slot is long enough for exam '" + code + "'.");
            }

            if (Boolean.TRUE.equals(exam.requiresProjector()) && !hasProjectorRoom) {
                blocking.add("Exam '" + code + "' requires a projector but no room provides one.");
            }

            boolean needsComputers = Boolean.TRUE.equals(exam.requiresComputers())
                || "LAB".equalsIgnoreCase(exam.examType());
            if (needsComputers && !hasComputerRoom) {
                blocking.add("Exam '" + code + "' requires computers but no room provides them.");
            }

            PlanningTeacherDto teacher = teacherById.get(exam.teacherId());
            if (teacher != null) {
                Set<Integer> unavailable = new HashSet<>(distinctPositiveIds(teacher.unavailableSlotIds()));
                if (!slotIds.isEmpty() && !unavailable.isEmpty() && unavailable.containsAll(slotIds)) {
                    blocking.add("Teacher '" + teacher.name() + "' is unavailable for every configured slot.");
                }
            }
        }

        if (hasAccommodatedStudents) {
            advisory.add("Students with extra-time accommodations are stored, but seat-level accommodations still need a dedicated seating module.");
        }

        PlanningConfigDto config = safe.config();
        if (config != null && safePositive(config.minGapMinutes()) == 0) {
            advisory.add("Minimum gap is currently set to 0 minutes, so back-to-back student exams are allowed.");
        }

        return new PlanningReadinessResponse(
            blocking.isEmpty(),
            teachers.size(),
            rooms.size(),
            slots.size(),
            students.size(),
            exams.size(),
            enrollmentCount,
            List.copyOf(blocking.stream().distinct().toList()),
            List.copyOf(advisory.stream().distinct().toList())
        );
    }

    private PlanningDatasetDto buildStarterDataset() {
        return new PlanningDatasetDto(
            new PlanningConfigDto("Smart University", "Spring 2026", 120, 2),
            List.of(
                new PlanningTeacherDto(1, "Dr. Meera Shah", "Computer Science", "meera.shah@smartu.edu", List.of(6)),
                new PlanningTeacherDto(2, "Prof. Arjun Rao", "Electronics", "arjun.rao@smartu.edu", List.of()),
                new PlanningTeacherDto(3, "Dr. Kavya Nair", "Mathematics", "kavya.nair@smartu.edu", List.of(2))
            ),
            List.of(
                new PlanningRoomDto(1, "A-201", 40, true, false, "Academic Block A", "Classroom"),
                new PlanningRoomDto(2, "B-Lab-3", 32, true, true, "Innovation Block B", "Lab"),
                new PlanningRoomDto(3, "Main Hall", 120, true, false, "Central Hall", "Hall")
            ),
            List.of(
                new PlanningSlotDto(1, "Day 1 Morning", "2026-05-10", "09:00", "12:00"),
                new PlanningSlotDto(2, "Day 1 Afternoon", "2026-05-10", "14:00", "17:00"),
                new PlanningSlotDto(3, "Day 2 Morning", "2026-05-11", "09:00", "12:00"),
                new PlanningSlotDto(4, "Day 2 Afternoon", "2026-05-11", "14:00", "17:00"),
                new PlanningSlotDto(5, "Day 3 Morning", "2026-05-12", "09:00", "12:00"),
                new PlanningSlotDto(6, "Day 3 Afternoon", "2026-05-12", "14:00", "17:00")
            ),
            List.of(
                new PlanningStudentDto(1, "Aarav Gupta", "CS2301", 6, "CSE", 0, ""),
                new PlanningStudentDto(2, "Ishita Jain", "CS2302", 6, "CSE", 0, ""),
                new PlanningStudentDto(3, "Rohan Sen", "CS2303", 6, "CSE", 0, ""),
                new PlanningStudentDto(4, "Maya Kapoor", "CS2304", 6, "CSE", 30, "Needs extra time"),
                new PlanningStudentDto(5, "Neel Verma", "EC2301", 6, "ECE", 0, ""),
                new PlanningStudentDto(6, "Sara Ali", "EC2302", 6, "ECE", 0, ""),
                new PlanningStudentDto(7, "Kabir Joshi", "MA2301", 4, "MATH", 0, ""),
                new PlanningStudentDto(8, "Anaya Das", "MA2302", 4, "MATH", 0, "")
            ),
            List.of(
                new PlanningExamDto(1, "Advanced Algorithms", "CS601", 180, "CORE", 1, "CSE", "THEORY", true, false, "MORNING", 5, List.of(1, 2, 3, 4)),
                new PlanningExamDto(2, "Distributed Systems Lab", "CSL602", 180, "CORE", 1, "CSE", "LAB", true, true, "AFTERNOON", 4, List.of(1, 2, 3, 4)),
                new PlanningExamDto(3, "Digital Signal Processing", "EC603", 180, "CORE", 2, "ECE", "THEORY", true, false, "MORNING", 4, List.of(5, 6)),
                new PlanningExamDto(4, "Discrete Mathematics", "MA401", 120, "ELECTIVE", 3, "MATH", "THEORY", false, false, "MORNING", 3, List.of(1, 5, 7, 8)),
                new PlanningExamDto(5, "Optimization Techniques", "MA402", 120, "ELECTIVE", 3, "MATH", "THEORY", false, false, "AFTERNOON", 3, List.of(2, 6, 7, 8))
            )
        );
    }

    private int safePositive(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private long slotDurationMinutes(PlanningSlotDto slot) {
        if (slot == null || slot.startTime() == null || slot.endTime() == null) {
            return 0L;
        }
        try {
            LocalTime start = LocalTime.parse(slot.startTime());
            LocalTime end = LocalTime.parse(slot.endTime());
            return Math.max(0L, java.time.Duration.between(start, end).toMinutes());
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<Integer> distinctPositiveIds(List<Integer> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }
}
