package com.young.wang.rps.proxy.relay.repeater.https;

import com.young.wang.rps.proxy.acceptor.codec.HttpsRequestReadBytesDecoder;
import com.young.wang.rps.proxy.acceptor.codec.HttpsResponseWriteBytesEncoder;
import com.young.wang.rps.proxy.relay.facade.HttpsRelayFacade;
import com.young.wang.rps.proxy.relay.repeater.AbstractRepeater;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;

/**
 * Created by YoungWang on 2019-02-11.
 */
public class HttpsDirectTargetRepeater extends AbstractRepeater {
    private static final Logger log = LoggerFactory.getLogger(HttpsDirectTargetRepeater.class);

    private HttpsRelayFacade httpsRelayFacade;

    public HttpsDirectTargetRepeater(ApplicationContext applicationContext, Channel clientChannel, InetSocketAddress targetAddress) {
        super(applicationContext, clientChannel, targetAddress);
        this.httpsRelayFacade = applicationContext.getBean(HttpsRelayFacade.class);
    }

    @Override
    protected void clearCache(Channel clientChannel) {
        this.httpsRelayFacade.removeCache(clientChannel);
    }

    @Override
    protected String repeaterType() {
        return "HTTPS DT";
    }

    @Override
    protected ChannelFuture connectTarget() throws Exception {
        var that = this;
        ChannelFuture targetChannelFuture = proxyServerContext.buildBootstrap()
                .handler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new HttpsRequestReadBytesDecoder())
                                .addLast(new HttpsResponseWriteBytesEncoder())
                                .addLast(new HttpsDirectTargetHandler(that));
                    }
                })
                .connect(targetAddress);
        targetChannelFuture.await();

        if(targetChannelFuture.isSuccess()){
            if(clientChannel.isActive()){
                changeState(1);
                log.debug("{} 与目标主机建立连接成功.channelId:{}  目标:{}", repeaterType(), channelId, targetAddress);

                // 将报文解码的处理器去除,因为后续客户端请求的内容不解码
                if(clientChannel.pipeline().get("HTTP-REQUEST-DECODER") != null){
                    clientChannel.pipeline().remove("HTTP-REQUEST-DECODER");
                }
                clientChannel.pipeline()
                        .addFirst(new HttpsRequestReadBytesDecoder())
                        .addFirst(new HttpsResponseWriteBytesEncoder());
                // 发送应答
                this.relayToClient(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
            }else {
                changeState(2);
                log.warn("{} 客户端连接失效.channelId:{}  目标:{}", repeaterType(), channelId, targetAddress);
            }
        }else {
            Throwable cause = targetChannelFuture.cause();
            log.warn("{} 与目标主机建立连接失败.channelId:{}  目标:{}, 异常:{}", repeaterType(), channelId, targetAddress, cause.getMessage(), cause);
            changeState(2);
        }
        return targetChannelFuture;
    }

    @Override
    protected void relayRequestToTarget(Object request) throws Exception {
        if(this.targetChannelFuture!=null && this.targetChannelFuture.channel().isActive()){
            this.targetChannelFuture.channel()
                    .writeAndFlush(request)
//                    .await()
            ;
//            if(targetChannelFuture.isSuccess()){
//                log.debug("{} 转发成功.channelId:{}  目标:{}", repeaterType(), channelId, targetAddress);
//            }else {
//                log.warn("{} 转发失败.channelId:{}  目标:{}, 异常:{}", repeaterType(), channelId, targetAddress, targetChannelFuture.cause().getMessage(), targetChannelFuture.cause());
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
//                log.warn("{} 回写数据失败.channelId:{}. e:{}", repeaterType(), channelId, future.cause().getMessage(), future.cause());
//                changeState(2);
//            }
        }else {
            log.warn("{} 回写数据失败，客户端连接断开.channelId:{}  目标:{}", repeaterType(), channelId, targetAddress);
            changeState(2);
        }
    }
}
