package com.examscheduler.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.examscheduler.model.Teacher;

public class TeacherDAO extends BaseDAO {

    public List<Teacher> findAll() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();

        try {
            for (Document rs : collection("teacher").find()) {
                teachers.add(mapDoc(rs));
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to load teachers from MongoDB", ex);
        }
        return teachers;
    }

    public Teacher findById(int id) throws SQLException {
        try {
            Document rs = collection("teacher").find(new Document("id", id)).first();
            if (rs != null) {
                return mapDoc(rs);
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to find teacher by id in MongoDB", ex);
        }
        return null;
    }

    private Teacher mapDoc(Document rs) {
        return new Teacher(
            getInt(rs, "id", 0),
            getString(rs, "name"),
            getString(rs, "department"),
            getString(rs, "email")
        );
    }
}
