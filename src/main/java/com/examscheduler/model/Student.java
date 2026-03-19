package com.examscheduler.model;

/**
 * Represents a student enrolled in one or more exams.
 */
public class Student {

    private int id;
    private String name;
    private String rollNo;
    private int semester;
    private String branch;

    public Student() {}

    public Student(int id, String name, String rollNo, int semester, String branch) {
        this.id = id;
        this.name = name;
        this.rollNo = rollNo;
        this.semester = semester;
        this.branch = branch;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String r) { this.rollNo = r; }

    public int getSemester() { return semester; }
    public void setSemester(int s) { this.semester = s; }

    public String getBranch() { return branch; }
    public void setBranch(String b) { this.branch = b; }

    @Override
    public String toString() {
        return "Student{id=" + id + ", rollNo='" + rollNo
             + "', name='" + name + "', sem=" + semester + "}";
    }
}
