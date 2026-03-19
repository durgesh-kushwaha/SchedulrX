package com.examscheduler.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an exam invigilator.
 * unavailableSlots is loaded from DB or set manually before scheduling.
 */
public class Teacher {

    private int id;
    private String name;
    private String department;
    private String email;

    // Slots where this teacher is NOT available (pre-marked conflicts)
    private final Set<Integer> unavailableSlotIds = new HashSet<>();

    public Teacher() {}

    public Teacher(int id, String name, String department, String email) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.email = email;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String d) { this.department = d; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<Integer> getUnavailableSlotIds() { return unavailableSlotIds; }
    public void addUnavailableSlot(int slotId) { unavailableSlotIds.add(slotId); }

    public boolean isAvailableFor(int slotId) {
        return !unavailableSlotIds.contains(slotId);
    }

    @Override
    public String toString() {
        return "Teacher{id=" + id + ", name='" + name + "', dept='" + department + "'}";
    }
}
