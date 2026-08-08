package com.aspbackup.core.backup.source;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Walks a directory tree and collects files matching include/exclude patterns.
 */
public class FileCollector {

    private final List<String> includePatterns;
    private final List<String> excludePatterns;
    private final long minSizeBytes;
    private final long maxSizeBytes;
    private final int maxDepth;

    public FileCollector(List<String> includePatterns, List<String> excludePatterns,
                         long minSizeBytes, long maxSizeBytes, int maxDepth) {
        this.includePatterns = includePatterns != null ? includePatterns : List.of("**/*");
        this.excludePatterns = excludePatterns != null ? excludePatterns : List.of();
        this.minSizeBytes = minSizeBytes;
        this.maxSizeBytes = maxSizeBytes;
        this.maxDepth = maxDepth > 0 ? maxDepth : Integer.MAX_VALUE;
    }

    /**
     * Collect files from the given root directory.
     *
     * @param root       the root directory to scan
     * @param extraExcludes additional exclusion patterns specific to this source
     * @return list of matching file entries
     */
    public List<FileEntry> collect(Path root, List<String> extraExcludes) throws IOException {
        List<FileEntry> files = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            private int currentDepth = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (currentDepth >= maxDepth) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                currentDepth++;

                Path relative = root.relativize(dir);
                String dirPath = relative.toString().replace('\\', '/');

                // Check if this directory should be excluded
                if (shouldExclude(dirPath, extraExcludes)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                currentDepth--;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relative = root.relativize(file);
                String filePath = relative.toString().replace('\\', '/');

                // Check exclude patterns
                if (shouldExclude(filePath, extraExcludes)) {
                    return FileVisitResult.CONTINUE;
                }

                // Check include patterns
                if (!shouldInclude(filePath)) {
                    return FileVisitResult.CONTINUE;
                }

                // Check size constraints
                long size = attrs.size();
                if (minSizeBytes > 0 && size < minSizeBytes) {
                    return FileVisitResult.CONTINUE;
                }
                if (maxSizeBytes > 0 && size > maxSizeBytes) {
                    return FileVisitResult.CONTINUE;
                }

                files.add(new FileEntry(relative, file, size, attrs.lastModifiedTime().toMillis()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Skip files that can't be read (permissions, etc.)
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    private boolean shouldExclude(String filePath, List<String> extraExcludes) {
        // Check global exclude patterns
        for (String pattern : excludePatterns) {
            if (globMatch(pattern, filePath)) {
                return true;
            }
        }
        // Check extra source-specific excludes
        if (extraExcludes != null) {
            for (String pattern : extraExcludes) {
                if (globMatch(pattern, filePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldInclude(String filePath) {
        for (String pattern : includePatterns) {
            if (globMatch(pattern, filePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simple glob matching supporting ** and * wildcards.
     */
    static boolean globMatch(String pattern, String path) {
        // Convert glob pattern to regex
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", "<<<DOUBLESTAR>>>")
                .replace("*", "[^/]*")
                .replace("<<<DOUBLESTAR>>>", ".*")
                .replace("?", ".");
        try {
            return path.matches(regex);
        } catch (PatternSyntaxException e) {
            // If regex conversion fails, fall back to simple contains
            return path.contains(pattern.replace("**", "").replace("*", ""));
        }
    }
}