package com.aspbackup.core.config.model;

/**
 * Configuration model for a single transfer node.
 */
public class NodeConfig {

    private String id;
    private String host;
    private int port = 9876;
    private String authToken;
    private boolean enabled = true;
    private int weight = 1;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
}