package com.aspbackup.core.transfer.loadbalance;

import com.aspbackup.core.transfer.TransferNode;

import java.util.List;

/**
 * Interface for load balancing strategies across transfer nodes.
 */
public interface LoadBalancer {

    /**
     * Select the best node from the available list.
     *
     * @param nodes list of available nodes
     * @return the selected node, or null if none available
     */
    TransferNode selectNode(List<TransferNode> nodes);

    /**
     * Called when a chunk is successfully sent to a node.
     */
    void onChunkSent(TransferNode node, long bytes);

    /**
     * Called when a chunk transfer to a node fails.
     */
    void onChunkFailed(TransferNode node);

    /**
     * Get the strategy name.
     */
    String getName();
}