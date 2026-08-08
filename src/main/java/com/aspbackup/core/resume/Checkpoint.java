package com.aspbackup.core.resume;

/**
 * Represents a saved checkpoint for resumable backup operations.
 */
public class Checkpoint {

    private String taskId;
    private String backupType;
    private String targetId;
    private long totalFiles;
    private long filesProcessed;
    private long totalBytes;
    private long bytesProcessed;
    private long timestamp;

    public Checkpoint() {}

    public Checkpoint(String taskId, String backupType, String targetId,
                      long totalFiles, long filesProcessed,
                      long totalBytes, long bytesProcessed, long timestamp) {
        this.taskId = taskId;
        this.backupType = backupType;
        this.targetId = targetId;
        this.totalFiles = totalFiles;
        this.filesProcessed = filesProcessed;
        this.totalBytes = totalBytes;
        this.bytesProcessed = bytesProcessed;
        this.timestamp = timestamp;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getBackupType() { return backupType; }
    public void setBackupType(String backupType) { this.backupType = backupType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public long getTotalFiles() { return totalFiles; }
    public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }

    public long getFilesProcessed() { return filesProcessed; }
    public void setFilesProcessed(long filesProcessed) { this.filesProcessed = filesProcessed; }

    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }

    public long getBytesProcessed() { return bytesProcessed; }
    public void setBytesProcessed(long bytesProcessed) { this.bytesProcessed = bytesProcessed; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("Checkpoint[task=%s, type=%s, progress=%d/%d files]",
                taskId, backupType, filesProcessed, totalFiles);
    }
}