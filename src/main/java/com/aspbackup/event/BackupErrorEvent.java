package com.aspbackup.event;

import com.aspbackup.core.backup.BackupTask;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a backup operation encounters an error.
 */
public class BackupErrorEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final BackupTask task;
    private final String stage;
    private final String errorMessage;

    public BackupErrorEvent(BackupTask task, String stage, String errorMessage) {
        this.task = task;
        this.stage = stage;
        this.errorMessage = errorMessage;
    }

    public BackupTask getTask() { return task; }
    public String getStage() { return stage; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}