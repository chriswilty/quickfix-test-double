package com.chriswilty.spring.quickfixserver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FieldNotFound;
import quickfix.MemoryStoreFactory;
import quickfix.Message;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.ApplVerID;
import quickfix.field.CFICode;
import quickfix.field.MsgType;
import quickfix.field.OrderQty;
import quickfix.field.QuoteReqID;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.fix44.Quote;
import quickfix.fix44.QuoteCancel;
import quickfix.fix44.QuoteRequest;
import quickfix.fix44.QuoteRequestReject;
import quickfix.fix44.component.Instrument;
import quickfix.fix44.component.OrderQtyData;

import java.net.ConnectException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class QuickfixServerApplicationTests {

	// TODO ?? Start the main app itself? Maybe hit an endpoint to start the FIX acceptor?

	private static final Logger log = LoggerFactory.getLogger("FIX Session Tests");

	private static final Object initiatorMonitor = new Object();
	private static final Runnable onLogin = () -> {
		synchronized (initiatorMonitor) {
			initiatorMonitor.notifyAll();
		}
	};

	private static final List<Message> messages = new LinkedList<>();
	private static final Object messageMonitor = new Object();
	private static final Consumer<Message> onMessage = message -> {
		try {
			final MsgType messageType = (MsgType) message.getHeader().getField(new MsgType());
			log.info("Message {} received", messageType.getValue());
			if (messageType.valueEquals(QuoteRequestReject.MSGTYPE) || messageType.valueEquals(QuoteCancel.MSGTYPE)) {
				synchronized (messageMonitor) {
					messageMonitor.notifyAll();
				}
			} else {
				messages.add(message);
			}
		} catch (FieldNotFound ffs) {
			log.error("What fresh hell is this...", ffs);
		}
	};

	private static SocketInitiator socketInitiator;
	private static SessionID sessionID;

	@BeforeAll
	static void initiateFixSession() throws Exception {
		final Application loggingFixApplication = new LoggingFixApplication("Initiator", onLogin, onMessage);
		final SessionSettings sessionSettings = new SessionSettings(
				Objects.requireNonNull(
						new ClassPathResource("session-initiator.cfg").getInputStream(),
						"FIX Session config file not found"
				)
		);
		sessionID = sessionSettings.sectionIterator().next();

		socketInitiator = new SocketInitiator(
				loggingFixApplication,
				new MemoryStoreFactory(),
				sessionSettings,
				new ScreenLogFactory(sessionSettings),
				new DefaultMessageFactory(ApplVerID.FIX44)
		);

		synchronized (initiatorMonitor) {
			try {
				socketInitiator.start();
				initiatorMonitor.wait(5000);
				log.info("Initiator login: socket is ready");
			} catch (InterruptedException e) {
				log.warn("Initiator login: unexpected interrupt!");
			}
		}
		if (!socketInitiator.isLoggedOn()) {
			throw new ConnectException("Timed out waiting for Initiator login");
		}
	}

	@AfterAll
	static void terminateFixSession() {
		socketInitiator.stop(true);
	}

	@AfterEach
	void reset() {
		messages.clear();
	}

	@Test
	void sendQuoteRequestAndExpectQuotes() throws Exception {
		final String requestId = "FQR-001";
		final QuoteRequest quoteRequest = generateQuoteRequest(requestId);

		synchronized (messageMonitor) {
			Session.sendToTarget(quoteRequest, sessionID);
			try {
				messageMonitor.wait(8000);
			} catch (InterruptedException ie) {
				log.warn("Message await: unexpected interrupt!");
			}
		}

		assertThat(messages).hasSize(5);
		for (Message message : messages) {
			assertThat(message).isInstanceOf(Quote.class);
			assertThat(((Quote) message).getQuoteReqID().getValue()).isEqualTo(requestId);
		}
		// TODO Send QuoteCancel to stop quotes?
	}

	private QuoteRequest generateQuoteRequest(final String requestId) {
		final QuoteRequest quoteRequest = new QuoteRequest(new QuoteReqID(requestId));

		final QuoteRequest.NoRelatedSym noRelatedSymGroup = new QuoteRequest.NoRelatedSym();
		noRelatedSymGroup.set(new Side(Side.BUY));

		final Instrument instrument = new Instrument(new Symbol("EMB"));
		instrument.set(new CFICode("FFWCS"));
		noRelatedSymGroup.set(instrument);

		final OrderQtyData orderQtyData = new OrderQtyData();
		orderQtyData.set(new OrderQty(1000.00));
		noRelatedSymGroup.set(orderQtyData);

		quoteRequest.addGroup(noRelatedSymGroup);
		return quoteRequest;
	}

}
