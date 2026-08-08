package com.aspbackup.core.backup.target;

import com.aspbackup.core.transfer.TransferManager;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Writes backup data to a remote cluster via the distributed transfer system.
 */
public class RemoteBackupTarget implements BackupTarget {

    private final String id;
    private final TransferManager transferManager;
    private final int retentionCount;
    private final Logger logger;

    public RemoteBackupTarget(String id, TransferManager transferManager, int retentionCount, Logger logger) {
        this.id = id;
        this.transferManager = transferManager;
        this.retentionCount = retentionCount;
        this.logger = logger;
    }

    @Override
    public long write(InputStream data, String relativePath) throws IOException {
        // Write to a temp file first, then send via transfer manager
        Path tempFile = Files.createTempFile("aspbackup-remote-", ".tmp");
        try {
            long bytesCopied = Files.copy(data, tempFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Staged " + bytesCopied + " bytes for remote transfer: " + relativePath);
            // Phase 6 will implement actual transfer via TransferManager.sendToNodes()
            return bytesCopied;
        } finally {
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
        }
    }

    @Override
    public boolean exists(String relativePath) {
        return false; // Remote verification handled separately
    }

    @Override
    public long getFreeSpace() {
        return Long.MAX_VALUE; // Remote storage is managed by receiver
    }

    @Override
    public String getType() { return "REMOTE"; }

    @Override
    public String getId() { return id; }

    @Override
    public void enforceRetention() throws IOException {
        // Remote retention is managed by the receiver application
        logger.fine("Remote retention enforcement delegated to receiver");
    }
}