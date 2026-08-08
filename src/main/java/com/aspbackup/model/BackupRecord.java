package com.aspbackup.model;

import java.time.Instant;

/**
 * Represents a completed backup record for history tracking.
 */
public class BackupRecord {

    private final String taskId;
    private final String type;
    private final String targetId;
    private final String state;
    private final long fileSize;
    private final long durationMs;
    private final String checksum;
    private final Instant timestamp;

    public BackupRecord(String taskId, String type, String targetId, String state,
                         long fileSize, long durationMs, String checksum, Instant timestamp) {
        this.taskId = taskId;
        this.type = type;
        this.targetId = targetId;
        this.state = state;
        this.fileSize = fileSize;
        this.durationMs = durationMs;
        this.checksum = checksum;
        this.timestamp = timestamp;
    }

    public String getTaskId() { return taskId; }
    public String getType() { return type; }
    public String getTargetId() { return targetId; }
    public String getState() { return state; }
    public long getFileSize() { return fileSize; }
    public long getDurationMs() { return durationMs; }
    public String getChecksum() { return checksum; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("BackupRecord[id=%s, type=%s, state=%s, size=%d, time=%s]",
                taskId, type, state, fileSize, timestamp);
    }
}