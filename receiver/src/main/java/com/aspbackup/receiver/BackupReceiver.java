package com.aspbackup.receiver;

import com.aspbackup.receiver.server.ReceiverServer;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ASPbackup 备份接收端 — 主入口。
 * 独立运行的应用程序，接收来自 ASPbackup Spigot 插件的备份数据，
 * 将分布式传输的分块重新组装为完整备份档案。
 *
 * 用法：java -jar ASPbackup-receiver.jar [--port <端口>] [--dir <输出目录>] [--token <认证令牌>]
 */
public class BackupReceiver {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-接收端");

    private static int port = 9876;
    private static String outputDir = "received-backups";
    private static String authToken = "change-me";

    public static void main(String[] args) {
        parseArgs(args);

        LOGGER.info("============================================");
        LOGGER.info("  ASPbackup 备份接收端 v1.0.0");
        LOGGER.info("============================================");
        LOGGER.info("  监听端口：" + port);
        LOGGER.info("  输出目录：" + outputDir);
        LOGGER.info("============================================");

        try {
            ReceiverServer server = new ReceiverServer(port, Path.of(outputDir), authToken);
            server.start();

            // 注册关闭钩子，确保优雅退出
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("正在关闭接收端...");
                server.stop();
                LOGGER.info("接收端已停止。");
            }));

            LOGGER.info("接收端已就绪，等待备份连线...");
            server.blockUntilShutdown();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "接收端启动失败", e);
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
                    System.out.println("用法：java -jar ASPbackup-receiver.jar [选项]");
                    System.out.println("  --port <端口>      监听端口（预设：9876）");
                    System.out.println("  --dir <路径>       备份输出目录（预设：received-backups）");
                    System.out.println("  --token <令牌>     认证令牌（预设：change-me）");
                    System.out.println("  --help             显示此帮助信息");
                    System.exit(0);
                    break;
            }
        }
    }
}