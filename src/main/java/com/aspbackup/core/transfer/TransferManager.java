package com.aspbackup.core.transfer;

import com.aspbackup.ASPBackup;
import com.aspbackup.core.transfer.connection.ConnectionPool;
import com.aspbackup.core.transfer.loadbalance.LeastLoadedBalancer;
import com.aspbackup.core.transfer.loadbalance.LoadBalancer;
import com.aspbackup.core.transfer.loadbalance.RoundRobinBalancer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages distributed transfer of backup data to multiple remote nodes.
 * Coordinates connection pooling, load balancing, and chunk distribution.
 */
public class TransferManager {

    private final ASPBackup plugin;
    private final Map<String, TransferNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, TransferSession> activeSessions = new ConcurrentHashMap<>();
    private final ConnectionPool connectionPool;
    private final LoadBalancer loadBalancer;

    public TransferManager(ASPBackup plugin) {
        this.plugin = plugin;
        this.connectionPool = new ConnectionPool(5, 60000, plugin.getLogger());

        // Select load balancer based on config
        String strategy = plugin.getConfigManager().getTransferConfig().getLoadBalanceStrategy();
        this.loadBalancer = "least_loaded".equalsIgnoreCase(strategy)
                ? new LeastLoadedBalancer()
                : new RoundRobinBalancer();

        // Initialize nodes from config
        initializeNodes();
    }

    /**
     * Initialize nodes from configuration.
     */
    private void initializeNodes() {
        for (var nodeConfig : plugin.getConfigManager().getNodeConfigs()) {
            if (nodeConfig.isEnabled()) {
                TransferNode node = new TransferNode(nodeConfig);
                nodes.put(node.getId(), node);
                plugin.getLogger().info("Transfer node registered: " + node.getId() + " (" + node.getHost() + ":" + node.getPort() + ")");
            }
        }
        plugin.getLogger().info("Transfer manager initialized with " + nodes.size() + " node(s), strategy: " + loadBalancer.getName());
    }

    /**
     * Register a new transfer node.
     */
    public void registerNode(TransferNode node) {
        nodes.put(node.getId(), node);
        plugin.getLogger().info("Transfer node added: " + node.getId());
    }

    /**
     * Remove a transfer node.
     */
    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        plugin.getLogger().info("Transfer node removed: " + nodeId);
    }

    /**
     * Get the status of a transfer node.
     */
    public String getNodeStatus(String nodeId) {
        TransferNode node = nodes.get(nodeId);
        if (node == null) return "NOT_FOUND";
        return node.isOnline() ? "ONLINE" : "OFFLINE";
    }

    /**
     * Get all registered nodes.
     */
    public Collection<TransferNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Get the load balancer.
     */
    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    /**
     * Get the connection pool.
     */
    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    /**
     * Get active transfer sessions.
     */
    public Map<String, TransferSession> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }

    /**
     * Shutdown the transfer manager, closing all connections.
     */
    public void shutdown() {
        activeSessions.clear();
        connectionPool.shutdown();
        nodes.clear();
        plugin.getLogger().info("TransferManager shut down.");
    }
}