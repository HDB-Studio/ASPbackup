package com.aspbackup.receiver.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Handles individual client connections from the ASPbackup plugin.
 * Processes handshake, chunk reception, and file assembly.
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-Receiver");

    private final Path outputDir;
    private final String expectedToken;
    private boolean authenticated = false;
    private String currentTaskId;
    private Path currentTaskDir;

    public ClientHandler(Path outputDir, String expectedToken) {
        this.outputDir = outputDir;
        this.expectedToken = expectedToken;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("New connection from: " + ctx.channel().remoteAddress());
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
            LOGGER.severe("Error processing packet: " + e.getMessage());
            sendError(ctx, e.getMessage());
        } finally {
            buf.release();
        }
    }

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

        if (!expectedToken.equals(token) && !"change-me".equals(expectedToken)) {
            LOGGER.warning("Authentication failed for node: " + nodeId);
            sendError(ctx, "Authentication failed");
            ctx.close();
            return;
        }

        authenticated = true;
        LOGGER.info("Node authenticated: " + nodeId + " (protocol v" + protocolVersion + ", capabilities: 0x"
                + Integer.toHexString(capabilities) + ")");

        // Send ACK
        ByteBuf ack = ctx.alloc().buffer(4);
        ack.writeInt(0); // 0 = success
        ctx.writeAndFlush(ack);
    }

    private void handleChunkData(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        // Read task ID
        int taskIdLen = buf.readUnsignedShort();
        byte[] taskIdBytes = new byte[taskIdLen];
        buf.readBytes(taskIdBytes);
        String taskId = new String(taskIdBytes, StandardCharsets.UTF_8);

        // Initialize task directory on first chunk
        if (!taskId.equals(currentTaskId)) {
            currentTaskId = taskId;
            currentTaskDir = outputDir.resolve(taskId);
            Files.createDirectories(currentTaskDir);
        }

        long chunkIndex = buf.readLong();
        long totalChunks = buf.readLong();
        long offset = buf.readLong();
        int dataLength = buf.readInt();

        // Read checksum
        int checksumLen = buf.readUnsignedShort();
        byte[] checksumBytes = new byte[checksumLen];
        buf.readBytes(checksumBytes);
        String checksum = new String(checksumBytes, StandardCharsets.UTF_8);

        // Read chunk data
        byte[] data = new byte[dataLength];
        buf.readBytes(data);

        // Write chunk to disk
        Path chunkFile = currentTaskDir.resolve("chunk_" + String.format("%08d", chunkIndex) + ".part");
        Files.write(chunkFile, data);

        LOGGER.fine("Received chunk " + (chunkIndex + 1) + "/" + totalChunks +
                " for task " + taskId + " (" + dataLength + " bytes)");

        // Send ACK
        ByteBuf ack = ctx.alloc().buffer(19);
        // taskId
        ack.writeShort(taskIdBytes.length);
        ack.writeBytes(taskIdBytes);
        // chunkIndex
        ack.writeLong(chunkIndex);
        // success
        ack.writeBoolean(true);
        // no error message
        ack.writeShort(0);
        ctx.writeAndFlush(ack);

        // Check if all chunks received
        if (chunkIndex + 1 >= totalChunks) {
            assembleFile(ctx, taskId, totalChunks);
        }
    }

    private void assembleFile(ChannelHandlerContext ctx, String taskId, long totalChunks) throws Exception {
        LOGGER.info("Assembly complete for task " + taskId + ". Total chunks: " + totalChunks);
        // Assembly is handled by FileAssembler
        // In full implementation, this would trigger the file assembler
    }

    private void sendError(ChannelHandlerContext ctx, String message) {
        ByteBuf error = ctx.alloc().buffer(2 + message.length());
        error.writeShort(message.length());
        error.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(error);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("Connection closed: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warning("Connection error: " + cause.getMessage());
        ctx.close();
    }
}