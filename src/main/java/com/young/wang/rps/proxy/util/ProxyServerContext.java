package com.young.wang.rps.proxy.util;

import com.young.wang.rps.config.ServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by YoungWang on 2019-02-13.
 */
@Component
public class ProxyServerContext {

    private final Bootstrap bootstrap;
    private final ExecutorService relayThreadPool;

    public ProxyServerContext(ServerConfig serverConfig) {
        this.bootstrap = new Bootstrap()
                .group(new NioEventLoopGroup(5))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, serverConfig.getConnectTimeoutMillis());

        this.relayThreadPool = Executors.newFixedThreadPool(serverConfig.getRelayThreadNum());
    }

    public Bootstrap buildBootstrap() {
        return bootstrap.clone();
    }

    public void executeRelayThread(Runnable command){
        this.relayThreadPool.execute(command);
    }

}
