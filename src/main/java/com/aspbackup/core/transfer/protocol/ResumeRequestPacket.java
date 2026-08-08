package com.aspbackup.core.transfer.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Request to resume transfer from a specific chunk.
 * Packet ID: 0x07
 */
public class ResumeRequestPacket extends Packet {

    private String taskId;
    private long lastReceivedChunk;

    public ResumeRequestPacket() {
        super(0x07);
    }

    public ResumeRequestPacket(String taskId, long lastReceivedChunk) {
        super(0x07);
        this.taskId = taskId;
        this.lastReceivedChunk = lastReceivedChunk;
    }

    @Override
    public void encode(DataOutputStream out) throws IOException {
        writeString(out, taskId);
        out.writeLong(lastReceivedChunk);
    }

    @Override
    public void decode(DataInputStream in) throws IOException {
        taskId = readString(in);
        lastReceivedChunk = in.readLong();
    }

    public String getTaskId() { return taskId; }
    public long getLastReceivedChunk() { return lastReceivedChunk; }

    private void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private String readString(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}