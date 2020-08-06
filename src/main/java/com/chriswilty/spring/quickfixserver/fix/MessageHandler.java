package com.chriswilty.spring.quickfixserver.fix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.MessageCracker.Handler;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.QuoteID;
import quickfix.field.QuoteRequestRejectReason;
import quickfix.field.ValidUntilTime;
import quickfix.fix44.Message;
import quickfix.fix44.QuoteRequest;
import quickfix.fix44.QuoteRequestReject;
import quickfix.fix44.component.OrderQtyData;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class MessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Random random = new Random();
    private final AtomicInteger quoteNumber = new AtomicInteger(0);

    @Handler
    public void handleQuoteRequest(final QuoteRequest request, final SessionID sessionID) {
        log.info("[{}] Received Quote Request:\n{}", sessionID, request);

        // For now, just send a few quotes then reject.
        for (int i = 0; i < 5; i++) {
            sendOnTick(() -> generateQuote(request), sessionID);
        }
        sendOnTick(() -> generateQuoteRequestReject(request), sessionID);
    }

    private Optional<Message> generateQuote(final QuoteRequest quoteRequest) {
        try {
            final quickfix.fix44.Quote quote = new quickfix.fix44.Quote(
                    new QuoteID(String.format("quote-%04d", quoteNumber.incrementAndGet()))
            );
            quote.set(quoteRequest.getQuoteReqID());
            quote.set(new OrdType(OrdType.PREVIOUSLY_QUOTED));
            quote.set(new ValidUntilTime(LocalDateTime.now().plusMinutes(1)));

            final QuoteRequest.NoRelatedSym noRelatedSymGroup = (QuoteRequest.NoRelatedSym) quoteRequest.getGroup(1, new QuoteRequest.NoRelatedSym());
            quote.set(noRelatedSymGroup.getInstrument());
            quote.set(noRelatedSymGroup.getSide());

            final double orderQty = noRelatedSymGroup.getOrderQtyData().getOrderQty().getValue();
            final OrderQtyData orderQtyData = new OrderQtyData();
            orderQtyData.set(new OrderQty(wobble(orderQty)));
            quote.set(orderQtyData);

            return Optional.of(quote);
        } catch (FieldNotFound ffs) {
            logUnexpectedException(ffs);
            return Optional.empty();
        }
    }

    private Optional<Message> generateQuoteRequestReject(final QuoteRequest quoteRequest) {
        try {
            final QuoteRequestReject quoteRequestReject = new QuoteRequestReject(
                    quoteRequest.getQuoteReqID(),
                    new QuoteRequestRejectReason(QuoteRequestRejectReason.EXCHANGE_CLOSED)
            );
            quoteRequestReject.addGroup(quoteRequest.getGroup(1, new QuoteRequest.NoRelatedSym()));

            return Optional.of(quoteRequestReject);
        } catch (FieldNotFound ffs) {
            logUnexpectedException(ffs);
            return Optional.empty();
        }
    }

    private void sendOnTick(final Supplier<Optional<Message>> messageSupplier, final SessionID sessionID) {
        messageSupplier.get().ifPresent(message -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Timeout unexpectedly interrupted", e);
            }

            try {
                Session.sendToTarget(message, sessionID);
            } catch (SessionNotFound ffs) {
                logUnexpectedException(ffs);
            }
        });
    }

    private double wobble(final double input) {
        return input + random.nextGaussian();
    }

    private void logUnexpectedException(final Exception exception) {
        log.error("Well fuckity, that was unexpected :(", exception);
    }
}
