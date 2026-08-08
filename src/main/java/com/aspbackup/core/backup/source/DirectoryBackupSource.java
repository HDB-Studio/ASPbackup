package com.aspbackup.core.backup.source;

import com.aspbackup.core.config.model.BackupConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Backs up a local directory with configurable filtering.
 */
public class DirectoryBackupSource implements BackupSource {

    private final Path rootPath;
    private final String name;
    private final FileCollector collector;
    private final List<String> extraExcludes;
    private final Logger logger;

    private List<FileEntry> cachedFiles;

    public DirectoryBackupSource(Path rootPath, String name,
                                  BackupConfig.FileFilterDef globalFilter,
                                  List<String> extraIncludes,
                                  List<String> extraExcludes, int maxDepth,
                                  Logger logger) {
        this.rootPath = rootPath;
        this.name = name;
        this.extraExcludes = extraExcludes != null ? extraExcludes : List.of();
        this.logger = logger;

        // 合并全局 include 和来源特定 include
        List<String> mergedIncludes = new ArrayList<>(globalFilter.getInclude());
        if (extraIncludes != null) {
            mergedIncludes.addAll(extraIncludes);
        }

        this.collector = new FileCollector(
                mergedIncludes,
                globalFilter.getExclude(),
                globalFilter.getMinSizeBytes(),
                globalFilter.getMaxSizeBytes(),
                maxDepth
        );
    }

    @Override
    public List<FileEntry> collectFiles() {
        if (cachedFiles != null) {
            return cachedFiles;
        }

        try {
            logger.info("Collecting files from: " + rootPath);
            cachedFiles = collector.collect(rootPath, extraExcludes);
            logger.info("Collected " + cachedFiles.size() + " files from " + name);
            return cachedFiles;
        } catch (IOException e) {
            logger.severe("Failed to collect files from " + rootPath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public long getTotalSize() {
        return collectFiles().stream().mapToLong(FileEntry::getSize).sum();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Path getRootPath() {
        return rootPath;
    }
}