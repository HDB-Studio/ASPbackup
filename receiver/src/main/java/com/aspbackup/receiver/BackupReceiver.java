package com.aspbackup.receiver;

import com.aspbackup.receiver.server.ReceiverServer;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the ASPbackup Receiver application.
 * Accepts connections from the ASPbackup Spigot plugin and assembles
 * backup data from distributed chunks.
 *
 * Usage: java -jar ASPbackup-receiver.jar [--port <port>] [--dir <output-dir>] [--token <auth-token>]
 */
public class BackupReceiver {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-Receiver");

    private static int port = 9876;
    private static String outputDir = "received-backups";
    private static String authToken = "change-me";

    public static void main(String[] args) {
        parseArgs(args);

        LOGGER.info("============================================");
        LOGGER.info("  ASPbackup Receiver v1.0.0");
        LOGGER.info("============================================");
        LOGGER.info("  Port:        " + port);
        LOGGER.info("  Output dir:  " + outputDir);
        LOGGER.info("============================================");

        try {
            ReceiverServer server = new ReceiverServer(port, Path.of(outputDir), authToken);
            server.start();

            // Register shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down receiver...");
                server.stop();
                LOGGER.info("Receiver stopped.");
            }));

            LOGGER.info("Receiver is ready. Waiting for connections...");
            server.blockUntilShutdown();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start receiver", e);
            System.exit(1);
        }
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 < args.length) port = Integer.parseInt(args[++i]);
                    break;
                case "--dir":
                    if (i + 1 < args.length) outputDir = args[++i];
                    break;
                case "--token":
                    if (i + 1 < args.length) authToken = args[++i];
                    break;
                case "--help":
                    System.out.println("Usage: java -jar ASPbackup-receiver.jar [options]");
                    System.out.println("  --port <port>      Listening port (default: 9876)");
                    System.out.println("  --dir <path>       Output directory for backups (default: received-backups)");
                    System.out.println("  --token <token>    Authentication token (default: change-me)");
                    System.exit(0);
                    break;
            }
        }
    }
}