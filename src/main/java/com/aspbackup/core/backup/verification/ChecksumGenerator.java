package com.aspbackup.core.backup.verification;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generates SHA-256 checksums for files and backup manifests.
 */
public class ChecksumGenerator {

    private static final int BUFFER_SIZE = 8192;
    private final Logger logger;

    public ChecksumGenerator(Logger logger) {
        this.logger = logger;
    }

    /**
     * Generate a SHA-256 hex digest for a file.
     */
    public String generateSha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream fis = Files.newInputStream(file)) {
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    digest.update(buffer, 0, len);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }

    /**
     * Generate a SHA-256 hex digest from an input stream.
     */
    public String generateSha256(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = input.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }

    /**
     * Generate a manifest mapping relative paths to checksums.
     */
    public Map<String, String> generateManifest(List<com.aspbackup.core.backup.source.FileEntry> files) {
        Map<String, String> manifest = new LinkedHashMap<>();
        for (var entry : files) {
            try {
                String hash = generateSha256(entry.getAbsolutePath());
                manifest.put(entry.getRelativePath().toString().replace('\\', '/'), hash);
            } catch (IOException e) {
                logger.warning("Failed to generate checksum for: " + entry.getRelativePath());
                manifest.put(entry.getRelativePath().toString().replace('\\', '/'), "ERROR");
            }
        }
        return manifest;
    }

    /**
     * Persist a manifest to a JSON-like file.
     */
    public void persistManifest(Map<String, String> manifest, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            for (var entry : manifest.entrySet()) {
                writer.write(entry.getValue() + "  " + entry.getKey());
                writer.newLine();
            }
        }
    }

    /**
     * Load a manifest from a file.
     */
    public Map<String, String> loadManifest(Path manifestFile) throws IOException {
        Map<String, String> manifest = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(manifestFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Format: <sha256>  <path>
                int spaceIdx = line.indexOf("  ");
                if (spaceIdx > 0) {
                    String hash = line.substring(0, spaceIdx);
                    String path = line.substring(spaceIdx + 2);
                    manifest.put(path, hash);
                }
            }
        }
        return manifest;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}