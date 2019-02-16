package com.young.wang.rps.proxy.relay.facade;

import com.young.wang.rps.proxy.relay.repeater.Repeater;
import com.young.wang.rps.proxy.relay.repeater.https.HttpsDirectTargetRepeater;
import com.young.wang.rps.proxy.util.ProxyUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class HttpsRelayFacade {
    private static final Logger log = LoggerFactory.getLogger(HttpsRelayFacade.class);

    private Map<Channel, Repeater> cache = new HashMap<>();
    private ApplicationContext applicationContext;
    private Lock readLock;
    private Lock writeLock;

    @Autowired
    public HttpsRelayFacade(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        this.readLock = rwLock.readLock();
        this.writeLock = rwLock.writeLock();
    }

    public void connect(Channel channel, HttpRequest request){
        String nextProxy = this.getNextProxy();
        Repeater repeater;
        this.writeLock.lock();
        try {
            if(nextProxy == null){
                repeater = new HttpsDirectTargetRepeater(applicationContext, channel, ProxyUtil.getAddressByRequest(request));
                cache.put(channel, repeater);
            }else {
                repeater = new HttpsDirectTargetRepeater(applicationContext, channel, ProxyUtil.getAddressByRequest(request));
                cache.put(channel, repeater);
                // todo 代理待实现
            }
        } finally {
            this.writeLock.unlock();
        }
        repeater.connect();
    }

    public void relay(Channel channel, byte[] data){
        this.readLock.lock();
        Repeater repeater;
        try {
            repeater = cache.get(channel);
        } finally {
            this.readLock.unlock();
        }

        if(repeater != null){
            repeater.relayToTarget(data);
        }else {
            ProxyUtil.closeChannel(channel);
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
