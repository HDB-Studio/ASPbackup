package com.aspbackup.core.backup.source;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a source of files to be backed up.
 */
public interface BackupSource {

    /**
     * Collect all files matching the source's criteria.
     *
     * @return list of file entries to back up
     */
    List<FileEntry> collectFiles();

    /**
     * Get the total size of all files in bytes.
     */
    long getTotalSize();

    /**
     * Get a human-readable name for this source.
     */
    String getName();

    /**
     * Get the root path of this source.
     */
    Path getRootPath();
}