package com.examscheduler.util;

/**
 * Backward-compatible DB utility wrapper.
 *
 * The application now uses MongoDB via {@link MongoConnection}, but the CLI
 * still calls this class on shutdown. Keep this as a compatibility shim.
 */
public class DBConnection {

    private DBConnection() {
    }

    /** Call this when the application shuts down. */
    public static void closeConnection() {
        MongoConnection.closeConnection();
    }
}
