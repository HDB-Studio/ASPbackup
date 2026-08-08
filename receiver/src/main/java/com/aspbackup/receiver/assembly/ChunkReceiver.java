package com.aspbackup.receiver.assembly;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * 接收并缓冲分块数据，供后续组装使用。
 */
public class ChunkReceiver {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-接收端");

    private final Path taskDir;

    public ChunkReceiver(Path taskDir) {
        this.taskDir = taskDir;
    }

    /**
     * 接收一个分块并写入暂存档。
     */
    public void receiveChunk(long chunkIndex, byte[] data) throws IOException {
        Files.createDirectories(taskDir);
        String chunkName = String.format("chunk_%08d.part", chunkIndex);
        Path chunkFile = taskDir.resolve(chunkName);
        Files.write(chunkFile, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 获取所有分块文件列表，按索引排序。
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
     * 检查是否已接收所有分块。
     */
    public boolean isComplete(long totalChunks) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskDir, "chunk_*.part")) {
            long count = 0;
            for (var ignored : stream) count++;
            return count >= totalChunks;
        }
    }

    /**
     * 组装完成后清理所有分块暂存档。
     */
    public void cleanup() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskDir, "chunk_*.part")) {
            for (Path file : stream) {
                Files.delete(file);
            }
        }
        LOGGER.fine("分块暂存档已清理：" + taskDir);
    }
}