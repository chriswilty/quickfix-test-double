package com.chriswilty.spring.quickfixserver.fix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import quickfix.Application;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.MsgType;

@Service
public class FixApplication implements Application {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final MessageCracker messageCracker;

    @Autowired
    public FixApplication(final MessageHandler messageHandler) {
        messageCracker = new MessageCracker(messageHandler);
    }


    @Override
    public void onCreate(SessionID sessionID) {
        log.info("[{}] Session created", sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("[{}] Logon", sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.info("[{}] Logout", sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {}

    @Override
    public void fromAdmin(Message message, SessionID sessionID) {}

    @Override
    public void toApp(Message message, SessionID sessionID) {
        log.info("[{}] Message to App:", sessionID);
        log.info("{}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
        log.info("[{}] Message type {} from App", sessionID, message.getHeader().getField(new MsgType()).getValue());
        messageCracker.crack(message, sessionID);
    }
}
