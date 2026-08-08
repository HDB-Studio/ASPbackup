package com.aspbackup.core.transfer.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Base class for all network packets in the ASPbackup transfer protocol.
 */
public abstract class Packet {

    public static final int MAGIC_HIGH = 0x41; // 'A'
    public static final int MAGIC_LOW  = 0x42; // 'B'

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
     * Read a complete packet from an input stream.
     */
    public static Packet readPacket(DataInputStream in) throws IOException {
        int magicHigh = in.readUnsignedByte();
        int magicLow = in.readUnsignedByte();
        if (magicHigh != MAGIC_HIGH || magicLow != MAGIC_LOW) {
            throw new IOException("Invalid magic bytes: " + magicHigh + " " + magicLow);
        }

        int packetId = in.readUnsignedByte();
        int payloadLen = in.readInt();

        Packet packet = switch (packetId) {
            case 0x01 -> new HandshakePacket();
            case 0x03 -> new ChunkPacket();
            case 0x04 -> new AckPacket();
            case 0x07 -> new ResumeRequestPacket();
            case 0x09 -> new StatusPacket();
            default -> throw new IOException("Unknown packet ID: 0x" + Integer.toHexString(packetId));
        };

        packet.decode(in);
        return packet;
    }

    /**
     * Write a complete packet to an output stream.
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeByte(MAGIC_HIGH);
        out.writeByte(MAGIC_LOW);
        out.writeByte(packetId);
        // We don't know the payload length ahead of time for all packets,
        // so subclasses handle this differently
        encode(out);
    }
}