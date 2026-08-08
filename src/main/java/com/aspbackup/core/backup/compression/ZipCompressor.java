package com.aspbackup.core.backup.compression;

import com.aspbackup.core.backup.source.FileEntry;

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
        long compressedBytes;

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(output))) {
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

        // Estimate compressed size (we can't easily get it from ZipOutputStream)
        compressedBytes = originalBytes; // Placeholder; actual size is in the temp file

        return new CompressionResult(compressedBytes, originalBytes);
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