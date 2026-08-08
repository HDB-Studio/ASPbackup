package com.aspbackup.core.transfer.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handshake packet sent from plugin to receiver to authenticate and negotiate.
 * Packet ID: 0x01
 */
public class HandshakePacket extends Packet {

    private int protocolVersion = 1;
    private String nodeId;
    private String authToken;
    private int capabilities; // bit flags

    public HandshakePacket() {
        super(0x01);
    }

    public HandshakePacket(String nodeId, String authToken, int capabilities) {
        super(0x01);
        this.nodeId = nodeId;
        this.authToken = authToken;
        this.capabilities = capabilities;
    }

    @Override
    public void encode(DataOutputStream out) throws IOException {
        out.writeShort(protocolVersion);
        writeString(out, authToken);
        writeString(out, nodeId);
        out.writeByte(capabilities);
    }

    @Override
    public void decode(DataInputStream in) throws IOException {
        protocolVersion = in.readUnsignedShort();
        authToken = readString(in);
        nodeId = readString(in);
        capabilities = in.readUnsignedByte();
    }

    public int getProtocolVersion() { return protocolVersion; }
    public String getNodeId() { return nodeId; }
    public String getAuthToken() { return authToken; }
    public int getCapabilities() { return capabilities; }

    private void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeByte(bytes.length);
        out.write(bytes);
    }

    private String readString(DataInputStream in) throws IOException {
        int len = in.readUnsignedByte();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}