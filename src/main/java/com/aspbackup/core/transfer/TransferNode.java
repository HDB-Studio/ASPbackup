package com.aspbackup.core.transfer;

import com.aspbackup.core.config.model.NodeConfig;

/**
 * Represents a transfer node with its connection state.
 */
public class TransferNode {

    private final String id;
    private final String host;
    private final int port;
    private final String authToken;
    private final int weight;
    private boolean enabled;
    private volatile boolean online;
    private volatile long bytesTransferred;
    private volatile long lastSeen;

    public TransferNode(NodeConfig config) {
        this.id = config.getId();
        this.host = config.getHost();
        this.port = config.getPort();
        this.authToken = config.getAuthToken();
        this.weight = config.getWeight();
        this.enabled = config.isEnabled();
        this.online = false;
        this.bytesTransferred = 0;
        this.lastSeen = 0;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getAuthToken() { return authToken; }
    public int getWeight() { return weight; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public long getBytesTransferred() { return bytesTransferred; }
    public void addBytesTransferred(long bytes) { this.bytesTransferred += bytes; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    /**
     * Get the current load factor (higher = more loaded).
     */
    public double getLoadFactor() {
        if (weight <= 0) return Double.MAX_VALUE;
        return (double) bytesTransferred / weight;
    }

    @Override
    public String toString() {
        return String.format("TransferNode[id=%s, %s:%d, online=%s, transferred=%d]",
                id, host, port, online, bytesTransferred);
    }
}