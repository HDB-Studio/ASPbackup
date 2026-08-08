package com.aspbackup.core.backup.target;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Writes backup data to a local filesystem path.
 */
public class LocalBackupTarget implements BackupTarget {

    private final String id;
    private final Path basePath;
    private final int retentionCount;
    private final Logger logger;

    public LocalBackupTarget(String id, Path basePath, int retentionCount, Logger logger) {
        this.id = id;
        this.basePath = basePath;
        this.retentionCount = retentionCount;
        this.logger = logger;
    }

    @Override
    public long write(InputStream data, String relativePath) throws IOException {
        Path targetFile = basePath.resolve(relativePath);
        Files.createDirectories(targetFile.getParent());

        long bytesCopied = Files.copy(data, targetFile, StandardCopyOption.REPLACE_EXISTING);
        logger.fine("Written " + bytesCopied + " bytes to " + targetFile);
        return bytesCopied;
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(basePath.resolve(relativePath));
    }

    @Override
    public long getFreeSpace() {
        try {
            FileStore store = Files.getFileStore(basePath);
            return store.getUsableSpace();
        } catch (IOException e) {
            logger.warning("Failed to get free space for " + basePath + ": " + e.getMessage());
            return Long.MAX_VALUE; // Assume OK if we can't check
        }
    }

    @Override
    public String getType() {
        return "LOCAL";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void enforceRetention() throws IOException {
        if (retentionCount <= 0) return;

        try (Stream<Path> files = Files.list(basePath)) {
            var backupFiles = files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparingLong(this::getLastModified).reversed())
                    .toList();

            if (backupFiles.size() > retentionCount) {
                for (int i = retentionCount; i < backupFiles.size(); i++) {
                    Path file = backupFiles.get(i);
                    Files.delete(file);
                    logger.info("Deleted old backup: " + file.getFileName());
                }
            }
        }
    }

    private long getLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "LocalBackupTarget[id=" + id + ", path=" + basePath + "]";
    }
}