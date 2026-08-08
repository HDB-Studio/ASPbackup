package com.aspbackup.core.transfer.protocol;

import java.io.*;

/**
 * 传输协议封包基类。
 * 使用 4 字节长度前缀格式，与 Netty 的 LengthFieldBasedFrameDecoder 兼容。
 */
public abstract class Packet {

    private final int packetId;

    protected Packet(int packetId) {
        this.packetId = packetId;
    }

    public int getPacketId() { return packetId; }

    /**
     * Encode this packet's payload to a DataOutputStream.
     */
    public abstract void encode(DataOutputStream out) throws IOException;

    /**
     * Decode this packet's payload from a DataInputStream.
     */
    public abstract void decode(DataInputStream in) throws IOException;

    /**
     * Write a complete packet with 4-byte length prefix.
     */
    public void write(DataOutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream tempOut = new DataOutputStream(baos);
        encode(tempOut);
        tempOut.flush();
        byte[] payload = baos.toByteArray();

        out.writeInt(payload.length);
        out.write(payload);
    }
}