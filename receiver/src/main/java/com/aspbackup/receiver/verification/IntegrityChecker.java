package com.aspbackup.receiver.verification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * 使用 SHA-256 校验接收到的备份档案完整性。
 */
public class IntegrityChecker {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-接收端");
    private static final int BUFFER_SIZE = 8192;

    /**
     * 校验文件的 SHA-256 校验和是否与预期值匹配。
     *
     * @param file             要校验的文件
     * @param expectedChecksum 预期的 SHA-256 十六进制字符串
     * @return 校验和是否匹配
     */
    public boolean verify(Path file, String expectedChecksum) throws IOException {
        String actual = sha256(file);
        boolean match = actual.equalsIgnoreCase(expectedChecksum);
        if (match) {
            LOGGER.info("完整性校验通过：" + file.getFileName());
        } else {
            LOGGER.severe("完整性校验失败：" + file.getFileName() +
                    "（预期=" + expectedChecksum + "，实际=" + actual + "）");
        }
        return match;
    }

    /**
     * 计算文件的 SHA-256 十六进制摘要。
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
            throw new IOException("SHA-256 不可用", e);
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