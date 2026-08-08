package com.aspbackup.receiver.assembly;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Receives and buffers chunks, then assembles them into a complete file.
 */
public class ChunkReceiver {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-Receiver");

    private final Path taskDir;

    public ChunkReceiver(Path taskDir) {
        this.taskDir = taskDir;
    }

    /**
     * Receive a chunk and write it to a temporary file.
     */
    public void receiveChunk(long chunkIndex, byte[] data) throws IOException {
        Files.createDirectories(taskDir);
        String chunkName = String.format("chunk_%08d.part", chunkIndex);
        Path chunkFile = taskDir.resolve(chunkName);
        Files.write(chunkFile, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Get the list of all chunk files, sorted by index.
     */
    public List<Path> getChunkFiles() throws IOException {
        List<Path> chunks = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskDir, "chunk_*.part")) {
            for (Path file : stream) {
                chunks.add(file);
            }
        }
        chunks.sort(Comparator.comparing(Path::getFileName));
        return chunks;
    }

    /**
     * Check if all chunks for a given total have been received.
     */
    public boolean isComplete(long totalChunks) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskDir, "chunk_*.part")) {
            long count = 0;
            for (var ignored : stream) count++;
            return count >= totalChunks;
        }
    }

    /**
     * Clean up chunk files after assembly.
     */
    public void cleanup() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskDir, "chunk_*.part")) {
            for (Path file : stream) {
                Files.delete(file);
            }
        }
    }
}