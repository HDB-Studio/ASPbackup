package com.aspbackup.core.backup;

/**
 * Represents the current state of a backup task.
 */
public enum BackupState {

    /** Task has been created but not yet started */
    INITIALIZING,

    /** Task is actively collecting files */
    COLLECTING,

    /** Task is compressing collected files */
    COMPRESSING,

    /** Task is transferring backup to target */
    TRANSFERRING,

    /** Task is verifying backup integrity */
    VERIFYING,

    /** Task has been paused and checkpoint saved */
    PAUSED,

    /** Task completed successfully */
    COMPLETED,

    /** Task was cancelled by administrator */
    CANCELLED,

    /** Task failed due to an error */
    FAILED

}