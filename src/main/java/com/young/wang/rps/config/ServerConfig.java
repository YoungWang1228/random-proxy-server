package com.young.wang.rps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by YoungWang on 2019-02-09.
 */
@Component
@ConfigurationProperties
public class ServerConfig {

    /**
     * 连接到目标主机超时时间
     */
    private Integer connectTimeoutMillis = 3000;

    /**
     * 代理服务器监听的端口
     */
    private Integer proxyPort = 9000;

    /**
     * nio selector 线程数
     */
    private Integer nioEventThreadNum = 8;

    /**
     * 接收器线程数
     */
    private Integer acceptorThreadNum = 8;

    /**
     * 转发器线程数
     */
    private Integer relayThreadNum = 20;

    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(Integer connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Integer getNioEventThreadNum() {
        return nioEventThreadNum;
    }

    public void setNioEventThreadNum(Integer nioEventThreadNum) {
        this.nioEventThreadNum = nioEventThreadNum;
    }

    public Integer getAcceptorThreadNum() {
        return acceptorThreadNum;
    }

    public void setAcceptorThreadNum(Integer acceptorThreadNum) {
        this.acceptorThreadNum = acceptorThreadNum;
    }

    public Integer getRelayThreadNum() {
        return relayThreadNum;
    }

    public void setRelayThreadNum(Integer relayThreadNum) {
        this.relayThreadNum = relayThreadNum;
    }
}
