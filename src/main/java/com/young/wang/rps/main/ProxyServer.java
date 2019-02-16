package com.young.wang.rps.main;

import com.young.wang.rps.config.ServerConfig;
import com.young.wang.rps.proxy.acceptor.handler.ProxyAcceptMsgHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Created by YoungWang on 2019-02-09.
 */
@Component
public class ProxyServer {
    private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

    private final ServerConfig serverConfig;
    private final ProxyAcceptMsgHandler proxyAcceptMsgHandler;

    public ProxyServer(ServerConfig serverConfig, ProxyAcceptMsgHandler proxyAcceptMsgHandler) {
        this.serverConfig = serverConfig;
        this.proxyAcceptMsgHandler = proxyAcceptMsgHandler;
    }

    public void start() {
        log.info(" >>> 代理服务器 >>> 启动中...");

        EventLoopGroup bossGroup = new NioEventLoopGroup(this.serverConfig.getNioEventThreadNum());
        EventLoopGroup workerGroup = new NioEventLoopGroup(this.serverConfig.getAcceptorThreadNum());
        ServerBootstrap serverBootStrap = new ServerBootstrap();

        serverBootStrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast("HTTP-REQUEST-DECODER", new HttpRequestDecoder())
                                .addLast("HTTP-RESPONSE-ENCODER", new HttpResponseEncoder())
                                //自定义 客户端输入事件 处理器
                                .addLast("PROXY_ACCEPT_MSG_HANDLER", proxyAcceptMsgHandler);
                    }
                })
                //服务端接受连接的队列长度
                .option(ChannelOption.SO_BACKLOG, 2048)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.serverConfig.getConnectTimeoutMillis())
                //接收缓冲区大小
                .option(ChannelOption.SO_RCVBUF, 128 * 1024);

        log.info(" >>> 代理服务器 >>> 已启动 , 端口: {} ", this.serverConfig.getProxyPort());

        try {
            ChannelFuture future = serverBootStrap.bind(this.serverConfig.getProxyPort()).sync();
            future.channel().closeFuture().sync();
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
