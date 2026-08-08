package com.aspbackup.core.transfer.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Acknowledgment packet sent from receiver to plugin.
 * Packet ID: 0x04
 */
public class AckPacket extends Packet {

    private String taskId;
    private long chunkIndex;
    private boolean success;
    private String errorMessage;

    public AckPacket() {
        super(0x04);
    }

    public AckPacket(String taskId, long chunkIndex, boolean success, String errorMessage) {
        super(0x04);
        this.taskId = taskId;
        this.chunkIndex = chunkIndex;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    @Override
    public void encode(DataOutputStream out) throws IOException {
        writeString(out, taskId);
        out.writeLong(chunkIndex);
        out.writeBoolean(success);
        writeString(out, errorMessage != null ? errorMessage : "");
    }

    @Override
    public void decode(DataInputStream in) throws IOException {
        taskId = readString(in);
        chunkIndex = in.readLong();
        success = in.readBoolean();
        errorMessage = readString(in);
    }

    public String getTaskId() { return taskId; }
    public long getChunkIndex() { return chunkIndex; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

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