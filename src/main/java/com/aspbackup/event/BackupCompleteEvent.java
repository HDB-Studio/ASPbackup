package com.aspbackup.event;

import com.aspbackup.core.backup.BackupTask;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a backup operation completes successfully.
 */
public class BackupCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final BackupTask task;

    public BackupCompleteEvent(BackupTask task) {
        this.task = task;
    }

    public BackupTask getTask() { return task; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}