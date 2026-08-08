package com.aspbackup.core.backup.source;

import java.nio.file.Path;

/**
 * Represents a single file entry in a backup source.
 */
public class FileEntry {

    private final Path relativePath;
    private final Path absolutePath;
    private final long size;
    private final long lastModified;

    public FileEntry(Path relativePath, Path absolutePath, long size, long lastModified) {
        this.relativePath = relativePath;
        this.absolutePath = absolutePath;
        this.size = size;
        this.lastModified = lastModified;
    }

    public Path getRelativePath() { return relativePath; }
    public Path getAbsolutePath() { return absolutePath; }
    public long getSize() { return size; }
    public long getLastModified() { return lastModified; }

    @Override
    public String toString() {
        return relativePath.toString() + " (" + size + " bytes)";
    }
}