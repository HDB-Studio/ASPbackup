package com.aspbackup.logging;

import java.time.Instant;

/**
 * Represents a single log entry for backup operations.
 */
public class LogEntry {

    private final Instant timestamp;
    private final String level;
    private final String taskId;
    private final String message;

    public LogEntry(String level, String taskId, String message) {
        this.timestamp = Instant.now();
        this.level = level;
        this.taskId = taskId;
        this.message = message;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getTaskId() { return taskId; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s] %s",
                timestamp.toString(), level, taskId != null ? taskId : "SYSTEM", message);
    }
}