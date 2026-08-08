package com.aspbackup.core.transfer;

import com.aspbackup.core.transfer.chunk.Chunk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an active distributed transfer session.
 */
public class TransferSession {

    private final String taskId;
    private final List<TransferNode> nodes;
    private final Map<TransferNode, List<Chunk>> assignments;
    private final AtomicLong chunksSent = new AtomicLong(0);
    private final AtomicLong chunksAcked = new AtomicLong(0);
    private final long totalChunks;
    private final long startTime;

    public TransferSession(String taskId, List<TransferNode> nodes,
                            Map<TransferNode, List<Chunk>> assignments) {
        this.taskId = taskId;
        this.nodes = nodes;
        this.assignments = new ConcurrentHashMap<>(assignments);
        this.totalChunks = assignments.values().stream().mapToLong(List::size).sum();
        this.startTime = System.currentTimeMillis();
    }

    public String getTaskId() { return taskId; }
    public List<TransferNode> getNodes() { return nodes; }
    public Map<TransferNode, List<Chunk>> getAssignments() { return assignments; }

    public void onChunkSent() { chunksSent.incrementAndGet(); }
    public void onChunkAcked() { chunksAcked.incrementAndGet(); }

    public long getChunksSent() { return chunksSent.get(); }
    public long getChunksAcked() { return chunksAcked.get(); }
    public long getTotalChunks() { return totalChunks; }

    public double getProgress() {
        if (totalChunks == 0) return 0.0;
        return (chunksAcked.get() * 100.0) / totalChunks;
    }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTime;
    }

    public long getSpeedBytesPerSec(long bytesPerChunk) {
        long elapsed = getElapsedMs();
        if (elapsed == 0) return 0;
        return (chunksAcked.get() * bytesPerChunk * 1000) / elapsed;
    }

    public boolean isComplete() {
        return chunksAcked.get() >= totalChunks;
    }
}