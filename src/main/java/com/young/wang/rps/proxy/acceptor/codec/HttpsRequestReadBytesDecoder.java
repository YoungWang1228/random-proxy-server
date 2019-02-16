package com.young.wang.rps.proxy.acceptor.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by YoungWang on 2019-02-11.
 */
public class HttpsRequestReadBytesDecoder extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof ByteBuf){
            ByteBuf in = (ByteBuf) msg;
            try {
                byte[] data = new byte[in.readableBytes()];
                in.getBytes(0, data);

                ctx.fireChannelRead(data);
            } finally {
                in.release();
            }
        }else {
            ctx.fireChannelRead(msg);
        }
    }



}
