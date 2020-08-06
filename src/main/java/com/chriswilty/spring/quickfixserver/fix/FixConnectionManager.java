package com.chriswilty.spring.quickfixserver.fix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.MemoryStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.field.ApplVerID;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

@Component
public class FixConnectionManager implements Runnable {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SocketAcceptor socketAcceptor;

    @Autowired
    public FixConnectionManager(final FixApplication fixApplication) throws ConfigError, IOException {
        final SessionSettings sessionSettings = new SessionSettings(
                Objects.requireNonNull(
                        new ClassPathResource("session-acceptor.cfg").getInputStream(),
                        "FIX Session config file not found"
                )
        );
        socketAcceptor = new SocketAcceptor(
                fixApplication,
                new MemoryStoreFactory(),
                sessionSettings,
                new ScreenLogFactory(sessionSettings),
                new DefaultMessageFactory(ApplVerID.FIX44)
        );
    }

    @Override
    public void run() {
        try {
            log.info("Starting FIX acceptor session...");
            socketAcceptor.start();
            log.info("Press any key to close the session");

            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();

            log.info("Shutting down FIX connection...");
            socketAcceptor.stop(true);
            log.info("...Done");
        } catch (ConfigError ce) {
            log.error("Fatal error, could not start Acceptor:", ce);
        }
    }
}
