package com.tuyoo.bi.proxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class ForwardProxyHandler extends ChannelInboundHandlerAdapter {

    private ChannelFuture cf;
    private final String host;
    private final int port;
    Timer timer = new Timer();

    public ForwardProxyHandler(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void createClient(ChannelHandlerContext ctx, Object msg) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .channel(ctx.channel().getClass())
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch)  {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx0, Object msg) {
                                ctx.channel().writeAndFlush(msg);
                            }
                        });
                    }

                });
        cf = bootstrap.connect(host, port);
        cf.channel().closeFuture().addListener(future -> {
            log.info("close client channel ... ");
        });
        log.info("create connection [{}:{}] ...", host, port);
        cf.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("client conn first ... ");
                future.channel().writeAndFlush(msg);
            } else {
                log.warn("connection failed [{}:{}] ...", host, port);
                ctx.channel().close();
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (cf == null) {
            createClient(ctx, msg);
        } else {
//            log.info("server msg to client ... ");
            cf.channel().writeAndFlush(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cf.channel().close();
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                ctx.channel().close();
//            }
//        }, 5000);
        super.channelActive(ctx);
    }
}
