package com.examscheduler.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bson.Document;

import com.examscheduler.model.TimeSlot;

public class SlotDAO extends BaseDAO {

    public List<TimeSlot> findAll() throws SQLException {
        List<TimeSlot> slots = new ArrayList<>();

        try {
            for (Document doc : collection("time_slot").find()) {
                slots.add(mapDoc(doc));
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to load time slots from MongoDB", ex);
        }

        slots.sort(Comparator
            .comparing(TimeSlot::getExamDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TimeSlot::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

        return slots;
    }

    public TimeSlot findById(int id) throws SQLException {
        try {
            Document doc = collection("time_slot").find(new Document("id", id)).first();
            return doc == null ? null : mapDoc(doc);
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to find time slot by id in MongoDB", ex);
        }
    }

    private TimeSlot mapDoc(Document rs) {
        String dateRaw = firstString(rs, "examDate", "exam_date");
        String startRaw = firstString(rs, "startTime", "start_time");
        String endRaw = firstString(rs, "endTime", "end_time");

        return new TimeSlot(
            getInt(rs, "id", 0),
            dateRaw == null ? null : LocalDate.parse(dateRaw),
            startRaw == null ? null : LocalTime.parse(startRaw),
            endRaw == null ? null : LocalTime.parse(endRaw)
        );
    }

    private String firstString(Document doc, String primary, String fallback) {
        Object first = doc.get(primary);
        if (first != null) {
            return first.toString();
        }
        Object second = doc.get(fallback);
        return second == null ? null : second.toString();
    }
}
