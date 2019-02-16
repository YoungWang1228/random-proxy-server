package com.young.wang.rps.proxy.relay.repeater;

/**
 * Created by YoungWang on 2019-02-15.
 */
public interface Repeater {
    void connect();
    void relayToTarget(Object request);
    void relayToClient(Object response);
    void clientDisconnect();
    void targetDisconnect();
}
