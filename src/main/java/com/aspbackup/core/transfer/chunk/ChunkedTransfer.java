package com.aspbackup.core.transfer.chunk;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits files into chunks for distributed parallel transfer.
 */
public class ChunkedTransfer {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Split a file into fixed-size chunks.
     *
     * @param file        the file to split
     * @param chunkSizeKB chunk size in kilobytes
     * @param taskId      the backup task ID
     * @param fileIndex   the index of this file in the backup
     * @return list of chunks
     */
    public static List<Chunk> splitFile(Path file, int chunkSizeKB,
                                         String taskId, int fileIndex) throws IOException {
        List<Chunk> chunks = new ArrayList<>();
        long fileSize = Files.size(file);
        int chunkSize = chunkSizeKB * 1024;
        long numChunks = (fileSize + chunkSize - 1) / chunkSize;

        try (InputStream fis = Files.newInputStream(file)) {
            byte[] buffer = new byte[chunkSize];

            for (long i = 0; i < numChunks; i++) {
                long offset = i * chunkSize;
                int bytesToRead = (int) Math.min(chunkSize, fileSize - offset);
                byte[] data = new byte[bytesToRead];

                int totalRead = 0;
                while (totalRead < bytesToRead) {
                    int read = fis.read(data, totalRead, bytesToRead - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }

                String checksum = sha256(data);
                Chunk chunk = new Chunk(i, offset, totalRead, checksum, data, taskId, fileIndex);
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    /**
     * Assemble chunks back into a complete file on the receiver side.
     */
    public static void assemble(List<Chunk> chunks, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        chunks.sort((a, b) -> Long.compare(a.getChunkId(), b.getChunkId()));

        try (OutputStream fos = Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Chunk chunk : chunks) {
                fos.write(chunk.getData());
            }
        }
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "error";
        }
    }
}