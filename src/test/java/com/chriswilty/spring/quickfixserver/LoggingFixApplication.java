package com.chriswilty.spring.quickfixserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.Message;
import quickfix.SessionID;

import java.util.function.Consumer;

public class LoggingFixApplication implements Application {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String sessionName;
    private final Runnable onLogon;
    private final Consumer<Message> onMessage;

    public LoggingFixApplication(final String type, final Runnable loginCallback, final Consumer<Message> messageCallback) {
        sessionName = type;
        onLogon = loginCallback;
        onMessage = messageCallback;
    }

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("{} onCreate({})", sessionName, sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        log.info("{} onLogon({})", sessionName, sessionId);
        onLogon.run();
    }

    @Override
    public void onLogout(SessionID sessionId) {
        log.info("{} onLogout({})", sessionName, sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.info("{} toAdmin({}, {})", sessionName, message.getClass().getSimpleName(), sessionId);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        log.info("{} fromAdmin({}, {})", sessionName, message.getClass().getSimpleName(), sessionId);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        log.info("{} toApp({}, {})", sessionName, message.getClass().getSimpleName(), sessionId);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) {
        log.info("{} fromApp({}, {})", sessionName, message.getClass().getSimpleName(), sessionId);
        onMessage.accept(message);
    }
}
