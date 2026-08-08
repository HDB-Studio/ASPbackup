package com.aspbackup.receiver.verification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Verifies the integrity of received backup files using SHA-256.
 */
public class IntegrityChecker {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-Receiver");
    private static final int BUFFER_SIZE = 8192;

    /**
     * Verify a file's SHA-256 checksum against an expected value.
     *
     * @param file             the file to verify
     * @param expectedChecksum the expected SHA-256 hex string
     * @return true if checksums match
     */
    public boolean verify(Path file, String expectedChecksum) throws IOException {
        String actual = sha256(file);
        boolean match = actual.equalsIgnoreCase(expectedChecksum);
        if (match) {
            LOGGER.info("Integrity check PASSED: " + file.getFileName());
        } else {
            LOGGER.severe("Integrity check FAILED: " + file.getFileName() +
                    " (expected=" + expectedChecksum + ", actual=" + actual + ")");
        }
        return match;
    }

    /**
     * Compute the SHA-256 hex digest of a file.
     */
    public String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream in = Files.newInputStream(file)) {
                int len;
                while ((len = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, len);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}