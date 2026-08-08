package com.aspbackup.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * File system utility methods.
 */
public final class FileUtil {

    private FileUtil() {}

    /**
     * Calculate the total size of a directory recursively.
     */
    public static long directorySize(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            return Files.size(path);
        }
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0; }
                    })
                    .sum();
        }
    }

    /**
     * Delete a directory recursively.
     */
    public static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        }
    }

    /**
     * Ensure a directory exists, creating it if needed.
     */
    public static Path ensureDirectory(Path path) throws IOException {
        return Files.createDirectories(path);
    }

    /**
     * Get a safe filename by replacing invalid characters.
     */
    public static String safeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Count files in a directory recursively.
     */
    public static long countFiles(Path path) throws IOException {
        if (!Files.isDirectory(path)) return 1;
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile).count();
        }
    }
}