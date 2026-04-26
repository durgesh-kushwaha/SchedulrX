package com.examscheduler.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import com.examscheduler.model.Exam;
import com.examscheduler.model.Teacher;

public class ExamDAO extends BaseDAO {

    /**
     * Loads all exams with their teacher and enrollment count in one query.
     * The enrollment count is used for sorting and room selection.
     */
    public List<Exam> findAllWithEnrollmentCount() throws SQLException {
        List<Exam> exams = new ArrayList<>();

        try {
            Map<Integer, Teacher> teacherById = new HashMap<>();
            for (Document t : collection("teacher").find()) {
                int id = valueAsInt(t, "id", 0);
                Teacher teacher = new Teacher(
                    id,
                    firstString(t, "name", "teacher_name"),
                    firstString(t, "department"),
                    firstString(t, "email")
                );
                Object unavailableRaw = t.get("unavailableSlotIds");
                if (unavailableRaw instanceof List<?> unavailableSlots) {
                    for (Object slotId : unavailableSlots) {
                        if (slotId instanceof Number number) {
                            teacher.addUnavailableSlot(number.intValue());
                        }
                    }
                }
                teacherById.put(id, teacher);
            }

            Map<Integer, Integer> enrollmentCountByExam = new HashMap<>();
            for (Document en : collection("enrollment").find()) {
                int examId = firstInt(en, "examId", "exam_id");
                enrollmentCountByExam.merge(examId, 1, Integer::sum);
            }

            for (Document e : collection("exam").find()) {
                int examId = valueAsInt(e, "id", 0);
                int teacherId = firstInt(e, "teacherId", "teacher_id");
                Teacher teacher = teacherById.get(teacherId);

                String priorityRaw = firstString(e, "priority");
                Exam.Priority priority = Exam.Priority.CORE;
                if (priorityRaw != null && !priorityRaw.isBlank()) {
                    priority = Exam.Priority.valueOf(priorityRaw.toUpperCase());
                }

                Exam exam = new Exam(
                    examId,
                    firstString(e, "subjectName", "subject_name"),
                    firstString(e, "subjectCode", "subject_code"),
                    firstInt(e, "durationMinutes", "duration_minutes"),
                    priority,
                    teacher
                );
                exam.setDepartment(firstString(e, "department"));
                exam.setRequiresProjector(firstBoolean(e, "requiresProjector", "requires_projector"));
                exam.setRequiresComputers(firstBoolean(e, "requiresComputers", "requires_computers"));
                exam.setPreferredSession(firstString(e, "preferredSession", "preferred_session"));
                exam.setDifficultyLevel(firstInt(e, "difficultyLevel", "difficulty_level"));
                String examTypeRaw = firstString(e, "examType", "exam_type");
                if (examTypeRaw != null && !examTypeRaw.isBlank()) {
                    exam.setExamType(Exam.ExamType.valueOf(examTypeRaw.toUpperCase()));
                }
                exam.setEnrollmentCount(enrollmentCountByExam.getOrDefault(examId, 0));
                exams.add(exam);
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to load exams from MongoDB", ex);
        }

        exams.sort(Comparator
            .comparing((Exam e) -> e.getPriority() == Exam.Priority.CORE ? 0 : 1)
            .thenComparingInt(e -> -e.getEnrollmentCount()));

        return exams;
    }

    /**
     * Builds the studentExamMap needed by overlap constraints.
     * Returns Map<examId, Set<studentId>> - loaded once, reused throughout solving.
     */
    public Map<Integer, Set<Integer>> buildStudentExamMap() throws SQLException {
        Map<Integer, Set<Integer>> map = new HashMap<>();

        try {
            for (Document en : collection("enrollment").find()) {
                int examId = firstInt(en, "examId", "exam_id");
                int studentId = firstInt(en, "studentId", "student_id");
                map.computeIfAbsent(examId, k -> new HashSet<>()).add(studentId);
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to load enrollments from MongoDB", ex);
        }
        return map;
    }

    private int firstInt(Document doc, String primary, String fallback) {
        Object first = doc.get(primary);
        if (first instanceof Number n) {
            return n.intValue();
        }
        Object second = doc.get(fallback);
        if (second instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private int valueAsInt(Document doc, String key, int defaultValue) {
        Object value = doc.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    private String firstString(Document doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private boolean firstBoolean(Document doc, String primary, String fallback) {
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
