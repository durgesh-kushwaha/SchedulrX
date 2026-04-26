package com.examscheduler.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import com.examscheduler.api.dto.PlanningConfigDto;
import com.examscheduler.api.dto.PlanningDatasetDto;
import com.examscheduler.api.dto.PlanningExamDto;
import com.examscheduler.api.dto.PlanningRoomDto;
import com.examscheduler.api.dto.PlanningSlotDto;
import com.examscheduler.api.dto.PlanningStudentDto;
import com.examscheduler.api.dto.PlanningTeacherDto;
import com.mongodb.client.model.ReplaceOptions;

public class PlanningDAO extends BaseDAO {

    private static final String ACTIVE_CONFIG_ID = "ACTIVE";

    public PlanningDatasetDto loadDataset() throws SQLException {
        try {
            Document configDoc = collection("planning_config").find(new Document("_id", ACTIVE_CONFIG_ID)).first();
            PlanningConfigDto config = new PlanningConfigDto(
                firstString(configDoc, "institutionName", "institution_name"),
                firstString(configDoc, "termName", "term_name"),
                firstInt(configDoc, "minGapMinutes", "min_gap_minutes"),
                firstInt(configDoc, "maxExamsPerDay", "max_exams_per_day")
            );

            Map<Integer, List<Integer>> studentIdsByExam = new HashMap<>();
            for (Document doc : collection("enrollment").find()) {
                int examId = firstInt(doc, "examId", "exam_id");
                int studentId = firstInt(doc, "studentId", "student_id");
                studentIdsByExam.computeIfAbsent(examId, key -> new ArrayList<>()).add(studentId);
            }

            List<PlanningTeacherDto> teachers = new ArrayList<>();
            for (Document doc : collection("teacher").find()) {
                teachers.add(new PlanningTeacherDto(
                    firstInt(doc, "id", null),
                    firstString(doc, "name"),
                    firstString(doc, "department"),
                    firstString(doc, "email"),
                    integerList(doc.get("unavailableSlotIds"))
                ));
            }
            teachers.sort(Comparator.comparing(PlanningTeacherDto::id, Comparator.nullsLast(Comparator.naturalOrder())));

            List<PlanningRoomDto> rooms = new ArrayList<>();
            for (Document doc : collection("room").find()) {
                rooms.add(new PlanningRoomDto(
                    firstInt(doc, "id", null),
                    firstString(doc, "name"),
                    firstInt(doc, "capacity", null),
                    firstBoolean(doc, "hasProjector", "has_projector"),
                    firstBoolean(doc, "hasComputers", "has_computers"),
                    firstString(doc, "building"),
                    firstString(doc, "seatingType", "seating_type")
                ));
            }
            rooms.sort(Comparator.comparing(PlanningRoomDto::id, Comparator.nullsLast(Comparator.naturalOrder())));

            List<PlanningSlotDto> slots = new ArrayList<>();
            for (Document doc : collection("time_slot").find()) {
                slots.add(new PlanningSlotDto(
                    firstInt(doc, "id", null),
                    firstString(doc, "label"),
                    firstString(doc, "examDate", "exam_date"),
                    firstString(doc, "startTime", "start_time"),
                    firstString(doc, "endTime", "end_time")
                ));
            }
            slots.sort(Comparator.comparing(PlanningSlotDto::id, Comparator.nullsLast(Comparator.naturalOrder())));

            List<PlanningStudentDto> students = new ArrayList<>();
            for (Document doc : collection("student").find()) {
                students.add(new PlanningStudentDto(
                    firstInt(doc, "id", null),
                    firstString(doc, "name"),
                    firstString(doc, "rollNo", "roll_no"),
                    firstInt(doc, "semester", null),
                    firstString(doc, "branch"),
                    firstInt(doc, "extraTimeMinutes", "extra_time_minutes"),
                    firstString(doc, "specialNeedsNotes", "special_needs_notes")
                ));
            }
            students.sort(Comparator.comparing(PlanningStudentDto::id, Comparator.nullsLast(Comparator.naturalOrder())));

            List<PlanningExamDto> exams = new ArrayList<>();
            for (Document doc : collection("exam").find()) {
                Integer examIdValue = firstInt(doc, "id", null);
                int examId = examIdValue == null ? 0 : examIdValue;
                List<Integer> studentIds = studentIdsByExam.getOrDefault(examId, List.of());
                exams.add(new PlanningExamDto(
                    examId,
                    firstString(doc, "subjectName", "subject_name"),
                    firstString(doc, "subjectCode", "subject_code"),
                    firstInt(doc, "durationMinutes", "duration_minutes"),
                    firstString(doc, "priority"),
                    firstInt(doc, "teacherId", "teacher_id"),
                    firstString(doc, "department"),
                    firstString(doc, "examType", "exam_type"),
                    firstBoolean(doc, "requiresProjector", "requires_projector"),
                    firstBoolean(doc, "requiresComputers", "requires_computers"),
                    firstString(doc, "preferredSession", "preferred_session"),
                    firstInt(doc, "difficultyLevel", "difficulty_level"),
                    List.copyOf(studentIds)
                ));
            }
            exams.sort(Comparator.comparing(PlanningExamDto::id, Comparator.nullsLast(Comparator.naturalOrder())));

            return new PlanningDatasetDto(config, teachers, rooms, slots, students, exams);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to load planning dataset from MongoDB", ex);
        }
    }

