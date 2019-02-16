package com.young.wang.rps.proxy.acceptor.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * Created by YoungWang on 2019-02-14.
 */
public class HttpsResponseWriteBytesEncoder extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof byte[]){
            ByteBuf buf = ctx.alloc().ioBuffer();
            buf.writeBytes((byte[]) msg);
            ctx.write(buf, promise);
        }else {
            super.write(ctx, msg, promise);
        }
    }
}
