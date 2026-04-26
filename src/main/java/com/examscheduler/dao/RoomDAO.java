package com.examscheduler.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bson.Document;

import com.examscheduler.model.Room;

public class RoomDAO extends BaseDAO {

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();

        try {
            for (Document doc : collection("room").find()) {
                rooms.add(mapDoc(doc));
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to load rooms from MongoDB", ex);
        }

        rooms.sort(Comparator.comparingInt(Room::getCapacity));
        return rooms;
    }

    public Room findById(int id) throws SQLException {
        try {
            Document doc = collection("room").find(new Document("id", id)).first();
            return doc == null ? null : mapDoc(doc);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to find room by id in MongoDB", ex);
        }
    }

    /** Finds rooms that can fit at least minCapacity students, sorted by capacity ascending.
     *  The scheduler picks the smallest valid room to avoid wasting large halls. */
    public List<Room> findRoomsWithMinCapacity(int minCapacity) throws SQLException {
        List<Room> rooms = new ArrayList<>();

        try {
            for (Document doc : collection("room").find()) {
                Room room = mapDoc(doc);
                if (room.getCapacity() >= minCapacity) {
                    rooms.add(room);
                }
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to query rooms by capacity in MongoDB", ex);
        }

        rooms.sort(Comparator.comparingInt(Room::getCapacity));
        return rooms;
    }

    private Room mapDoc(Document rs) {
        return new Room(
            getInt(rs, "id", 0),
            firstString(rs, "name"),
            firstInt(rs, "capacity"),
            firstBoolean(rs, "hasProjector", "has_projector"),
            firstBoolean(rs, "hasComputers", "has_computers"),
            firstString(rs, "building"),
            firstString(rs, "seatingType", "seating_type")
        );
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

    private int firstInt(Document doc, String key) {
        Object value = doc.get(key);
        return value instanceof Number n ? n.intValue() : 0;
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
