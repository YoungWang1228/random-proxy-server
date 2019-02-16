package com.young.wang.rps.proxy.relay.facade;

import com.young.wang.rps.proxy.relay.repeater.Repeater;
import com.young.wang.rps.proxy.relay.repeater.http.HttpDirectTargetRepeater;
import com.young.wang.rps.proxy.util.ProxyUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by YoungWang on 2019-02-11.
 */
@Component
public class HttpRelayFacade {
    private static final Logger log = LoggerFactory.getLogger(HttpRelayFacade.class);

    private Map<Channel, Repeater> cache = new HashMap<>();
    private ApplicationContext applicationContext;
    private Lock readLock;
    private Lock writeLock;

    public HttpRelayFacade(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        this.readLock = rwLock.readLock();
        this.writeLock = rwLock.writeLock();
    }

    public void relay(Channel channel, HttpObject httpObject){
        if(httpObject instanceof HttpRequest){
            log.debug("HTTP 请求.channelId:{}. 目标:{}", ProxyUtil.getChannelId(channel), ((HttpRequest)httpObject).uri());
        }
        this.readLock.lock();
        Repeater repeater;
        try {
            repeater = cache.get(channel);
        } finally {
            this.readLock.unlock();
        }

        if(repeater == null){
            String nextProxy = this.getNextProxy();
            this.writeLock.lock();
            try {
                // 双检查
                repeater = cache.get(channel);
                if(repeater != null){
                    repeater.relayToTarget(httpObject);
                    return;
                }
                if(httpObject instanceof HttpRequest){
                    HttpRequest request = (HttpRequest) httpObject;
                    if(nextProxy == null){
                        log.debug("HTTP 创建转发器.channelId:{}", ProxyUtil.getChannelId(channel));
                        repeater = new HttpDirectTargetRepeater(applicationContext, channel, ProxyUtil.getAddressByRequest(request));
                        cache.put(channel, repeater);
                    }else {
                        repeater = new HttpDirectTargetRepeater(applicationContext, channel, ProxyUtil.getAddressByRequest(request));
                        cache.put(channel, repeater);
                        // todo 代理待实现
                    }
                }else{
                    log.warn("HTTP 无效请求，连接关闭.channelId:{}", ProxyUtil.getChannelId(channel));
                    ProxyUtil.closeChannel(channel);
                    return;
                }
            } finally {
                this.writeLock.unlock();
            }
            repeater.connect();
            repeater.relayToTarget(httpObject);

        }else {
            // 连接存在
            repeater.relayToTarget(httpObject);
        }
    }

    public void disconnect(Channel channel){
        this.readLock.lock();
        Repeater repeater;
        try {
            repeater = cache.get(channel);
        } finally {
            this.readLock.unlock();
        }
        if(repeater != null){
            repeater.clientDisconnect();
        }
    }

    public void removeCache(Channel channel){
        this.writeLock.lock();
        try {
            cache.remove(channel);
        } finally {
            this.writeLock.unlock();
        }
    }

    private String getNextProxy(){
        return null;
    }

}
