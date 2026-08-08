package com.aspbackup.core.backup.compression;

import com.aspbackup.core.backup.source.FileEntry;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compresses backup files into a ZIP archive.
 */
public class ZipCompressor implements Compressor {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public CompressionResult compress(List<FileEntry> files, OutputStream output,
                                       Path baseDir, int level) throws IOException {
        long originalBytes = 0;

        // Wrap output with CountingOutputStream to track compressed bytes
        CountingOutputStream countingOutput = new CountingOutputStream(output);

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(countingOutput))) {
            zos.setLevel(level);

            byte[] buffer = new byte[BUFFER_SIZE];
            for (FileEntry entry : files) {
                Path file = entry.getAbsolutePath();
                String zipEntryName = entry.getRelativePath().toString().replace('\\', '/');

                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                zipEntry.setTime(entry.getLastModified());
                zos.putNextEntry(zipEntry);

                try (InputStream fis = Files.newInputStream(file)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }

                zos.closeEntry();
                originalBytes += entry.getSize();
            }
            zos.finish();
        }

        return new CompressionResult(countingOutput.getByteCount(), originalBytes);
    }

    @Override
    public String getExtension() {
        return ".zip";
    }

    @Override
    public boolean supportsStreaming() {
        return false;
    }
}