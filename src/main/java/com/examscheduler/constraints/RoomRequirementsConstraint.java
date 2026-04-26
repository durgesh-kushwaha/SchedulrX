package com.examscheduler.constraints;

import java.util.List;

import com.examscheduler.model.Exam;
import com.examscheduler.model.Room;
import com.examscheduler.model.ScheduledExam;

public class RoomRequirementsConstraint implements Constraint {

    @Override
    public boolean isSatisfied(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        if (candidate == null || candidate.getExam() == null || candidate.getRoom() == null) {
            return true;
        }

        Exam exam = candidate.getExam();
        Room room = candidate.getRoom();

        if (exam.isRequiresProjector() && !room.isHasProjector()) {
            return false;
        }

        boolean needsComputers = exam.isRequiresComputers() || exam.getExamType() == Exam.ExamType.LAB;
        return !needsComputers || room.isHasComputers();
    }

    @Override
    public String getViolationMessage(ScheduledExam candidate, List<ScheduledExam> currentSchedule) {
        Exam exam = candidate.getExam();
        Room room = candidate.getRoom();
        String code = exam != null ? exam.getSubjectCode() : "UNKNOWN";
        String roomName = room != null ? room.getName() : "UNKNOWN";

        if (exam != null && exam.isRequiresProjector() && room != null && !room.isHasProjector()) {
            return "Exam '" + code + "' requires a projector but room '" + roomName + "' does not provide one";
        }
        return "Exam '" + code + "' requires computers but room '" + roomName + "' does not provide them";
    }

    @Override
    public ConstraintType getType() {
        return ConstraintType.HARD;
    }

    @Override
    public String getName() {
        return "RoomRequirements";
    }
}
