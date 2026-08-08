package com.aspbackup.core.transfer.connection;

import com.aspbackup.core.transfer.TransferNode;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Manages a pool of reusable connections to transfer nodes.
 */
public class ConnectionPool {

    private final Map<String, Deque<NodeConnection>> pool = new ConcurrentHashMap<>();
    private final int maxPerNode;
    private final long idleTimeoutMs;
    private final Logger logger;

    public ConnectionPool(int maxPerNode, long idleTimeoutMs, Logger logger) {
        this.maxPerNode = maxPerNode;
        this.idleTimeoutMs = idleTimeoutMs;
        this.logger = logger;
    }

    /**
     * Get or create a connection to a node.
     */
    public NodeConnection getConnection(TransferNode node, int connectTimeoutMs) throws IOException {
        Deque<NodeConnection> connections = pool.computeIfAbsent(node.getId(), k -> new ArrayDeque<>());

        // Try to reuse an existing connection
        NodeConnection conn;
        while ((conn = connections.poll()) != null) {
            if (conn.isConnected()) {
                conn.touch();
                return conn;
            }
            closeQuietly(conn);
        }

        // Create new connection
        conn = new NodeConnection(node);
        conn.connect(connectTimeoutMs);
        conn.handshake();
        return conn;
    }

    /**
     * Return a connection to the pool for reuse.
     */
    public void releaseConnection(NodeConnection conn) {
        if (conn == null || !conn.isConnected()) return;

        Deque<NodeConnection> connections = pool.get(conn.getNode().getId());
        if (connections != null && connections.size() < maxPerNode) {
            conn.touch();
            connections.offer(conn);
        } else {
            closeQuietly(conn);
        }
    }

    /**
     * Evict idle connections that haven't been used recently.
     */
    public void evictIdleConnections() {
        long now = System.currentTimeMillis();
        for (var entry : pool.entrySet()) {
            Deque<NodeConnection> connections = entry.getValue();
            connections.removeIf(conn -> {
                if (now - conn.getLastUsed() > idleTimeoutMs) {
                    closeQuietly(conn);
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Close all connections in the pool.
     */
    public void shutdown() {
        for (var entry : pool.entrySet()) {
            for (NodeConnection conn : entry.getValue()) {
                closeQuietly(conn);
            }
        }
        pool.clear();
        logger.info("Connection pool shut down.");
    }

    /**
     * Get the number of active connections.
     */
    public int getActiveConnectionCount() {
        return pool.values().stream().mapToInt(Deque::size).sum();
    }

    private void closeQuietly(NodeConnection conn) {
        try { conn.close(); } catch (Exception ignored) {}
    }
}