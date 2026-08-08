package com.aspbackup.receiver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * 基于 Netty 的 TCP 服务器，接受来自 ASPbackup 插件的连线，
 * 处理备份分块数据的接收与组装。
 */
public class ReceiverServer {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-接收端");

    private final int port;
    private final Path outputDir;
    private final String authToken;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public ReceiverServer(int port, Path outputDir, String authToken) {
        this.port = port;
        this.outputDir = outputDir;
        this.authToken = authToken;
    }

    /**
     * 启动接收端服务器。
     */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 帧解码器：读取长度前缀并提取帧
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                64 * 1024 * 1024, // 最大帧 64MB
                                0, 4,              // 长度字段偏移 0，大小 4 字节
                                0, 4));             // 跳过长度头
                        pipeline.addLast(new LengthFieldPrepender(4));
                        // 自定义备份协议处理器
                        pipeline.addLast(new ClientHandler(outputDir, authToken));
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        serverChannel = bootstrap.bind(port).sync().channel();
        LOGGER.info("接收端服务器已启动，监听端口 " + port);
    }

    /**
     * 停止接收端服务器。
     */
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 阻塞直到服务器通道关闭。
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (serverChannel != null) {
            serverChannel.closeFuture().sync();
        }
    }
}