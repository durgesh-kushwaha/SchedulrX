package com.examscheduler.dao;

import org.bson.Document;

import com.examscheduler.util.MongoConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

/**
 * Base class for all DAOs.
 * Provides shared Mongo database access and simple sequence generation.
 */
public abstract class BaseDAO {

    protected MongoDatabase getDatabase() {
        return MongoConnection.getDatabase();
    }

    protected MongoCollection<Document> collection(String name) {
        return getDatabase().getCollection(name);
    }

    protected long nextSequence(String sequenceName) {
        Document updated = collection("_counters").findOneAndUpdate(
            eq("_id", sequenceName),
            new Document()
                .append("$setOnInsert", new Document("_id", sequenceName))
                .append("$inc", new Document("seq", 1L)),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
        );
        return updated == null ? 1L : updated.getLong("seq");
    }

    protected int getInt(Document doc, String field, int defaultValue) {
        Object value = doc.get(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    protected long getLong(Document doc, String field, long defaultValue) {
        Object value = doc.get(field);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return defaultValue;
    }

    protected String getString(Document doc, String field) {
        Object value = doc.get(field);
        return value == null ? null : value.toString();
    }
}
