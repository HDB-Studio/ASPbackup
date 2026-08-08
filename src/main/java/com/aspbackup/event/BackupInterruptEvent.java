package com.aspbackup.event;

import com.aspbackup.core.backup.BackupTask;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a backup operation is interrupted/paused.
 */
public class BackupInterruptEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final BackupTask task;
    private final String reason;
    private final String checkpointId;

    public BackupInterruptEvent(BackupTask task, String reason, String checkpointId) {
        this.task = task;
        this.reason = reason;
        this.checkpointId = checkpointId;
    }

    public BackupTask getTask() { return task; }
    public String getReason() { return reason; }
    public String getCheckpointId() { return checkpointId; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}