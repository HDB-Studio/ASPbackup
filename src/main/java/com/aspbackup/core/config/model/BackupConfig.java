package com.aspbackup.core.config.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration model for backup settings.
 */
public class BackupConfig {

    private boolean autoOnStart = true;
    private boolean autoOnShutdown = true;
    private int startDelaySeconds = 30;
    private int blockingTimeoutSeconds = 300;
    private String tempDirectory = "plugins/ASPbackup/temp";
    private long maxBackupSizeMb = 0;
    private boolean verifyAfterBackup = true;
    private String compressionFormat = "targz";
    private int compressionLevel = 6;
    private List<SourceDef> sources = new ArrayList<>();
    private List<TargetDef> targets = new ArrayList<>();
    private FileFilterDef fileFilter = new FileFilterDef();

    // Getters and setters
    public boolean isAutoOnStart() { return autoOnStart; }
    public void setAutoOnStart(boolean autoOnStart) { this.autoOnStart = autoOnStart; }

    public boolean isAutoOnShutdown() { return autoOnShutdown; }
    public void setAutoOnShutdown(boolean autoOnShutdown) { this.autoOnShutdown = autoOnShutdown; }

    public int getStartDelaySeconds() { return startDelaySeconds; }
    public void setStartDelaySeconds(int startDelaySeconds) { this.startDelaySeconds = startDelaySeconds; }

    public int getBlockingTimeoutSeconds() { return blockingTimeoutSeconds; }
    public void setBlockingTimeoutSeconds(int blockingTimeoutSeconds) { this.blockingTimeoutSeconds = blockingTimeoutSeconds; }

    public String getTempDirectory() { return tempDirectory; }
    public void setTempDirectory(String tempDirectory) { this.tempDirectory = tempDirectory; }

    public long getMaxBackupSizeMb() { return maxBackupSizeMb; }
    public void setMaxBackupSizeMb(long maxBackupSizeMb) { this.maxBackupSizeMb = maxBackupSizeMb; }

    public boolean isVerifyAfterBackup() { return verifyAfterBackup; }
    public void setVerifyAfterBackup(boolean verifyAfterBackup) { this.verifyAfterBackup = verifyAfterBackup; }

    public String getCompressionFormat() { return compressionFormat; }
    public void setCompressionFormat(String compressionFormat) { this.compressionFormat = compressionFormat; }

    public int getCompressionLevel() { return compressionLevel; }
    public void setCompressionLevel(int compressionLevel) { this.compressionLevel = compressionLevel; }

    public List<SourceDef> getSources() { return sources; }
    public void setSources(List<SourceDef> sources) { this.sources = sources; }

    public List<TargetDef> getTargets() { return targets; }
    public void setTargets(List<TargetDef> targets) { this.targets = targets; }

    public FileFilterDef getFileFilter() { return fileFilter; }
    public void setFileFilter(FileFilterDef fileFilter) { this.fileFilter = fileFilter; }

    /**
     * Backup source definition.
     */
    public static class SourceDef {
        private String path;
        private String name;
        private List<String> include = new ArrayList<>();
        private List<String> exclude = new ArrayList<>();
        private int maxDepth = 100;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getInclude() { return include; }
        public void setInclude(List<String> include) { this.include = include; }
        public List<String> getExclude() { return exclude; }
        public void setExclude(List<String> exclude) { this.exclude = exclude; }
        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    }

    /**
     * Backup target definition.
     */
    public static class TargetDef {
        private String id;
        private String type = "LOCAL";
        private String path = "backups/";
        private int retentionCount = 10;
        private long minFreeSpaceMb = 1024;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public int getRetentionCount() { return retentionCount; }
        public void setRetentionCount(int retentionCount) { this.retentionCount = retentionCount; }
        public long getMinFreeSpaceMb() { return minFreeSpaceMb; }
        public void setMinFreeSpaceMb(long minFreeSpaceMb) { this.minFreeSpaceMb = minFreeSpaceMb; }
    }

    /**
     * File filter definition.
     */
    public static class FileFilterDef {
        private List<String> include = new ArrayList<>();
        private List<String> exclude = new ArrayList<>();
        private long minSizeBytes = 0;
        private long maxSizeBytes = 0;

        public List<String> getInclude() { return include; }
        public void setInclude(List<String> include) { this.include = include; }
        public List<String> getExclude() { return exclude; }
        public void setExclude(List<String> exclude) { this.exclude = exclude; }
        public long getMinSizeBytes() { return minSizeBytes; }
        public void setMinSizeBytes(long minSizeBytes) { this.minSizeBytes = minSizeBytes; }
        public long getMaxSizeBytes() { return maxSizeBytes; }
        public void setMaxSizeBytes(long maxSizeBytes) { this.maxSizeBytes = maxSizeBytes; }
    }
}