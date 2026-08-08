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
 * Netty-based TCP server that accepts connections from the ASPbackup plugin
 * and handles backup chunk reception.
 */
public class ReceiverServer {

    private static final Logger LOGGER = Logger.getLogger("ASPbackup-Receiver");

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
     * Start the receiver server.
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
                        // Frame decoder: reads the length prefix and extracts the frame
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                64 * 1024 * 1024, // 64MB max frame
                                0, 4,              // length field at offset 0, size 4
                                0, 4));             // strip length header
                        pipeline.addLast(new LengthFieldPrepender(4));
                        // Custom handler for backup protocol
                        pipeline.addLast(new ClientHandler(outputDir, authToken));
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        serverChannel = bootstrap.bind(port).sync().channel();
        LOGGER.info("Receiver server started on port " + port);
    }

    /**
     * Stop the receiver server.
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
     * Block until the server channel is closed.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (serverChannel != null) {
            serverChannel.closeFuture().sync();
        }
    }
}