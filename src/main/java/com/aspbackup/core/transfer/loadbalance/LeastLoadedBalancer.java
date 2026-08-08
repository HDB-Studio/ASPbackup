package com.aspbackup.core.transfer.loadbalance;

import com.aspbackup.core.transfer.TransferNode;

import java.util.Comparator;
import java.util.List;

/**
 * Selects the node with the lowest current load (bytes transferred / weight).
 */
public class LeastLoadedBalancer implements LoadBalancer {

    @Override
    public TransferNode selectNode(List<TransferNode> nodes) {
        return nodes.stream()
                .filter(n -> n.isEnabled() && n.isOnline())
                .min(Comparator.comparingDouble(TransferNode::getLoadFactor))
                .orElse(null);
    }

    @Override
    public void onChunkSent(TransferNode node, long bytes) {
        node.addBytesTransferred(bytes);
    }

    @Override
    public void onChunkFailed(TransferNode node) {
        // Penalize the node by adding virtual load
        node.addBytesTransferred(10 * 1024 * 1024); // 10MB penalty
    }

    @Override
    public String getName() {
        return "least_loaded";
    }
}