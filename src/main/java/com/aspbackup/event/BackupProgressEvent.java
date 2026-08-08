package com.aspbackup.event;

import com.aspbackup.core.backup.BackupTask;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called periodically during a backup operation to report progress.
 */
public class BackupProgressEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final BackupTask task;
    private final double percent;
    private final long filesDone;
    private final long bytesDone;

    public BackupProgressEvent(BackupTask task, double percent, long filesDone, long bytesDone) {
        this.task = task;
        this.percent = percent;
        this.filesDone = filesDone;
        this.bytesDone = bytesDone;
    }

    public BackupTask getTask() { return task; }
    public double getPercent() { return percent; }
    public long getFilesDone() { return filesDone; }
    public long getBytesDone() { return bytesDone; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}