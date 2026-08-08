package com.aspbackup.core.transfer.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Status report packet.
 * Packet ID: 0x09
 */
public class StatusPacket extends Packet {

    private String taskId;
    private double progress;
    private long bytesTransferred;
    private long speedBytesPerSec;
    private String status;

    public StatusPacket() {
        super(0x09);
    }

    public StatusPacket(String taskId, double progress, long bytesTransferred,
                         long speedBytesPerSec, String status) {
        super(0x09);
        this.taskId = taskId;
        this.progress = progress;
        this.bytesTransferred = bytesTransferred;
        this.speedBytesPerSec = speedBytesPerSec;
        this.status = status;
    }

    @Override
    public void encode(DataOutputStream out) throws IOException {
        writeString(out, taskId);
        out.writeDouble(progress);
        out.writeLong(bytesTransferred);
        out.writeLong(speedBytesPerSec);
        writeString(out, status);
    }

    @Override
    public void decode(DataInputStream in) throws IOException {
        taskId = readString(in);
        progress = in.readDouble();
        bytesTransferred = in.readLong();
        speedBytesPerSec = in.readLong();
        status = readString(in);
    }

    public String getTaskId() { return taskId; }
    public double getProgress() { return progress; }
    public long getBytesTransferred() { return bytesTransferred; }
    public long getSpeedBytesPerSec() { return speedBytesPerSec; }
    public String getStatus() { return status; }

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