# Mocking and Stubbing

## Component

`TransportHandshaker` was selected because it sends handshake messages over a
network channel. A real socket would make the tests slower and less reliable,
so the request sender is replaced with a Mockito mock.

The test double is:

```java
TransportHandshaker.HandshakeRequestSender requestSender
```

The real handshaker and an embedded channel are used, while the request sender
is mocked. This keeps the test focused on how `TransportHandshaker` responds
when the connection closes.

## New test cases and rationale

`TransportHandshakerMockStubTests` adds two channel-close scenarios that are
not covered by the existing `TransportHandshakerTests` file:

- `testHandshakeFailsWhenChannelCloses` closes the channel while a handshake
  is pending. It verifies that the failure is reported and the pending
  handshake is removed.
- `testHandshakeFailsIfChannelClosesWhileRequestIsSent` closes the channel
  during request dispatch. It covers the timing-specific case where the
  connection disappears while the request is being sent.

These cases represent realistic network failures without opening a real
socket.

## Mocking strategy

`TransportHandshakerTests` already tests normal handshake behavior and a sender
that throws an exception. The tests in
`TransportHandshakerMockStubTests` cover additional channel-close behavior.

The first test uses `mock(...)` to replace the request sender and `verify(...)`
to confirm that the handshake request was sent. The channel is then closed,
which verifies that a pending handshake fails and is removed.

The second test uses Mockito stubbing with `doAnswer(...).when(...)`. The stub
closes the embedded channel while `sendRequest(...)` is executing. This models
the timing-specific case where a network connection disappears during request
dispatch. The test verifies that the handshake reports the connection-reset
failure and does not remain pending.

## Running the tests

From the repository root, run:

```bash
./mvnw -T1 -pl server -am test \
  "-Dtest=TransportHandshakerMockStubTests" \
  "-Dsurefire.failIfNoSpecifiedTests=false"
```

The Surefire report is written to:

```text
server/target/surefire-reports/
```

## Coverage improvement analysis

The new tests increased coverage for the main `TransportHandshaker` class.
Instruction coverage increased from 88.71% to 90.86%, and line coverage
increased from 90.24% to 92.68%. Branch coverage remained at 80.00% because
the added tests exercised existing channel-close paths rather than adding a
new branch to the class.
