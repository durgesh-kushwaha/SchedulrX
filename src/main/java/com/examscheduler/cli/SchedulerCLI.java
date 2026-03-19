package com.examscheduler.cli;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.examscheduler.controller.ScheduleController;
import com.examscheduler.model.ScheduledExam;
import com.examscheduler.util.DBConnection;

/**
 * Command-Line Interface for the Smart Exam Scheduler.
 *
 * Run with: java -jar SmartExamScheduler.jar
 * Or from IDE: run this class directly.
 */
public class SchedulerCLI {

    private static final String DIVIDER =
        "===========================================================";

    private static final ScheduleController controller = new ScheduleController();

    public static void main(String[] args) {
        printBanner();

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> generateAndPrint();
                    case "2" -> { System.out.println("Coming soon: View existing timetable."); }
                    case "3" -> { System.out.println("Coming soon: Add/edit exams."); }
                    case "0" -> {
                        System.out.println("\nShutting down. Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            }
        } finally {
            DBConnection.closeConnection();
        }
    }

    // Menu actions

    private static void generateAndPrint() {
        System.out.println("\n" + DIVIDER);
        System.out.println(" GENERATING EXAM TIMETABLE");
        System.out.println(DIVIDER);

        try {
            List<ScheduledExam> schedule = controller.generateTimetable();
            printTimetable(schedule);
            printConflictReport(schedule);
        } catch (SQLException e) {
            System.err.println("\n[ERROR] Database error: " + e.getMessage());
            System.err.println("Check MongoDB local instance and MONGODB_URI/MONGODB_DATABASE values.");
        }
    }

    // Display helpers

    private static void printTimetable(List<ScheduledExam> schedule) {
        List<ScheduledExam> placed = schedule.stream()
            .filter(ScheduledExam::isScheduled)
            .sorted((a, b) -> {
                int dateCmp = a.getSlot().getExamDate()
                               .compareTo(b.getSlot().getExamDate());
                if (dateCmp != 0) return dateCmp;
                return a.getSlot().getStartTime()
                        .compareTo(b.getSlot().getStartTime());
            })
            .collect(Collectors.toList());

        System.out.println("\n FINAL TIMETABLE (" + placed.size() + " exams scheduled)\n");

        // Table header
        System.out.printf("%-12s %-10s %-12s %-25s %-20s %-10s%n",
            "DATE", "START", "END", "SUBJECT", "ROOM", "TEACHER");
        System.out.println("-".repeat(95));

        String lastDate = "";
        for (ScheduledExam se : placed) {
            String date = se.getSlot().getExamDate().toString();

            // Print a blank line between different dates for readability
            if (!date.equals(lastDate) && !lastDate.isEmpty()) {
                System.out.println();
            }
            lastDate = date;

            String teacherName = se.getExam().getTeacher() != null
                ? se.getExam().getTeacher().getName() : "TBD";

            System.out.printf("%-12s %-10s %-12s %-25s %-20s %-10s%n",
                date,
                se.getSlot().getStartTime(),
                se.getSlot().getEndTime(),
                se.getExam().getSubjectCode() + " " + se.getExam().getSubjectName(),
                se.getRoom().getName(),
                teacherName
            );
        }
        System.out.println("-".repeat(95));
    }

    private static void printConflictReport(List<ScheduledExam> schedule) {
        List<ScheduledExam> unplaced = schedule.stream()
            .filter(se -> !se.isScheduled())
            .collect(Collectors.toList());

        if (unplaced.isEmpty()) {
            System.out.println("\n All exams scheduled successfully - no conflicts!");
            return;
        }

        System.out.println("\n CONFLICT REPORT (" + unplaced.size() + " unplaced)");
        System.out.println("-".repeat(60));
        for (ScheduledExam se : unplaced) {
            System.out.println("  [" + se.getStatus() + "] "
                + se.getExam().getSubjectCode()
                + " - " + se.getConflictReason());
        }
    }

    private static void printBanner() {
        System.out.println(DIVIDER);
        System.out.println("        SMART EXAM SCHEDULING SYSTEM v1.0");
        System.out.println("        Constraint Satisfaction Problem Solver");
        System.out.println(DIVIDER);
    }

    private static void printMenu() {
        System.out.println("\n  1. Generate exam timetable");
        System.out.println("  2. View current timetable");
        System.out.println("  3. Manage exams / rooms");
        System.out.println("  0. Exit");
        System.out.print("\n  Choice: ");
    }
}
