package com.young.wang.rps.proxy.relay.repeater.http;

import com.young.wang.rps.proxy.relay.facade.HttpRelayFacade;
import com.young.wang.rps.proxy.relay.repeater.AbstractRepeater;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;

/**
 * Created by YoungWang on 2019-02-13.
 */
public class HttpDirectTargetRepeater extends AbstractRepeater {
    private static final Logger log = LoggerFactory.getLogger(HttpDirectTargetRepeater.class);

    private HttpRelayFacade httpRelayFacade;

    public HttpDirectTargetRepeater(ApplicationContext applicationContext, Channel clientChannel, InetSocketAddress targetAddress) {
        super(applicationContext, clientChannel, targetAddress);
        this.httpRelayFacade = applicationContext.getBean(HttpRelayFacade.class);
    }

    @Override
    protected void clearCache(Channel clientChannel) {
        this.httpRelayFacade.removeCache(clientChannel);
    }

    @Override
    protected String repeaterType() {
        return "HTTP DT";
    }

    @Override
    protected ChannelFuture connectTarget() throws Exception {
        var that = this;
        ChannelFuture targetChannelFuture = proxyServerContext.buildBootstrap()
                .handler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpDirectTargetHandler(that));
                    }
                })
                .connect(targetAddress);
        targetChannelFuture.await();
        if(targetChannelFuture.isSuccess()){
            super.changeState(1);
            log.debug("{} 与目标主机建立连接成功.channelId:{}  目标:{}", repeaterType(), channelId, targetAddress);
        }else {
            Throwable cause = targetChannelFuture.cause();
            log.warn("{} 与目标主机建立连接失败.channelId:{}  目标:{}, 异常:{}", repeaterType(), channelId, targetAddress, cause.getMessage(), cause);
            super.changeState(2);
        }
        return targetChannelFuture;
    }

    @Override
    protected void relayRequestToTarget(Object request) throws Exception {
        if(this.targetChannelFuture!=null && this.targetChannelFuture.channel().isActive()){
            Object uri = this.targetAddress;
            if(request instanceof HttpRequest){
                HttpRequest hr = (HttpRequest) request;
                HttpHeaders headers = hr.headers();
                if(headers.get("Proxy-Connection") != null){
                    headers.add("Connection", headers.get("Proxy-Connection"));
                }
                headers.remove("Proxy-Connection");
                uri = hr.uri();
            }

            this.targetChannelFuture.channel()
                    .writeAndFlush(request)
//                    .await()
            ;
//            if(targetChannelFuture.isSuccess()){
//                log.debug("{} 转发成功.channelId:{}  目标:{}", repeaterType(), channelId, uri);
//            }else {
//                log.warn("{} 转发失败.channelId:{}  目标:{}, 异常:{}", repeaterType(), channelId, uri, targetChannelFuture.cause().getMessage(), targetChannelFuture.cause());
//                changeState(2);
//            }
        }else {
            log.warn("{} 转发失败，目标无法连接.channelId:{}  目标:{}", repeaterType(), channelId, targetAddress);
            changeState(2);
        }
    }

    @Override
    protected void relayResponseToClient(Object response) throws Exception {
        if(clientChannel.isActive()){
            //发回给客户端
            ChannelFuture future = clientChannel.writeAndFlush(response);
//            future.await();
//            if(future.isSuccess())
//                log.debug("{} 回写数据成功.channelId:{}", repeaterType(), channelId);
//            else {
//                log.debug("{} 回写数据失败.channelId:{}. e:{}", repeaterType(), channelId, future.cause().getMessage(), future.cause());
//                changeState(2);
//            }
        }else {
            log.warn("{} 回写数据失败，客户端连接断开.channelId:{}  目标:{}", repeaterType(), channelId, targetAddress);
            changeState(2);
        }
    }

}
