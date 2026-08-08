package com.aspbackup.core.config.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model for distributed transfer settings.
 */
public class TransferConfig {

    private int chunkSizeKb = 1024;
    private int parallelThreads = 4;
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 30000;
    private int retryCount = 3;
    private int retryDelayMs = 5000;
    private String loadBalanceStrategy = "least_loaded";
    private List<NodeConfig> nodes = new ArrayList<>();

    public int getChunkSizeKb() { return chunkSizeKb; }
    public void setChunkSizeKb(int chunkSizeKb) { this.chunkSizeKb = chunkSizeKb; }

    public int getParallelThreads() { return parallelThreads; }
    public void setParallelThreads(int parallelThreads) { this.parallelThreads = parallelThreads; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(int retryDelayMs) { this.retryDelayMs = retryDelayMs; }

    public String getLoadBalanceStrategy() { return loadBalanceStrategy; }
    public void setLoadBalanceStrategy(String loadBalanceStrategy) { this.loadBalanceStrategy = loadBalanceStrategy; }

    public List<NodeConfig> getNodes() { return nodes; }
    public void setNodes(List<NodeConfig> nodes) { this.nodes = nodes; }
}