package com.aspbackup.logging;

import com.aspbackup.ASPBackup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Handles backup-specific logging to daily log files and optional console output.
 */
public class BackupLogger {

    private final ASPBackup plugin;
    private final Path logDirectory;
    private final boolean consoleOutput;
    private final String logLevel;
    private final int retentionDays;

    private BufferedWriter writer;
    private String currentDate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BackupLogger(ASPBackup plugin) {
        this.plugin = plugin;
        var config = plugin.getConfigManager();
        this.logDirectory = Path.of(config.getLogDirectory());
        this.consoleOutput = config.isConsoleOutput();
        this.logLevel = config.getLogLevel().toUpperCase();
        this.retentionDays = config.getLogRetentionDays();

        try {
            Files.createDirectories(logDirectory);
            cleanupOldLogs();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create log directory: " + logDirectory, e);
        }
    }

    /**
     * Log a backup operation message.
     */
    public synchronized void log(String level, String taskId, String message) {
        if (!shouldLog(level)) return;

        LogEntry entry = new LogEntry(level, taskId, message);
        String line = entry.toString();

        // Console output
        if (consoleOutput) {
            Level bukkitLevel = switch (level) {
                case "ERROR" -> Level.SEVERE;
                case "WARN" -> Level.WARNING;
                case "DEBUG" -> Level.FINE;
                default -> Level.INFO;
            };
            plugin.getLogger().log(bukkitLevel, "[ASPbackup] " + line);
        }

        // File output
        try {
            rotateLogIfNeeded();
            if (writer != null) {
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write to backup log file", e);
        }
    }

    public void info(String taskId, String message) {
        log("INFO", taskId, message);
    }

    public void warn(String taskId, String message) {
        log("WARN", taskId, message);
    }

    public void error(String taskId, String message) {
        log("ERROR", taskId, message);
    }

    public void debug(String taskId, String message) {
        log("DEBUG", taskId, message);
    }

    /**
     * Log a backup start event.
     */
    public void logBackupStart(String taskId, String type, String source, String target) {
        info(taskId, String.format("Backup started: type=%s, source=%s, target=%s", type, source, target));
    }

    /**
     * Log a backup progress update.
     */
    public void logBackupProgress(String taskId, double percent, long filesDone, long bytesDone) {
        debug(taskId, String.format("Progress: %.1f%% (%d files, %d bytes)", percent, filesDone, bytesDone));
    }

    /**
     * Log a backup completion.
     */
    public void logBackupComplete(String taskId, long fileSize, long durationMs, String checksum) {
        info(taskId, String.format("Backup completed: size=%d bytes, duration=%dms, checksum=%s",
                fileSize, durationMs, checksum));
    }

    /**
     * Log a backup error.
     */
    public void logBackupError(String taskId, String stage, String errorMessage) {
        error(taskId, String.format("Backup error at stage '%s': %s", stage, errorMessage));
    }

    /**
     * Log a transfer chunk event.
     */
    public void logTransferChunk(String taskId, String nodeId, int chunkIndex, boolean success) {
        if (plugin.getConfigManager().isVerboseTransfer()) {
            debug(taskId, String.format("Chunk %d -> %s: %s", chunkIndex, nodeId, success ? "OK" : "FAIL"));
        }
    }

    /**
     * Close the log writer.
     */
    public synchronized void close() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close backup log file", e);
        }
    }

    private boolean shouldLog(String level) {
        return switch (logLevel) {
            case "DEBUG" -> true;
            case "INFO" -> !level.equals("DEBUG");
            case "WARN" -> level.equals("WARN") || level.equals("ERROR");
            case "ERROR" -> level.equals("ERROR");
            default -> true;
        };
    }

    private void rotateLogIfNeeded() throws IOException {
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(currentDate)) {
            if (writer != null) {
                writer.close();
            }
            Path logFile = logDirectory.resolve("backup-" + today + ".log");
            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(logFile.toFile(), true),
                            StandardCharsets.UTF_8));
            currentDate = today;
        }
    }

    private void cleanupOldLogs() {
        try {
            LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDirectory, "backup-*.log")) {
                for (Path file : stream) {
                    String name = file.getFileName().toString();
                    String dateStr = name.replace("backup-", "").replace(".log", "");
                    try {
                        LocalDate fileDate = LocalDate.parse(dateStr, DATE_FMT);
                        if (fileDate.isBefore(cutoff)) {
                            Files.delete(file);
                            plugin.getLogger().fine("Deleted old log file: " + name);
                        }
                    } catch (Exception ignored) {
                        // Skip files that don't match the naming pattern
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clean up old log files", e);
        }
    }
}