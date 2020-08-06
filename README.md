# Simple FIX server (Acceptor) using QuickFIX/J

A proof-of-concept to reacquaint myself with QuickFIX, using SpringBoot with CommandLineRunner (cos
why not).

## Build it

```
./mvnw clean compile
```

## Run it

```
./mvnw spring-boot:run
```

## Test it

Tests require that the FIX Acceptor is running (as above).

A FIX Initiator is started to communicate with the acceptor; the initiator waits for Login, then
sends a QuoteRequest and processes all Quote messages returned, until a QuoteRequestReject message
is received.

```
./mvnw test
```

## Future work

- Add more message processing for a more complete "Test Double", including e.g. trigger values in
QuoteRequests to force certain behaviour, such as immediate QuoteRequestReject, simulating Exchange
closed, etc.
- For more control, could trigger Acceptor start and stop by sending a web request, instead of using
the CommandLineRunner.