    public void replaceDataset(PlanningDatasetDto dataset) throws SQLException {
        try {
            PlanningDatasetDto safe = dataset == null
                ? new PlanningDatasetDto(null, List.of(), List.of(), List.of(), List.of(), List.of())
                : dataset;

            clearScenarioCollections();
            saveConfig(safe.config());
            saveTeachers(safe.teachers());
            saveRooms(safe.rooms());
            saveSlots(safe.slots());
            saveStudents(safe.students());
            saveExamsAndEnrollments(safe.exams());
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to save planning dataset to MongoDB", ex);
        }
    }

    private void clearScenarioCollections() {
        collection("teacher").deleteMany(new Document());
        collection("room").deleteMany(new Document());
        collection("time_slot").deleteMany(new Document());
        collection("student").deleteMany(new Document());
        collection("exam").deleteMany(new Document());
        collection("enrollment").deleteMany(new Document());
        collection("scheduled_exam").deleteMany(new Document());
        collection("exam_override").deleteMany(new Document());
    }

    private void saveConfig(PlanningConfigDto config) {
        PlanningConfigDto safe = config == null
            ? new PlanningConfigDto(null, null, null, null)
            : config;
        Document doc = new Document("_id", ACTIVE_CONFIG_ID)
            .append("institutionName", safe.institutionName())
            .append("termName", safe.termName())
            .append("minGapMinutes", safe.minGapMinutes())
            .append("maxExamsPerDay", safe.maxExamsPerDay());
        collection("planning_config").replaceOne(
            new Document("_id", ACTIVE_CONFIG_ID),
            doc,
            new ReplaceOptions().upsert(true)
        );
    }

    private void saveTeachers(List<PlanningTeacherDto> teachers) {
        List<Document> docs = new ArrayList<>();
        for (PlanningTeacherDto teacher : safeList(teachers)) {
            int id = positiveOrNext(teacher.id(), "teacher");
            docs.add(new Document()
                .append("id", id)
                .append("name", teacher.name())
                .append("department", teacher.department())
                .append("email", teacher.email())
                .append("unavailableSlotIds", distinctPositiveIds(teacher.unavailableSlotIds())));
        }
        if (!docs.isEmpty()) {
            collection("teacher").insertMany(docs);
        }
    }

    private void saveRooms(List<PlanningRoomDto> rooms) {
        List<Document> docs = new ArrayList<>();
        for (PlanningRoomDto room : safeList(rooms)) {
            int id = positiveOrNext(room.id(), "room");
            docs.add(new Document()
                .append("id", id)
                .append("name", room.name())
                .append("capacity", room.capacity())
                .append("hasProjector", Boolean.TRUE.equals(room.hasProjector()))
                .append("hasComputers", Boolean.TRUE.equals(room.hasComputers()))
                .append("building", room.building())
                .append("seatingType", room.seatingType()));
        }
        if (!docs.isEmpty()) {
            collection("room").insertMany(docs);
        }
    }

