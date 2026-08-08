package com.aspbackup.core.transfer.connection;

import com.aspbackup.core.transfer.TransferNode;
import com.aspbackup.core.transfer.protocol.HandshakePacket;

import java.io.*;
import java.net.Socket;

/**
 * Represents a TCP connection to a transfer node (receiver).
 */
public class NodeConnection implements Closeable {

    private final TransferNode node;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private long lastUsed;

    public NodeConnection(TransferNode node) {
        this.node = node;
    }

    /**
     * Connect to the remote node.
     */
    public void connect(int timeoutMs) throws IOException {
        socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(node.getHost(), node.getPort()), timeoutMs);
        socket.setSoTimeout(30000);
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        lastUsed = System.currentTimeMillis();
    }

    /**
     * Perform handshake with the receiver.
     */
    public boolean handshake() throws IOException {
        HandshakePacket packet = new HandshakePacket(
                node.getId(), node.getAuthToken(), 0x07); // all capabilities
        packet.write(out);
        out.flush();

        // 读取长度前缀（4 字节），然后读取 ACK（4 字节 int，0 = 成功）
        int frameLen = in.readInt();
        int ack = in.readInt();
        if (ack != 0) {
            throw new IOException("握手失败，接收端返回错误码：" + ack);
        }
        return true;
    }

    public DataOutputStream getOutputStream() { return out; }
    public DataInputStream getInputStream() { return in; }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public long getLastUsed() { return lastUsed; }
    public void touch() { lastUsed = System.currentTimeMillis(); }

    public TransferNode getNode() { return node; }

    @Override
    public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        out = null;
        in = null;
    }
}