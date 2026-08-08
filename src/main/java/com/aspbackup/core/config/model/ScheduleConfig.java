package com.aspbackup.core.config.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model for backup scheduling.
 */
public class ScheduleConfig {

    private boolean enabled = false;
    private int intervalMinutes = 360;
    private String backupType = "full";
    private String targetId = "local";
    private List<String> quietHours = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }

    public String getBackupType() { return backupType; }
    public void setBackupType(String backupType) { this.backupType = backupType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public List<String> getQuietHours() { return quietHours; }
    public void setQuietHours(List<String> quietHours) { this.quietHours = quietHours; }
}