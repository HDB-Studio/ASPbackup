package com.aspbackup.model;

/**
 * Represents information about a transfer node.
 */
public class NodeInfo {

    private final String id;
    private final String host;
    private final int port;
    private final boolean enabled;
    private final boolean online;
    private final long bytesTransferred;
    private final long lastSeen;

    public NodeInfo(String id, String host, int port, boolean enabled,
                     boolean online, long bytesTransferred, long lastSeen) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
        this.online = online;
        this.bytesTransferred = bytesTransferred;
        this.lastSeen = lastSeen;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isEnabled() { return enabled; }
    public boolean isOnline() { return online; }
    public long getBytesTransferred() { return bytesTransferred; }
    public long getLastSeen() { return lastSeen; }
}