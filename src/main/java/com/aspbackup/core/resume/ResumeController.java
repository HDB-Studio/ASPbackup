package com.aspbackup.core.resume;

import com.aspbackup.ASPBackup;
import com.aspbackup.core.backup.BackupTask;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Controls checkpoint save/load for backup resume functionality.
 */
public class ResumeController {

    private final ASPBackup plugin;
    private final CheckpointStore store;

    public ResumeController(ASPBackup plugin) {
        this.plugin = plugin;
        this.store = new CheckpointStore(plugin);
    }

    /**
     * Save a checkpoint for a backup task.
     */
    public void saveCheckpoint(BackupTask task) {
        if (!plugin.getConfigManager().isCheckpointEnabled()) return;

        Checkpoint checkpoint = new Checkpoint(
                task.getTaskId(),
                task.getType().name(),
                task.getTargetId(),
                task.getTotalFiles(),
                task.getFilesProcessed(),
                task.getTotalBytes(),
                task.getBytesProcessed(),
                System.currentTimeMillis()
        );

        try {
            store.save(checkpoint);
            plugin.getBackupLogger().info(task.getTaskId(), "Checkpoint saved: " +
                    task.getFilesProcessed() + "/" + task.getTotalFiles() + " files processed");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save checkpoint for task " + task.getTaskId(), e);
        }
    }

    /**
     * Load a checkpoint for a task.
     */
    public Checkpoint loadCheckpoint(String taskId) {
        try {
            return store.load(taskId);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load checkpoint for task " + taskId, e);
            return null;
        }
    }

    /**
     * Delete a checkpoint after successful resume.
     */
    public void deleteCheckpoint(String taskId) {
        try {
            store.delete(taskId);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete checkpoint for task " + taskId, e);
        }
    }

    public boolean hasCheckpoint(String taskId) {
        return store.exists(taskId);
    }

    public void cleanupStale() {
        int maxAge = plugin.getConfigManager().getCheckpointMaxAgeDays();
        store.cleanup(maxAge);
    }
}