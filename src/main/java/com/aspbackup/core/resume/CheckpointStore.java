package com.aspbackup.core.resume;

import com.aspbackup.ASPBackup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Stores and retrieves checkpoint files on disk using Java Properties format.
 */
public class CheckpointStore {

    private final ASPBackup plugin;
    private final Path checkpointDir;

    public CheckpointStore(ASPBackup plugin) {
        this.plugin = plugin;
        this.checkpointDir = Path.of(plugin.getConfigManager().getCheckpointDirectory());
        try {
            Files.createDirectories(checkpointDir);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create checkpoint directory", e);
        }
    }

    /**
     * Save a checkpoint to disk.
     */
    public void save(Checkpoint checkpoint) throws IOException {
        Path file = checkpointFile(checkpoint.getTaskId());
        Properties props = new Properties();
        props.setProperty("taskId", checkpoint.getTaskId());
        props.setProperty("backupType", checkpoint.getBackupType());
        props.setProperty("targetId", checkpoint.getTargetId());
        props.setProperty("totalFiles", String.valueOf(checkpoint.getTotalFiles()));
        props.setProperty("filesProcessed", String.valueOf(checkpoint.getFilesProcessed()));
        props.setProperty("totalBytes", String.valueOf(checkpoint.getTotalBytes()));
        props.setProperty("bytesProcessed", String.valueOf(checkpoint.getBytesProcessed()));
        props.setProperty("timestamp", String.valueOf(checkpoint.getTimestamp()));

        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "ASPbackup Checkpoint - " + checkpoint.getTaskId());
        }
    }

    /**
     * Load a checkpoint from disk.
     */
    public Checkpoint load(String taskId) throws IOException {
        Path file = checkpointFile(taskId);
        if (!Files.exists(file)) return null;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }

        Checkpoint cp = new Checkpoint();
        cp.setTaskId(props.getProperty("taskId", taskId));
        cp.setBackupType(props.getProperty("backupType", "FULL"));
        cp.setTargetId(props.getProperty("targetId", "local"));
        cp.setTotalFiles(Long.parseLong(props.getProperty("totalFiles", "0")));
        cp.setFilesProcessed(Long.parseLong(props.getProperty("filesProcessed", "0")));
        cp.setTotalBytes(Long.parseLong(props.getProperty("totalBytes", "0")));
        cp.setBytesProcessed(Long.parseLong(props.getProperty("bytesProcessed", "0")));
        cp.setTimestamp(Long.parseLong(props.getProperty("timestamp", "0")));
        return cp;
    }

    /**
     * Delete a checkpoint.
     */
    public void delete(String taskId) throws IOException {
        Files.deleteIfExists(checkpointFile(taskId));
    }

    /**
     * Check if a checkpoint exists.
     */
    public boolean exists(String taskId) {
        return Files.exists(checkpointFile(taskId));
    }

    /**
     * Clean up checkpoints older than the specified number of days.
     */
    public void cleanup(int maxAgeDays) {
        try {
            Instant cutoff = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(checkpointDir, "*.properties")) {
                for (Path file : stream) {
                    try {
                        if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                            Files.delete(file);
                            plugin.getLogger().fine("Deleted stale checkpoint: " + file.getFileName());
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clean up stale checkpoints", e);
        }
    }

    private Path checkpointFile(String taskId) {
        return checkpointDir.resolve(taskId + ".properties");
    }
}