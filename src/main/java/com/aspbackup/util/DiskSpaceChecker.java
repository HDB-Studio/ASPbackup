package com.aspbackup.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Disk space monitoring utilities.
 */
public class DiskSpaceChecker {

    /**
     * Get the available free space in bytes for a path.
     */
    public static long getFreeSpace(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            return store.getUsableSpace();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Get the total space in bytes for a path.
     */
    public static long getTotalSpace(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            return store.getTotalSpace();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Check if sufficient space is available.
     */
    public static boolean isSufficient(long requiredBytes, long availableBytes) {
        return availableBytes >= requiredBytes;
    }

    /**
     * Get the free space percentage.
     */
    public static double getFreeSpacePercent(Path path) {
        long total = getTotalSpace(path);
        if (total == 0 || total == Long.MAX_VALUE) return 100.0;
        return (getFreeSpace(path) * 100.0) / total;
    }

    /**
     * Format bytes to human-readable string.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}