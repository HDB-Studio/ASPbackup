package com.aspbackup.core.transfer.loadbalance;

import com.aspbackup.core.transfer.TransferNode;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cycles through nodes in round-robin order.
 */
public class RoundRobinBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public TransferNode selectNode(List<TransferNode> nodes) {
        List<TransferNode> available = nodes.stream()
                .filter(n -> n.isEnabled() && n.isOnline())
                .toList();

        if (available.isEmpty()) return null;

        int index = Math.abs(counter.getAndIncrement() % available.size());
        return available.get(index);
    }

    @Override
    public void onChunkSent(TransferNode node, long bytes) {
        node.addBytesTransferred(bytes);
    }

    @Override
    public void onChunkFailed(TransferNode node) {
        // Round-robin doesn't track failures; least-loaded will
    }

    @Override
    public String getName() {
        return "round_robin";
    }
}