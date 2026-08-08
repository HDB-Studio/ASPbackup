package com.aspbackup.core.backup.target;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Writes backup data to a NAS (Network Attached Storage) path.
 * Includes retry logic for network interruptions.
 */
public class NASBackupTarget implements BackupTarget {

    private final String id;
    private final Path basePath;
    private final int retentionCount;
    private final int maxRetries;
    private final Logger logger;

    public NASBackupTarget(String id, Path basePath, int retentionCount, int maxRetries, Logger logger) {
        this.id = id;
        this.basePath = basePath;
        this.retentionCount = retentionCount;
        this.maxRetries = maxRetries;
        this.logger = logger;
    }

    @Override
    public long write(InputStream data, String relativePath) throws IOException {
        Path targetFile = basePath.resolve(relativePath);

        IOException lastError = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Files.createDirectories(targetFile.getParent());
                long bytesCopied = Files.copy(data, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.fine("NAS write successful: " + targetFile + " (" + bytesCopied + " bytes)");
                return bytesCopied;
            } catch (IOException e) {
                lastError = e;
                logger.warning("NAS write attempt " + attempt + "/" + maxRetries + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try { Thread.sleep(2000L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new IOException("NAS write failed after " + maxRetries + " attempts", lastError);
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
            return Long.MAX_VALUE;
        }
    }

    @Override
    public String getType() { return "NAS"; }

    @Override
    public String getId() { return id; }

    @Override
    public void enforceRetention() throws IOException {
        if (retentionCount <= 0) return;
        try (Stream<Path> files = Files.list(basePath)) {
            var backupFiles = files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.<Path>comparingLong(p -> {
                        try { return Files.getLastModifiedTime(p).toMillis(); } catch (IOException e) { return 0; }
                    }).reversed())
                    .toList();
            if (backupFiles.size() > retentionCount) {
                for (int i = retentionCount; i < backupFiles.size(); i++) {
                    Files.delete(backupFiles.get(i));
                    logger.info("Deleted old NAS backup: " + backupFiles.get(i).getFileName());
                }
            }
        }
    }
}