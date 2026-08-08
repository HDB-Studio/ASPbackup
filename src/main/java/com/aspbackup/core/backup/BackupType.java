package com.aspbackup.core.backup;

/**
 * Represents the type of backup operation.
 */
public enum BackupType {

    /** Full backup - backs up all files in the source */
    FULL,

    /** Incremental backup - only backs up files changed since last backup */
    INCREMENTAL

}