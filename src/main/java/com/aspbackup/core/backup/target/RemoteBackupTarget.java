package com.aspbackup.core.backup.target;

import com.aspbackup.core.transfer.TransferNode;
import com.aspbackup.core.transfer.connection.ConnectionPool;
import com.aspbackup.core.transfer.connection.NodeConnection;
import com.aspbackup.core.transfer.protocol.HandshakePacket;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 将备份数据通过 TCP 传输到远端接收端。
 * 实现分块传输、ACK 确认、完整性校验。
 */
public class RemoteBackupTarget implements BackupTarget {

    private static final int PROTOCOL_VERSION = 1;
    private static final int CAPABILITIES = 0x07;

    private final String id;
    private final String taskId;
    private final TransferNode node;
    private final ConnectionPool connectionPool;
    private final int chunkSizeBytes;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int retryCount;
    private final Logger logger;

    public RemoteBackupTarget(String id, String taskId, TransferNode node,
                               ConnectionPool connectionPool,
                               int chunkSizeKb, int connectTimeoutMs,
                               int readTimeoutMs, int retryCount, Logger logger) {
        this.id = id;
        this.taskId = taskId;
        this.node = node;
        this.connectionPool = connectionPool;
        this.chunkSizeBytes = chunkSizeKb * 1024;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.retryCount = retryCount;
        this.logger = logger;
    }

    @Override
    public long write(InputStream data, String relativePath) throws IOException {
        logger.info("开始远端传输：任务=" + taskId + "，节点=" + node.getHost() + ":" + node.getPort());

        // 读取全部数据
        byte[] fileData = data.readAllBytes();
        long totalBytes = fileData.length;
        long totalChunks = (totalBytes + chunkSizeBytes - 1) / chunkSizeBytes;
        logger.info(String.format("远端传输：档案大小=%s，分块数=%d，每块=%dKB",
                formatBytes(totalBytes), totalChunks, chunkSizeBytes / 1024));

        // 计算整体 SHA-256
        String fileChecksum = computeSha256(fileData);
        logger.info("档案 SHA-256：" + fileChecksum);

        NodeConnection conn = null;
        int attempt = 0;
        IOException lastError = null;

        while (attempt <= retryCount) {
            try {
                conn = connectionPool.getConnection(node, connectTimeoutMs);
                logger.info("已连线至节点：" + node.getHost() + ":" + node.getPort());

                // 发送所有分块
                for (long chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                    long offset = chunkIndex * chunkSizeBytes;
                    int length = (int) Math.min(chunkSizeBytes, totalBytes - offset);

                    byte[] chunkData = new byte[length];
                    System.arraycopy(fileData, (int) offset, chunkData, 0, length);
                    String chunkChecksum = computeSha256(chunkData);

                    sendChunk(conn, chunkIndex, totalChunks, offset, chunkData, chunkChecksum);

                    // 读取 ACK
                    boolean ackOk = readChunkAck(conn, chunkIndex);
                    if (!ackOk) {
                        throw new IOException("分块 " + chunkIndex + " 的 ACK 失败");
                    }

                    // 进度汇报
                    if (chunkIndex % 10 == 0 || chunkIndex + 1 >= totalChunks) {
                        double progress = ((chunkIndex + 1) * 100.0) / totalChunks;
                        logger.info(String.format("远端传输进度：%d/%d（%.1f%%）",
                                chunkIndex + 1, totalChunks, progress));
                    }
                }

                // 等待接收端组装完成通知
                readCompletionAck(conn);

                node.addBytesTransferred(totalBytes);
                node.setLastSeen(System.currentTimeMillis());
                node.setOnline(true);

                logger.info("远端传输完成：" + taskId + "，共 " + formatBytes(totalBytes));
                return totalBytes;

            } catch (IOException e) {
                lastError = e;
                attempt++;
                logger.warning("远端传输失败（尝试 " + attempt + "/" + (retryCount + 1) + "）：" + e.getMessage());
                if (attempt <= retryCount) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            } finally {
                if (conn != null) {
                    connectionPool.releaseConnection(conn);
                }
            }
        }

        throw new IOException("远端传输失败，已重试 " + retryCount + " 次", lastError);
    }

    private void sendChunk(NodeConnection conn, long chunkIndex, long totalChunks,
                           long offset, byte[] data, String checksum) throws IOException {
        DataOutputStream out = conn.getOutputStream();

        byte[] taskIdBytes = taskId.getBytes(StandardCharsets.UTF_8);
        byte[] checksumBytes = checksum.getBytes(StandardCharsets.UTF_8);

        // 任务 ID 长度 + 任务 ID
        out.writeShort(taskIdBytes.length);
        out.write(taskIdBytes);
        // 分块索引
        out.writeLong(chunkIndex);
        // 总分块数
        out.writeLong(totalChunks);
        // 偏移量
        out.writeLong(offset);
        // 数据长度
        out.writeInt(data.length);
        // 校验和长度 + 校验和
        out.writeShort(checksumBytes.length);
        out.write(checksumBytes);
        // 实际数据
        out.write(data);

        out.flush();
    }

    private boolean readChunkAck(NodeConnection conn, long expectedChunkIndex) throws IOException {
        DataInputStream in = conn.getInputStream();

        int taskIdLen = in.readUnsignedShort();
        byte[] taskIdBytes = new byte[taskIdLen];
        in.readFully(taskIdBytes);

        long ackChunkIndex = in.readLong();
        boolean success = in.readBoolean();
        int errorMsgLen = in.readUnsignedShort();

        if (!success) {
            byte[] errBytes = new byte[errorMsgLen];
            in.readFully(errBytes);
            String errorMsg = new String(errBytes, StandardCharsets.UTF_8);
            logger.warning("分块 " + ackChunkIndex + " 错误：" + errorMsg);
            return false;
        }

        return ackChunkIndex == expectedChunkIndex;
    }

    private void readCompletionAck(NodeConnection conn) throws IOException {
        DataInputStream in = conn.getInputStream();

        int taskIdLen = in.readUnsignedShort();
        byte[] taskIdBytes = new byte[taskIdLen];
        in.readFully(taskIdBytes);

        boolean success = in.readBoolean();
        int msgLen = in.readUnsignedShort();

        if (success) {
            logger.info("接收端已确认组装完成：" + taskId);
        } else {
            byte[] errBytes = new byte[msgLen];
            in.readFully(errBytes);
            String errorMsg = new String(errBytes, StandardCharsets.UTF_8);
            throw new IOException("接收端组装失败：" + errorMsg);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        return false; // 远端目标不检查本地文件
    }

    @Override
    public long getFreeSpace() {
        return Long.MAX_VALUE; // 远端目标不检查本地空间
    }

    @Override
    public String getType() {
        return "REMOTE";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void enforceRetention() {
        // 远端目标的保留策略由接收端自行管理
    }

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}