package com.examscheduler.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Singleton MongoDB connection manager.
 *
 * Defaults to a local MongoDB instance and can be overridden by environment:
 * - MONGODB_URI
 * - MONGODB_DATABASE
 */
public final class MongoConnection {

    private static final String DEFAULT_URI = "mongodb://localhost:27017/exam_scheduler";
    private static final String DEFAULT_DB = "exam_scheduler";

    private static MongoClient client;
    private static String databaseName;

    private MongoConnection() {
    }

    public static synchronized MongoDatabase getDatabase() {
        if (client == null) {
            String uri = read("MONGODB_URI", DEFAULT_URI);
            databaseName = read("MONGODB_DATABASE", extractDbFromUri(uri, DEFAULT_DB));

            ConnectionString connectionString = new ConnectionString(uri);
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

            client = MongoClients.create(settings);
            System.out.println("[DB] MongoDB connection established: " + uri);
        }
        return client.getDatabase(databaseName);
    }

    public static synchronized void closeConnection() {
        if (client != null) {
            client.close();
            client = null;
            System.out.println("[DB] MongoDB connection closed.");
        }
    }

    private static String read(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String extractDbFromUri(String uri, String fallback) {
        int slash = uri.lastIndexOf('/');
        if (slash < 0 || slash == uri.length() - 1) {
            return fallback;
        }
        String tail = uri.substring(slash + 1);
        int queryIdx = tail.indexOf('?');
        String db = queryIdx >= 0 ? tail.substring(0, queryIdx) : tail;
        if (db.isBlank()) {
            return fallback;
        }
        return db;
    }
}