package com.examscheduler.model;

/**
 * Represents a physical examination room.
 * capacity is the hard upper bound - never exceeded by the scheduler.
 */
public class Room {

    private int id;
    private String name;
    private int capacity;
    private boolean hasProjector;
    private boolean hasComputers;
    private String building;
    private String seatingType;

    public Room() {}

    public Room(int id, String name, int capacity, boolean hasProjector, String building) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.hasProjector = hasProjector;
        this.building = building;
    }

    public Room(int id, String name, int capacity, boolean hasProjector, boolean hasComputers,
                String building, String seatingType) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.hasProjector = hasProjector;
        this.hasComputers = hasComputers;
        this.building = building;
        this.seatingType = seatingType;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isHasProjector() { return hasProjector; }
    public void setHasProjector(boolean hp) { this.hasProjector = hp; }

    public boolean isHasComputers() { return hasComputers; }
    public void setHasComputers(boolean hasComputers) { this.hasComputers = hasComputers; }

    public String getBuilding() { return building; }
    public void setBuilding(String b) { this.building = b; }

    public String getSeatingType() { return seatingType; }
    public void setSeatingType(String seatingType) { this.seatingType = seatingType; }

    /**
     * Can this room fit the given number of students?
     */
    public boolean canFit(int studentCount) {
        return this.capacity >= studentCount;
    }

    @Override
    public String toString() {
        return "Room{id=" + id + ", name='" + name
             + "', capacity=" + capacity + ", building='" + building + "'}";
    }
}
