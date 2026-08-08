package com.aspbackup.core.transfer.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Chunk data packet sent from plugin to receiver.
 * Packet ID: 0x03
 */
public class ChunkPacket extends Packet {

    private String taskId;
    private long chunkIndex;
    private long totalChunks;
    private long offset;
    private int dataLength;
    private String checksum;
    private byte[] data;

    public ChunkPacket() {
        super(0x03);
    }

    public ChunkPacket(String taskId, long chunkIndex, long totalChunks,
                       long offset, int dataLength, String checksum, byte[] data) {
        super(0x03);
        this.taskId = taskId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.offset = offset;
        this.dataLength = dataLength;
        this.checksum = checksum;
        this.data = data;
    }

    @Override
    public void encode(DataOutputStream out) throws IOException {
        writeString(out, taskId);
        out.writeLong(chunkIndex);
        out.writeLong(totalChunks);
        out.writeLong(offset);
        out.writeInt(dataLength);
        writeString(out, checksum);
        out.write(data);
    }

    @Override
    public void decode(DataInputStream in) throws IOException {
        taskId = readString(in);
        chunkIndex = in.readLong();
        totalChunks = in.readLong();
        offset = in.readLong();
        dataLength = in.readInt();
        checksum = readString(in);
        data = new byte[dataLength];
        in.readFully(data);
    }

    public String getTaskId() { return taskId; }
    public long getChunkIndex() { return chunkIndex; }
    public long getTotalChunks() { return totalChunks; }
    public long getOffset() { return offset; }
    public int getDataLength() { return dataLength; }
    public String getChecksum() { return checksum; }
    public byte[] getData() { return data; }

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