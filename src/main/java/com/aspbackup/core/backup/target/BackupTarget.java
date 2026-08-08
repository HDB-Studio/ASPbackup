package com.aspbackup.core.backup.target;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a destination for backup data.
 */
public interface BackupTarget {

    /**
     * Write data to the target at the given relative path.
     *
     * @param data the input stream to write
     * @param relativePath the relative path within the target
     * @return the number of bytes written
     */
    long write(InputStream data, String relativePath) throws IOException;

    /**
     * Check if a file exists at the given path.
     */
    boolean exists(String relativePath);

    /**
     * Get the available free space in bytes.
     */
    long getFreeSpace();

    /**
     * Get the target type identifier.
     */
    String getType();

    /**
     * Get the target's unique identifier.
     */
    String getId();

    /**
     * Delete old backups to enforce retention count.
     */
    void enforceRetention() throws IOException;
}