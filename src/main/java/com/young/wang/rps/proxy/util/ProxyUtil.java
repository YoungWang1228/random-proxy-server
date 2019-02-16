package com.young.wang.rps.proxy.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ProxyUtil {
	private static final Logger log = LoggerFactory.getLogger(ProxyUtil.class);

	public static String getChannelId(ChannelHandlerContext ctx) {
		return ctx.channel().id().asShortText();
	}

    public static String getChannelId(Channel channel) {
        return channel.id().asShortText();
    }

	public static void closeChannel(Channel channel){
		if(channel != null && channel.isOpen()){
			channel.close();
		}
	}

	public static InetSocketAddress getAddressByRequest(HttpRequest request) {
		String[] temp1 = request.headers().get("host").split(":");
		return  new InetSocketAddress(temp1[0], temp1.length == 1 ? 80 : Integer.parseInt(temp1[1]));
	}







    /**
     *  从 tail 发 ， 即所有的 Outbound 都会被执行
     */
    public static void responseFailedAndClose(Channel channel) {
        if(channel.isActive()){
            channel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_TIMEOUT));
        }
        closeChannel(channel);
    }

    /**
     *  从当前 handler 发， 排在当前 handler 后面的 Outbound 不会被执行
     */
    public static void responseFailedAndClose(ChannelHandlerContext ctx) {
        if(ctx.channel().isActive()){
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_TIMEOUT));
        }
        closeChannel(ctx.channel());
    }

}
