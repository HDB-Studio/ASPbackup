package com.aspbackup.receiver.assembly;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * 将排序后的分块文件组装为单一输出档案。
 */
public class FileAssembler {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-接收端");

    /**
     * 将分块文件列表组装为单一输出档案。
     *
     * @param chunks     排序后的分块文件列表
     * @param outputFile 目标输出档案
     * @return 写入的总字节数
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

        LOGGER.info("已组装 " + chunks.size() + " 个分块 → " + outputFile.getFileName() +
                "（" + formatBytes(totalBytes) + "）");
        return totalBytes;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}