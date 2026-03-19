package com.examscheduler.dao;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.examscheduler.api.dto.NotificationResponse;

public class NotificationDAO extends BaseDAO {

    public void notifyAllUsers(String type, String title, String message) throws SQLException {
        try {
            List<Document> toInsert = new ArrayList<>();
            for (Document user : collection("app_user").find()) {
                long notificationId = nextSequence("notification");
                long userId = getLong(user, "id", 0L);
                toInsert.add(new Document()
                    .append("id", notificationId)
                    .append("userId", userId)
                    .append("notificationType", type)
                    .append("title", title)
                    .append("message", message)
                    .append("isRead", false)
                    .append("createdAt", Date.from(Instant.now())));
            }
            if (!toInsert.isEmpty()) {
                collection("notification").insertMany(toInsert);
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to create notifications in MongoDB", ex);
        }
    }

    public List<NotificationResponse> findByUsername(String username, int page, int size) throws SQLException {
        List<NotificationResponse> rows = new ArrayList<>();

        try {
            Document user = collection("app_user").find(new Document("username", username)).first();
            if (user == null) {
                return rows;
            }

            long userId = getLong(user, "id", 0L);
            List<Document> docs = new ArrayList<>();
            for (Document doc : collection("notification").find(new Document("userId", userId))) {
                docs.add(doc);
            }

            docs.sort(Comparator.comparing(this::createdAtInstant).reversed());

            int from = Math.max(0, page * size);
            int to = Math.min(docs.size(), from + size);
            if (from >= docs.size()) {
                return rows;
            }

            for (Document rs : docs.subList(from, to)) {
                rows.add(new NotificationResponse(
                    getLong(rs, "id", 0L),
                    firstString(rs, "notificationType", "notification_type"),
                    firstString(rs, "title"),
                    firstString(rs, "message"),
                    firstBoolean(rs, "isRead", "is_read"),
                    createdAtInstant(rs)
                ));
            }
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to read notifications from MongoDB", ex);
        }

        return rows;
    }

    public long countByUsername(String username) throws SQLException {
        try {
            Document user = collection("app_user").find(new Document("username", username)).first();
            if (user == null) {
                return 0L;
            }
            long userId = getLong(user, "id", 0L);
            return collection("notification").countDocuments(new Document("userId", userId));
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to count notifications from MongoDB", ex);
        }
    }

    public void markRead(long id, String username) throws SQLException {
        try {
            Document user = collection("app_user").find(new Document("username", username)).first();
            if (user == null) {
                return;
            }
            long userId = getLong(user, "id", 0L);
            collection("notification").updateOne(
                new Document("id", id).append("userId", userId),
                new Document("$set", new Document("isRead", true))
            );
        } catch (RuntimeException ex) {
            throw new SQLException("Failed to mark notification read in MongoDB", ex);
        }
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

    private boolean firstBoolean(Document doc, String... keys) {
        for (String key : keys) {
            Object value = doc.get(key);
            if (value instanceof Boolean bool) {
                return bool;
            }
        }
        return false;
    }

    private Instant createdAtInstant(Document doc) {
        Object value = doc.get("createdAt");
        if (value == null) {
            value = doc.get("created_at");
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        return Instant.EPOCH;
    }
}
