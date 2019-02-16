package com.young.wang.rps.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Created by YoungWang on 2019-02-09.
 */
@Component
public class MainRunner implements CommandLineRunner{
    private static final Logger log = LoggerFactory.getLogger(MainRunner.class);
    private final ProxyServer proxyServer;

    public MainRunner(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void run(String... strings) throws Exception {

        proxyServer.start();

    }

}
