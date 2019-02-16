package com.young.wang.rps.proxy.relay.repeater;

import com.young.wang.rps.proxy.util.ProxyServerContext;
import com.young.wang.rps.proxy.util.ProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by YoungWang on 2019-02-15.
 */
public abstract class AbstractRepeater implements Repeater, Runnable {
    private static final Logger log = LoggerFactory.getLogger(AbstractRepeater.class);

    protected ApplicationContext applicationContext;
    protected ProxyServerContext proxyServerContext;
    protected Channel clientChannel;
    protected InetSocketAddress targetAddress;
    protected ChannelFuture targetChannelFuture;
    protected String channelId;

    // 状态 0、未建立连接  1、连接正常  2、连接断开  3、重连
    protected int state = 0;

    protected Queue<Object> toTargetQueue = new ConcurrentLinkedQueue<>();
    protected Queue<Object> toClientQueue = new ConcurrentLinkedQueue<>();

    protected boolean running = false;

    public AbstractRepeater(ApplicationContext applicationContext, Channel clientChannel, InetSocketAddress targetAddress) {
        this.applicationContext = applicationContext;
        this.clientChannel = clientChannel;
        this.targetAddress = targetAddress;
        this.proxyServerContext = applicationContext.getBean(ProxyServerContext.class);
        this.channelId = ProxyUtil.getChannelId(clientChannel);
    }

    @Override
    public void connect() {
        synchronized (this){
            if(this.running){
                return;
            }
            // 线程启动
            this.proxyServerContext.executeRelayThread(this);
            this.running = true;
        }
    }

    @Override
    public void relayToTarget(Object request) {
        this.toTargetQueue.add(request);
        this.notifyRunning();
    }

    @Override
    public void relayToClient(Object response) {
        this.toClientQueue.add(response);
        this.notifyRunning();
    }

    @Override
    public void clientDisconnect() {
        changeState(2);
        log.debug("{} 客户端连接断开,关闭连接.....channelId:{}  目标:{}", this.repeaterType(), channelId, targetAddress);
    }

    @Override
    public void targetDisconnect() {
        changeState(2);
        log.debug("{} 与目标主机连接断开,关闭连接....channelId:{}  目标:{}", this.repeaterType(), channelId, targetAddress);
    }

    @Override
    public void run() {
        while (true){
            try {
                if(state == 0 || state == 3){
                    // 连接 阻塞
                    this.targetChannelFuture = this.connectTarget();
                }else if(state == 1){
                    // 执行请求
                    Object request = this.toTargetQueue.poll();
                    Object response = this.toClientQueue.poll();
                    if(request != null){
                        this.relayRequestToTarget(request);
                    }
                    if(response != null){
                        this.relayResponseToClient(response);
                    }

                    if(request == null && response == null){
                        synchronized (this){
                            this.wait();
                        }
                    }
                }else if(state == 2){
                    // 退出
                    this.exit();
                    break;
                }
            } catch (Exception e) {
                log.warn("{} 转发异常.channelId:{}  目标:{}, 异常:{}", this.repeaterType(), channelId, targetAddress, e.getMessage(), e);
                changeState(2);
            }
        }
    }

    protected void exit(){
        // 清除缓存
        this.clearCache(this.clientChannel);

        ProxyUtil.closeChannel(this.clientChannel);
        if(this.targetChannelFuture != null) {
            ProxyUtil.closeChannel(targetChannelFuture.channel());
        }
    }

    protected void changeState(int state){
        this.state = state;
        this.notifyRunning();
    }

    protected void notifyRunning(){
        if(this.running){
            synchronized (this){
                this.notify();
            }
        }
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public ProxyServerContext getProxyServerContext() {
        return proxyServerContext;
    }

    public Channel getClientChannel() {
        return clientChannel;
    }

    public InetSocketAddress getTargetAddress() {
        return targetAddress;
    }

    public ChannelFuture getTargetChannelFuture() {
        return targetChannelFuture;
    }

    public String getChannelId() {
        return channelId;
    }

    public int getState() {
        return state;
    }

    protected abstract void clearCache(Channel clientChannel);
    protected abstract String repeaterType();
    protected abstract ChannelFuture connectTarget() throws Exception;
    protected abstract void relayRequestToTarget(Object request) throws Exception;
    protected abstract void relayResponseToClient(Object response) throws Exception;
}
