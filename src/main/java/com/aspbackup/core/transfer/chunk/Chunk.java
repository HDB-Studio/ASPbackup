package com.aspbackup.core.transfer.chunk;

/**
 * Represents a chunk of a file during distributed transfer.
 */
public class Chunk {

    private final long chunkId;
    private final long offset;
    private final int length;
    private final String checksum;
    private final byte[] data;
    private final String taskId;
    private final int fileIndex;

    public Chunk(long chunkId, long offset, int length, String checksum,
                  byte[] data, String taskId, int fileIndex) {
        this.chunkId = chunkId;
        this.offset = offset;
        this.length = length;
        this.checksum = checksum;
        this.data = data;
        this.taskId = taskId;
        this.fileIndex = fileIndex;
    }

    public long getChunkId() { return chunkId; }
    public long getOffset() { return offset; }
    public int getLength() { return length; }
    public String getChecksum() { return checksum; }
    public byte[] getData() { return data; }
    public String getTaskId() { return taskId; }
    public int getFileIndex() { return fileIndex; }

    @Override
    public String toString() {
        return String.format("Chunk[id=%d, offset=%d, len=%d, task=%s]",
                chunkId, offset, length, taskId);
    }
}