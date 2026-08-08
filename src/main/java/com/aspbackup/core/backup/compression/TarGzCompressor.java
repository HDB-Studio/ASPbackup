package com.aspbackup.core.backup.compression;

import com.aspbackup.core.backup.source.FileEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Compresses backup files into a .tar.gz archive using Apache Commons Compress.
 * Supports streaming mode, making it suitable for resume operations.
 */
public class TarGzCompressor implements Compressor {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public CompressionResult compress(List<FileEntry> files, OutputStream output,
                                       Path baseDir, int level) throws IOException {
        long originalBytes = 0;

        // Set compression level
        GzipParameters params = new GzipParameters();
        params.setCompressionLevel(level);

        // Wrap output with CountingOutputStream to track compressed bytes
        CountingOutputStream countingOutput = new CountingOutputStream(output);

        try (GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(
                new BufferedOutputStream(countingOutput), params);
             TarArchiveOutputStream tos = new TarArchiveOutputStream(gzos)) {

            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            byte[] buffer = new byte[BUFFER_SIZE];
            for (FileEntry entry : files) {
                Path file = entry.getAbsolutePath();
                String tarEntryName = entry.getRelativePath().toString().replace('\\', '/');

                TarArchiveEntry tarEntry = new TarArchiveEntry(file.toFile(), tarEntryName);
                tarEntry.setModTime(entry.getLastModified());
                tos.putArchiveEntry(tarEntry);

                try (InputStream fis = Files.newInputStream(file)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        tos.write(buffer, 0, len);
                    }
                }

                tos.closeArchiveEntry();
                originalBytes += entry.getSize();
            }
            tos.finish();
        }

        return new CompressionResult(countingOutput.getByteCount(), originalBytes);
    }

    @Override
    public String getExtension() {
        return ".tar.gz";
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }
}