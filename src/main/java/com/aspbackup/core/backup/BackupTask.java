package com.aspbackup.core.backup;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a single backup operation with its metadata and progress tracking.
 * Thread-safe for concurrent access from the backup worker and the command executor.
 */
public class BackupTask {

    private final String taskId;
    private final BackupType type;
    private final String targetId;
    private final Instant startTime;
    private final AtomicReference<BackupState> state;
    private final AtomicBoolean interruptRequested;

    // Progress tracking (atomic for thread-safe updates)
    private final AtomicLong totalFiles = new AtomicLong(0);
    private final AtomicLong filesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong bytesProcessed = new AtomicLong(0);

    // Result
    private volatile String checksum;
    private volatile long finalSize;
    private volatile long durationMs;

    public BackupTask(String taskId, BackupType type, String targetId) {
        this.taskId = taskId;
        this.type = type;
        this.targetId = targetId;
        this.startTime = Instant.now();
        this.state = new AtomicReference<>(BackupState.INITIALIZING);
        this.interruptRequested = new AtomicBoolean(false);
    }

    // --- Identity ---

    public String getTaskId() { return taskId; }
    public BackupType getType() { return type; }
    public String getTargetId() { return targetId; }
    public Instant getStartTime() { return startTime; }

    // --- State ---

    public BackupState getState() { return state.get(); }
    public void setState(BackupState newState) { state.set(newState); }

    /**
     * Request interruption of this backup task.
     * The task checks this flag at safe boundaries and will pause gracefully.
     */
    public void requestInterrupt() {
        interruptRequested.set(true);
    }

    public boolean isInterrupted() {
        return interruptRequested.get();
    }

    // --- Progress ---

    public long getTotalFiles() { return totalFiles.get(); }
    public void setTotalFiles(long n) { totalFiles.set(n); }

    public long getFilesProcessed() { return filesProcessed.get(); }
    public void setFilesProcessed(long n) { filesProcessed.set(n); }

    public long getTotalBytes() { return totalBytes.get(); }
    public void setTotalBytes(long n) { totalBytes.set(n); }

    public long getBytesProcessed() { return bytesProcessed.get(); }
    public void setBytesProcessed(long n) { bytesProcessed.set(n); }

    public double getProgress() {
        long tb = totalBytes.get();
        if (tb == 0) return 0.0;
        return (bytesProcessed.get() * 100.0) / tb;
    }

    // --- Result ---

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public long getFinalSize() { return finalSize; }
    public void setFinalSize(long finalSize) { this.finalSize = finalSize; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    @Override
    public String toString() {
        return String.format("BackupTask[id=%s, type=%s, state=%s, progress=%.1f%%]",
                taskId, type, state.get(), getProgress());
    }
}