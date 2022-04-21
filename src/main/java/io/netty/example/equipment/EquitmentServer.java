package io.netty.example.equipment;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EquitmentServer {

    private static final Logger logger = LoggerFactory.getLogger(EquitmentServer.class);

    private int port = 21419;

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workGroup;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private NettyServerConfig nettyServerConfig;

    protected void doConnect() {
        logger.info(String.format("Start EquitmentServer Connector bind on %d", port));
        try {
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            logger.info(String.format("EquitmentServer Connector accept on  %d", port));
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
        }

    }

    protected void doInitialise() throws Exception {


        nettyServerConfig = new NettyServerConfig();


        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(//
                nettyServerConfig.getServerWorkerThreads(), //
                new ThreadFactory() {
                    private AtomicInteger threadIndex = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyServerWorkerThread_" + this.threadIndex.incrementAndGet());
                    }
                });
        bossGroup = new NioEventLoopGroup(1);
        workGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();

        ServerBootstrap childHandler = serverBootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_BACKLOG, 1024).option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.SocketSndbufSize).childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.SocketRcvbufSize)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // Create a default pipeline implementation.
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new EquitmentDecode());
                        p.addLast(defaultEventExecutorGroup, new EquitmentHandler());
                    }

                });

        if (nettyServerConfig.NettyPooledByteBufAllocatorEnable) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
    }

    protected void doStart() throws Exception {
        try {
            doInitialise();
            init();
            doConnect();
        } catch (Exception e) {
            throw e;
        } finally {
            doDispose();
        }

    }

    protected void doDispose() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
        defaultEventExecutorGroup.shutdownGracefully();
    }

    public void start() throws Exception {
        doStart();
    }

    public void init() {
    }

    public static void main(String[] args) {
        EquitmentServer equitmentServer = new EquitmentServer();
        try {
            equitmentServer.start();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

}
