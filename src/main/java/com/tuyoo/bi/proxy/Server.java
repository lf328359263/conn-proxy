package com.tuyoo.bi.proxy;

import com.tuyoo.bi.proxy.handler.ForwardProxyHandler;
import com.tuyoo.bi.proxy.handler.LimitHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

@Slf4j
public class Server {

    public static void main(String[] args) {
//        String host = "114.67.234.214";
//        int port = 6379;
//        String host = "biserverha.ywdier.com";
//        int port = 21050;
        String host = "10.8.28.45";
        int port = 8080;
        int targetPort = 8088;
        if (args.length > 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
            targetPort = Integer.parseInt(args[2]);
        }
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.channel(NioServerSocketChannel.class);
        UnorderedThreadPoolEventExecutor forwardChannelEventExecutors = new UnorderedThreadPoolEventExecutor(10, new DefaultThreadFactory("forward-thread"));
        LimitHandler limitHandler = new LimitHandler(2);
        EventLoopGroup bossGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("boss"));
        EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("worker"));

        serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);

        int finalPort = port;
        String finalHost = host;
        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(limitHandler);
                pipeline.addLast(forwardChannelEventExecutors, new ForwardProxyHandler(finalHost, finalPort));
//                pipeline.addLast(new HttpProxyHandler(InetSocketAddress.createUnresolved("nn45.ywdier.com", 8080)));
            }
        });

        try {
            ChannelFuture serverChannelFuture = serverBootstrap.bind(targetPort).sync();
            log.info("complete {}:{} -> {}", host, port, targetPort);
            serverChannelFuture.channel().closeFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("server start error ... ");
            e.printStackTrace();
        }

    }

}
