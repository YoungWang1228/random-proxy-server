package com.young.wang.rps.proxy.relay.repeater.http;

import com.young.wang.rps.proxy.util.ProxyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by YoungWang on 2019-02-11.
 */
public class HttpDirectTargetHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(HttpDirectTargetHandler.class);

    private HttpDirectTargetRepeater repeater;

    public HttpDirectTargetHandler(HttpDirectTargetRepeater repeater) {
        this.repeater = repeater;
    }

    @Override
    public void channelRead(ChannelHandlerContext targetCtx, Object inMsg) throws Exception {
        //目标主机的响应数据
        log.debug("HTTP 读取到响应.channelId:{}", ProxyUtil.getChannelId(this.repeater.getClientChannel()));
        if(inMsg instanceof HttpObject){
            this.repeater.relayToClient(inMsg);
        }else {
            log.warn("HTTP 读取到错误响应, 丢弃.channelId:{}", ProxyUtil.getChannelId(this.repeater.getClientChannel()));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext targetCtx, Throwable cause) throws Exception {
        log.debug("HTTP 转发异常.channelId:{}  目标:{}, 异常:{}", ProxyUtil.getChannelId(this.repeater.getClientChannel()), this.repeater.getTargetAddress(), cause.getMessage(), cause);
        this.repeater.targetDisconnect();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        this.repeater.targetDisconnect();
    }
}
