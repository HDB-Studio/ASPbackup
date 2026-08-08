package com.aspbackup.receiver.assembly;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Assembles sorted chunk files into a single output file.
 */
public class FileAssembler {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-Receiver");

    /**
     * Assemble a list of chunk files into a single output file.
     *
     * @param chunks    the sorted list of chunk files
     * @param outputFile the target output file
     * @return the total number of bytes written
     */
    public long assemble(List<Path> chunks, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        long totalBytes = 0;

        try (OutputStream out = Files.newOutputStream(outputFile,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Path chunk : chunks) {
                byte[] data = Files.readAllBytes(chunk);
                out.write(data);
                totalBytes += data.length;
            }
        }

        LOGGER.info("Assembled " + chunks.size() + " chunks into " + outputFile +
                " (" + totalBytes + " bytes)");
        return totalBytes;
    }
}