    private void saveSlots(List<PlanningSlotDto> slots) {
        List<Document> docs = new ArrayList<>();
        for (PlanningSlotDto slot : safeList(slots)) {
            int id = positiveOrNext(slot.id(), "time_slot");
            docs.add(new Document()
                .append("id", id)
                .append("label", slot.label())
                .append("examDate", slot.examDate())
                .append("startTime", slot.startTime())
                .append("endTime", slot.endTime()));
        }
        if (!docs.isEmpty()) {
            collection("time_slot").insertMany(docs);
        }
    }

    private void saveStudents(List<PlanningStudentDto> students) {
        List<Document> docs = new ArrayList<>();
        for (PlanningStudentDto student : safeList(students)) {
            int id = positiveOrNext(student.id(), "student");
            docs.add(new Document()
                .append("id", id)
                .append("name", student.name())
                .append("rollNo", student.rollNo())
                .append("semester", student.semester())
                .append("branch", student.branch())
                .append("extraTimeMinutes", student.extraTimeMinutes())
                .append("specialNeedsNotes", student.specialNeedsNotes()));
        }
        if (!docs.isEmpty()) {
            collection("student").insertMany(docs);
        }
    }

    private void saveExamsAndEnrollments(List<PlanningExamDto> exams) {
        List<Document> examDocs = new ArrayList<>();
        List<Document> enrollmentDocs = new ArrayList<>();

        for (PlanningExamDto exam : safeList(exams)) {
            int id = positiveOrNext(exam.id(), "exam");
            examDocs.add(new Document()
                .append("id", id)
                .append("subjectName", exam.subjectName())
                .append("subjectCode", exam.subjectCode())
                .append("durationMinutes", exam.durationMinutes())
                .append("priority", exam.priority())
                .append("teacherId", exam.teacherId())
                .append("department", exam.department())
                .append("examType", exam.examType())
                .append("requiresProjector", Boolean.TRUE.equals(exam.requiresProjector()))
                .append("requiresComputers", Boolean.TRUE.equals(exam.requiresComputers()))
                .append("preferredSession", exam.preferredSession())
                .append("difficultyLevel", exam.difficultyLevel()));

            for (Integer studentId : distinctPositiveIds(exam.studentIds())) {
                enrollmentDocs.add(new Document()
                    .append("id", nextSequence("enrollment"))
                    .append("studentId", studentId)
                    .append("examId", id));
            }
        }

        if (!examDocs.isEmpty()) {
            collection("exam").insertMany(examDocs);
        }
        if (!enrollmentDocs.isEmpty()) {
            collection("enrollment").insertMany(enrollmentDocs);
        }
    }

    private List<Integer> integerList(Object raw) {
        if (!(raw instanceof List<?> items)) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Number number) {
                values.add(number.intValue());
            }
        }
        return values;
    }

    private List<Integer> distinctPositiveIds(List<Integer> ids) {
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer id : safeList(ids)) {
            if (id != null && id > 0) {
                unique.add(id);
            }
        }
        return new ArrayList<>(unique);
    }

    private int positiveOrNext(Integer providedId, String sequence) {
        return providedId != null && providedId > 0
            ? providedId
            : Math.toIntExact(nextSequence(sequence));
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private String firstString(Document doc, String... keys) {
        if (doc == null) {
            return null;
        }
        for (String key : keys) {
            Object value = doc.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private Integer firstInt(Document doc, String primary, String fallback) {
        if (doc == null) {
            return null;
        }
        Object first = doc.get(primary);
        if (first instanceof Number n) {
            return n.intValue();
        }
        Object second = fallback == null ? null : doc.get(fallback);
        if (second instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private boolean firstBoolean(Document doc, String primary, String fallback) {
        if (doc == null) {
            return false;
        }
        Object first = doc.get(primary);
        if (first instanceof Boolean b) {
            return b;
        }
        Object second = doc.get(fallback);
        if (second instanceof Boolean b) {
            return b;
        }
        return false;
    }
}
