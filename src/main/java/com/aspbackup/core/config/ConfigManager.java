package com.aspbackup.core.config;

import com.aspbackup.ASPBackup;
import com.aspbackup.core.config.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages all plugin configuration, loading from config.yml
 * and providing typed access to configuration sections.
 */
public class ConfigManager {

    private final ASPBackup plugin;
    private FileConfiguration config;

    private BackupConfig backupConfig;
    private TransferConfig transferConfig;
    private ScheduleConfig scheduleConfig;
    private List<NodeConfig> nodeConfigs;

    // Logging & disk space settings
    private String logLevel = "INFO";
    private String logDirectory = "plugins/ASPbackup/logs";
    private int logRetentionDays = 30;
    private boolean verboseTransfer = false;
    private boolean consoleOutput = true;
    private boolean checkpointEnabled = true;
    private String checkpointDirectory = "plugins/ASPbackup/checkpoints";
    private int checkpointMaxAgeDays = 7;
    private int diskSpaceWarnPercent = 15;
    private int diskSpaceCheckInterval = 300;

    public ConfigManager(ASPBackup plugin) {
        this.plugin = plugin;
    }

    /**
     * Load or reload configuration from config.yml.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        loadBackupConfig();
        loadTransferConfig();
        loadScheduleConfig();
        loadLoggingConfig();
        loadCheckpointConfig();
        loadDiskSpaceConfig();

        plugin.getLogger().info("Configuration loaded successfully.");
    }

    /**
     * Reload configuration from disk.
     */
    public void reload() {
        load();
        plugin.getLogger().info("Configuration reloaded.");
    }

    private void loadBackupConfig() {
        backupConfig = new BackupConfig();
        ConfigurationSection sec = config.getConfigurationSection("backup");
        if (sec == null) {
            plugin.getLogger().warning("No 'backup' section found in config, using defaults.");
            return;
        }

        backupConfig.setAutoOnStart(sec.getBoolean("auto-on-start", true));
        backupConfig.setAutoOnShutdown(sec.getBoolean("auto-on-shutdown", true));
        backupConfig.setStartDelaySeconds(sec.getInt("start-delay-seconds", 30));
        backupConfig.setTempDirectory(sec.getString("temp-directory", "plugins/ASPbackup/temp"));
        backupConfig.setMaxBackupSizeMb(sec.getLong("max-backup-size-mb", 0));
        backupConfig.setVerifyAfterBackup(sec.getBoolean("verify-after-backup", true));

        // Compression settings
        ConfigurationSection compSec = sec.getConfigurationSection("compression");
        if (compSec != null) {
            backupConfig.setCompressionFormat(compSec.getString("format", "targz"));
            backupConfig.setCompressionLevel(compSec.getInt("level", 6));
        }

        // Sources
        List<BackupConfig.SourceDef> sources = new ArrayList<>();
        List<?> sourceList = sec.getList("sources");
        if (sourceList != null) {
            for (Object obj : sourceList) {
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    var map = (java.util.Map<String, Object>) obj;
                    BackupConfig.SourceDef src = new BackupConfig.SourceDef();
                    src.setPath(String.valueOf(map.getOrDefault("path", "")));
                    src.setName(String.valueOf(map.getOrDefault("name", "")));
                    Object exc = map.get("exclude");
                    if (exc instanceof List) {
                        @SuppressWarnings("unchecked")
                        var excList = (List<String>) exc;
                        src.setExclude(excList);
                    }
                    Object md = map.get("max-depth");
                    if (md instanceof Number) {
                        src.setMaxDepth(((Number) md).intValue());
                    }
                    sources.add(src);
                }
            }
        }
        backupConfig.setSources(sources);

