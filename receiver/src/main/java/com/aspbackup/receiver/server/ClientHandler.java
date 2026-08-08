package com.aspbackup.receiver.server;

import com.aspbackup.receiver.assembly.ChunkReceiver;
import com.aspbackup.receiver.assembly.FileAssembler;
import com.aspbackup.receiver.verification.IntegrityChecker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * 处理来自 ASPbackup 插件的客户端连线。
 * 负责握手认证、分块接收、档案组装与完整性校验。
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-接收端");

    private final Path outputDir;
    private final String expectedToken;
    private boolean authenticated = false;
    private String currentTaskId;
    private Path currentTaskDir;
    private long receivedChunks = 0;
    private long totalChunks = 0;

    public ClientHandler(Path outputDir, String expectedToken) {
        this.outputDir = outputDir;
        this.expectedToken = expectedToken;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("新连线来自：" + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            if (!authenticated) {
                handleHandshake(ctx, buf);
            } else {
                handleChunkData(ctx, buf);
            }
        } catch (Exception e) {
            LOGGER.severe("处理封包时出错：" + e.getMessage());
            sendError(ctx, e.getMessage());
        } finally {
            buf.release();
        }
    }

    /**
     * 处理握手认证。
     */
    private void handleHandshake(ChannelHandlerContext ctx, ByteBuf buf) {
        int protocolVersion = buf.readUnsignedShort();
        int authTokenLen = buf.readUnsignedByte();
        byte[] tokenBytes = new byte[authTokenLen];
        buf.readBytes(tokenBytes);
        String token = new String(tokenBytes, StandardCharsets.UTF_8);

        int nodeIdLen = buf.readUnsignedByte();
        byte[] nodeIdBytes = new byte[nodeIdLen];
        buf.readBytes(nodeIdBytes);
        String nodeId = new String(nodeIdBytes, StandardCharsets.UTF_8);

        int capabilities = buf.readUnsignedByte();

        // 验证 token（"change-me" 为预设值，允许跳过验证）
        if (!expectedToken.equals(token) && !"change-me".equals(expectedToken)) {
            LOGGER.warning("节点认证失败：" + nodeId);
            sendError(ctx, "认证失败");
            ctx.close();
            return;
        }

        authenticated = true;
        LOGGER.info("节点已认证：" + nodeId + "（协议版本 v" + protocolVersion +
                "，能力：0x" + Integer.toHexString(capabilities) + "）");

        // 发送 ACK
        ByteBuf ack = ctx.alloc().buffer(4);
        ack.writeInt(0); // 0 = 成功
        ctx.writeAndFlush(ack);
    }

    /**
     * 处理分块数据接收。
     */
    private void handleChunkData(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        // 读取任务 ID
        int taskIdLen = buf.readUnsignedShort();
        byte[] taskIdBytes = new byte[taskIdLen];
        buf.readBytes(taskIdBytes);
        String taskId = new String(taskIdBytes, StandardCharsets.UTF_8);

        // 收到第一个分块时初始化任务目录
        if (!taskId.equals(currentTaskId)) {
            currentTaskId = taskId;
            currentTaskDir = outputDir.resolve(taskId);
            receivedChunks = 0;
            totalChunks = 0;
            Files.createDirectories(currentTaskDir);
            LOGGER.info("开始接收备份任务：" + taskId + "，输出目录：" + currentTaskDir);
        }

        long chunkIndex = buf.readLong();
        totalChunks = buf.readLong();
        long offset = buf.readLong();
        int dataLength = buf.readInt();

        // 读取校验和
        int checksumLen = buf.readUnsignedShort();
        byte[] checksumBytes = new byte[checksumLen];
        buf.readBytes(checksumBytes);
        String checksum = new String(checksumBytes, StandardCharsets.UTF_8);

        // 读取分块数据
        byte[] data = new byte[dataLength];
        buf.readBytes(data);

        // 写入分块到磁盘
        ChunkReceiver chunkReceiver = new ChunkReceiver(currentTaskDir);
        chunkReceiver.receiveChunk(chunkIndex, data);
        receivedChunks++;

        // 进度汇报（每 10 个分块或最后一个分块时输出）
        if (chunkIndex % 10 == 0 || chunkIndex + 1 >= totalChunks) {
            double progress = (receivedChunks * 100.0) / totalChunks;
            LOGGER.info(String.format("任务 %s 接收进度：%d/%d（%.1f%%），本块大小：%d 字节",
                    taskId, receivedChunks, totalChunks, progress, dataLength));
        }

        // 发送 ACK
        ByteBuf ack = ctx.alloc().buffer(19);
        ack.writeShort(taskIdBytes.length);
        ack.writeBytes(taskIdBytes);
        ack.writeLong(chunkIndex);
        ack.writeBoolean(true);
        ack.writeShort(0); // 无错误消息
        ctx.writeAndFlush(ack);

        // 检查是否所有分块已接收完毕
        if (chunkIndex + 1 >= totalChunks) {
            onAllChunksReceived(ctx, taskId, totalChunks, checksum);
        }
    }

    /**
     * 所有分块接收完毕后，组装档案并进行完整性校验。
     */
    private void onAllChunksReceived(ChannelHandlerContext ctx, String taskId, long totalChunks, String fileChecksum)
            throws Exception {
        LOGGER.info("任务 " + taskId + " 所有分块已接收完毕（共 " + totalChunks + " 个分块），开始组装档案...");

        // 使用 ChunkReceiver 获取所有分块
        ChunkReceiver chunkReceiver = new ChunkReceiver(currentTaskDir);
        var chunks = chunkReceiver.getChunkFiles();

        if (chunks.size() < totalChunks) {
            LOGGER.warning("任务 " + taskId + " 分块不完整：预期 " + totalChunks + "，实际 " + chunks.size());
            sendError(ctx, "分块不完整");
            return;
        }

        // 组装档案
        Path assembledFile = currentTaskDir.resolve("backup.tar.gz");
        FileAssembler assembler = new FileAssembler();
        long totalBytes = assembler.assemble(chunks, assembledFile);
        LOGGER.info("任务 " + taskId + " 档案组装完成：" + assembledFile + "（" + formatBytes(totalBytes) + "）");

        // 完整性校验
        if (fileChecksum != null && !fileChecksum.isEmpty() && !"待验证".equals(fileChecksum)) {
            IntegrityChecker checker = new IntegrityChecker();
            boolean verified = checker.verify(assembledFile, fileChecksum);
            if (verified) {
                LOGGER.info("任务 " + taskId + " 完整性校验通过！");
            } else {
                LOGGER.severe("任务 " + taskId + " 完整性校验失败！");
                sendError(ctx, "完整性校验失败");
                return;
            }
        }

        // 清理分块文件
        chunkReceiver.cleanup();
        LOGGER.info("任务 " + taskId + " 分块暂存档已清理。");

        // 发送完成通知
        ByteBuf complete = ctx.alloc().buffer(4 + taskIdBytes().length);
        complete.writeShort(taskIdBytes().length);
        complete.writeBytes(taskIdBytes());
        complete.writeBoolean(true); // 成功
        complete.writeShort(0);
        ctx.writeAndFlush(complete);

        LOGGER.info("============================================");
        LOGGER.info("  任务 " + taskId + " 接收完成！");
        LOGGER.info("  档案大小：" + formatBytes(totalBytes));
        LOGGER.info("  输出路径：" + assembledFile.toAbsolutePath());
        LOGGER.info("============================================");
    }

    private byte[] taskIdBytes() {
        return currentTaskId != null ? currentTaskId.getBytes(StandardCharsets.UTF_8) : new byte[0];
    }

    private void sendError(ChannelHandlerContext ctx, String message) {
        ByteBuf error = ctx.alloc().buffer(2 + message.getBytes(StandardCharsets.UTF_8).length);
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        error.writeShort(msgBytes.length);
        error.writeBytes(msgBytes);
        ctx.writeAndFlush(error);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("连线已关闭：" + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warning("连线异常：" + cause.getMessage());
        ctx.close();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}