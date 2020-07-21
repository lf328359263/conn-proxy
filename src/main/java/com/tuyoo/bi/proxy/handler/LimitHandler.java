package com.tuyoo.bi.proxy.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ChannelHandler.Sharable
public class LimitHandler extends ChannelDuplexHandler {

    private final AtomicInteger totalConnectionNumber = new AtomicInteger();
    private final int limitCount;

    public LimitHandler(int limitCount) {
        this.limitCount = limitCount;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int currConnection = totalConnectionNumber.getAndIncrement();
        if (currConnection >= limitCount) {
            ctx.close();
            log.warn("连接已达到上限： {} / {}",  currConnection,  limitCount);
        } else {
            log.info("新增连接，当前连接数：{} ", totalConnectionNumber.get());
            super.channelActive(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int currConnection = totalConnectionNumber.decrementAndGet();
        log.info("连接关闭，当前连接数：{} ", currConnection);
        super.channelInactive(ctx);
    }

}
