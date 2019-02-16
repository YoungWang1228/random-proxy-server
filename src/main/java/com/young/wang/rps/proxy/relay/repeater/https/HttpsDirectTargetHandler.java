package com.young.wang.rps.proxy.relay.repeater.https;

import com.young.wang.rps.proxy.util.ProxyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by YoungWang on 2019-02-11.
 */
public class HttpsDirectTargetHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(HttpsDirectTargetHandler.class);

    private HttpsDirectTargetRepeater repeater;

    public HttpsDirectTargetHandler(HttpsDirectTargetRepeater repeater) {
        this.repeater = repeater;
    }

    @Override
    public void channelRead(ChannelHandlerContext targetCtx, Object inMsg) throws Exception {
        log.debug("HTTPS 读取到响应.channelId:{}", ProxyUtil.getChannelId(this.repeater.getClientChannel()));
        this.repeater.relayToClient(inMsg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext targetCtx) throws Exception {
        this.repeater.targetDisconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext targetCtx, Throwable cause) throws Exception {
        log.debug("HTTPS 转发异常.channelId:{}  目标:{}, 异常:{}", ProxyUtil.getChannelId(this.repeater.getClientChannel()), this.repeater.getTargetAddress(), cause.getMessage(), cause);
        this.repeater.targetDisconnect();
    }


}
