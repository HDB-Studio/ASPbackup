package com.aspbackup.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread utility methods.
 */
public final class ThreadUtil {

    private ThreadUtil() {}

    /**
     * Create a daemon thread factory with a name prefix.
     */
    public static ThreadFactory daemonThreadFactory(String namePrefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, namePrefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) -> {
                System.err.println("Uncaught exception in thread " + thread.getName() + ": " + ex.getMessage());
            });
            return t;
        };
    }

    /**
     * Sleep for the specified milliseconds, ignoring interrupts.
     */
    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}