        // Targets
        List<BackupConfig.TargetDef> targets = new ArrayList<>();
        List<?> targetList = sec.getList("targets");
        if (targetList != null) {
            for (Object obj : targetList) {
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    var map = (java.util.Map<String, Object>) obj;
                    BackupConfig.TargetDef tgt = new BackupConfig.TargetDef();
                    tgt.setId(String.valueOf(map.getOrDefault("id", "")));
                    tgt.setType(String.valueOf(map.getOrDefault("type", "LOCAL")));
                    tgt.setPath(String.valueOf(map.getOrDefault("path", "backups/")));
                    Object rc = map.get("retention-count");
                    if (rc instanceof Number) tgt.setRetentionCount(((Number) rc).intValue());
                    Object fs = map.get("min-free-space-mb");
                    if (fs instanceof Number) tgt.setMinFreeSpaceMb(((Number) fs).longValue());
                    targets.add(tgt);
                }
            }
        }
        backupConfig.setTargets(targets);

        // File filter
        ConfigurationSection ffSec = sec.getConfigurationSection("file-filter");
        if (ffSec != null) {
            BackupConfig.FileFilterDef ff = new BackupConfig.FileFilterDef();
            ff.setInclude(ffSec.getStringList("include"));
            ff.setExclude(ffSec.getStringList("exclude"));
            ff.setMinSizeBytes(ffSec.getLong("min-size-bytes", 0));
            ff.setMaxSizeBytes(ffSec.getLong("max-size-bytes", 0));
            backupConfig.setFileFilter(ff);
        }
    }

    private void loadTransferConfig() {
        transferConfig = new TransferConfig();
        ConfigurationSection sec = config.getConfigurationSection("transfer");
        if (sec == null) return;

        transferConfig.setChunkSizeKb(sec.getInt("chunk-size-kb", 1024));
        transferConfig.setParallelThreads(sec.getInt("parallel-threads", 4));
        transferConfig.setConnectTimeoutMs(sec.getInt("connect-timeout-ms", 10000));
        transferConfig.setReadTimeoutMs(sec.getInt("read-timeout-ms", 30000));
        transferConfig.setRetryCount(sec.getInt("retry-count", 3));
        transferConfig.setRetryDelayMs(sec.getInt("retry-delay-ms", 5000));
        transferConfig.setLoadBalanceStrategy(sec.getString("load-balance-strategy", "least_loaded"));

        // Nodes
        nodeConfigs = new ArrayList<>();
        List<?> nodeList = sec.getList("nodes");
        if (nodeList != null) {
            for (Object obj : nodeList) {
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    var map = (java.util.Map<String, Object>) obj;
                    NodeConfig nc = new NodeConfig();
                    nc.setId(String.valueOf(map.getOrDefault("id", "")));
                    nc.setHost(String.valueOf(map.getOrDefault("host", "")));
                    Object port = map.get("port");
                    if (port instanceof Number) nc.setPort(((Number) port).intValue());
                    nc.setAuthToken(String.valueOf(map.getOrDefault("auth-token", "")));
                    Object en = map.get("enabled");
                    if (en instanceof Boolean) nc.setEnabled((Boolean) en);
                    Object wt = map.get("weight");
                    if (wt instanceof Number) nc.setWeight(((Number) wt).intValue());
                    nodeConfigs.add(nc);
                }
            }
        }
        transferConfig.setNodes(nodeConfigs);
    }

    private void loadScheduleConfig() {
        scheduleConfig = new ScheduleConfig();
        ConfigurationSection sec = config.getConfigurationSection("schedule");
        if (sec == null) return;

        scheduleConfig.setEnabled(sec.getBoolean("enabled", false));
        scheduleConfig.setIntervalMinutes(sec.getInt("interval-minutes", 360));
        scheduleConfig.setBackupType(sec.getString("backup-type", "full"));
        scheduleConfig.setTargetId(sec.getString("target-id", "local"));
        scheduleConfig.setQuietHours(sec.getStringList("quiet-hours"));
    }

    private void loadLoggingConfig() {
        ConfigurationSection sec = config.getConfigurationSection("logging");
        if (sec == null) return;

        logLevel = sec.getString("level", "INFO");
        logDirectory = sec.getString("directory", "plugins/ASPbackup/logs");
        logRetentionDays = sec.getInt("retention-days", 30);
        verboseTransfer = sec.getBoolean("verbose-transfer", false);
        consoleOutput = sec.getBoolean("console-output", true);
    }

    private void loadCheckpointConfig() {
        ConfigurationSection sec = config.getConfigurationSection("checkpoint");
        if (sec == null) return;

        checkpointEnabled = sec.getBoolean("enabled", true);
        checkpointDirectory = sec.getString("directory", "plugins/ASPbackup/checkpoints");
        checkpointMaxAgeDays = sec.getInt("max-age-days", 7);
    }

    private void loadDiskSpaceConfig() {
        ConfigurationSection sec = config.getConfigurationSection("disk-space");
        if (sec == null) return;

        diskSpaceWarnPercent = sec.getInt("warn-threshold-percent", 15);
        diskSpaceCheckInterval = sec.getInt("check-interval-seconds", 300);
    }

    // --- Getters ---

    public BackupConfig getBackupConfig() { return backupConfig; }
    public TransferConfig getTransferConfig() { return transferConfig; }
    public ScheduleConfig getScheduleConfig() { return scheduleConfig; }
    public List<NodeConfig> getNodeConfigs() { return nodeConfigs; }

    public String getLogLevel() { return logLevel; }
    public String getLogDirectory() { return logDirectory; }
    public int getLogRetentionDays() { return logRetentionDays; }
    public boolean isVerboseTransfer() { return verboseTransfer; }
    public boolean isConsoleOutput() { return consoleOutput; }

    public boolean isCheckpointEnabled() { return checkpointEnabled; }
    public String getCheckpointDirectory() { return checkpointDirectory; }
    public int getCheckpointMaxAgeDays() { return checkpointMaxAgeDays; }

    public int getDiskSpaceWarnPercent() { return diskSpaceWarnPercent; }
    public int getDiskSpaceCheckInterval() { return diskSpaceCheckInterval; }
}