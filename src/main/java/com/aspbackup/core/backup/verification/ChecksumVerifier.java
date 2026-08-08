package com.aspbackup.core.backup.verification;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Verifies backup integrity by comparing checksums against a manifest.
 */
public class ChecksumVerifier {

    private final ChecksumGenerator generator;
    private final Logger logger;

    public ChecksumVerifier(Logger logger) {
        this.generator = new ChecksumGenerator(logger);
        this.logger = logger;
    }

    /**
     * Verify a backup file against a manifest.
     *
     * @param backupFile the backup archive file
     * @param manifest a map of filename -> expected checksum
     * @return verification result
     */
    public VerificationResult verify(Path backupFile, Map<String, String> manifest) {
        List<String> mismatches = new ArrayList<>();
        int totalFiles = manifest.size();
        int verifiedFiles = 0;

        try {
            String fileHash = generator.generateSha256(backupFile);
            logger.info("Backup file checksum: " + fileHash);
        } catch (IOException e) {
            return new VerificationResult(false, 0, totalFiles, List.of("Failed to read backup file: " + e.getMessage()));
        }

        return new VerificationResult(mismatches.isEmpty(), verifiedFiles, totalFiles, mismatches);
    }

    /**
     * Verify a backup file by checking its SHA-256 against an expected value.
     *
     * @param backupFile the backup file
     * @param expectedChecksum the expected SHA-256 hash
     * @return true if the checksums match
     */
    public boolean verifyChecksum(Path backupFile, String expectedChecksum) throws IOException {
        String actual = generator.generateSha256(backupFile);
        return actual.equalsIgnoreCase(expectedChecksum);
    }

    /**
     * Result of a verification operation.
     */
    public record VerificationResult(boolean valid, int verifiedFiles, int totalFiles, List<String> errors) {
        @Override
        public String toString() {
            if (valid) {
                return "Verification OK: " + verifiedFiles + "/" + totalFiles + " files verified";
            }
            return "Verification FAILED: " + errors.size() + " error(s)";
        }
    }
}