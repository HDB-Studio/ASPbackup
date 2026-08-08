package com.aspbackup.core.backup.compression;

import com.aspbackup.core.backup.source.FileEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for backup compression strategies.
 */
public interface Compressor {

    /**
     * Compress a list of files into a single output stream.
     *
     * @param files      the files to compress
     * @param output     the output stream to write compressed data to
     * @param baseDir    the base directory for relative paths
     * @param level      compression level (1-9)
     * @return the result containing total bytes written
     */
    CompressionResult compress(List<FileEntry> files, OutputStream output,
                                Path baseDir, int level) throws IOException;

    /**
     * Get the file extension for this compression format.
     */
    String getExtension();

    /**
     * Whether this compressor supports streaming mode (useful for resume).
     */
    boolean supportsStreaming();

    /**
     * Result of a compression operation.
     */
    record CompressionResult(long compressedBytes, long originalBytes) {
        public double getCompressionRatio() {
            if (originalBytes == 0) return 0;
            return 1.0 - ((double) compressedBytes / originalBytes);
        }
    }
}