package com.young.wang.rps.proxy.acceptor.handler;

import com.young.wang.rps.proxy.relay.facade.HttpRelayFacade;
import com.young.wang.rps.proxy.relay.facade.HttpsRelayFacade;
import com.young.wang.rps.proxy.util.ProxyUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by YoungWang on 2019-02-11.
 */
@Component
@ChannelHandler.Sharable
public class ProxyAcceptMsgHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(ProxyAcceptMsgHandler.class);

    private final HttpRelayFacade httpRelayFacade;
    private final HttpsRelayFacade httpsRelayFacade;

    public ProxyAcceptMsgHandler(HttpRelayFacade httpRelayFacade, HttpsRelayFacade httpsRelayFacade) {
        this.httpRelayFacade = httpRelayFacade;
        this.httpsRelayFacade = httpsRelayFacade;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpObject){
            if(msg instanceof HttpRequest){
                HttpRequest request = (HttpRequest) msg;
                if (HttpMethod.CONNECT.equals(request.method())){
                    // HTTPS CONNECT
                    this.httpsRelayFacade.connect(ctx.channel(), (HttpRequest) msg);
                }else {
                    // 处理 http 请求
                    this.httpRelayFacade.relay(ctx.channel(), (HttpRequest) msg);
                }
            }else {
                if(msg == LastHttpContent.EMPTY_LAST_CONTENT){
                    return;
                }
                this.httpRelayFacade.relay(ctx.channel(), (HttpObject) msg);
            }

        }else if (msg instanceof byte[]){
            // byte 数组 是HTTPS 传输的加密数据
            this.httpsRelayFacade.relay(ctx.channel(), (byte[]) msg);
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.debug("通道id:{} ,客户端关闭连接.", ProxyUtil.getChannelId(ctx));

        this.httpRelayFacade.disconnect(ctx.channel());
        this.httpsRelayFacade.disconnect(ctx.channel());
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("通道id:{} , 发生异常:{}",ProxyUtil.getChannelId(ctx), cause.getMessage(), cause);

        this.httpRelayFacade.disconnect(ctx.channel());
        this.httpsRelayFacade.disconnect(ctx.channel());
    }

}
