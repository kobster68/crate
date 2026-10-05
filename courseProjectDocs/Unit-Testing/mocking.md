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


## Michael - CountTask


### Component

I chose to test the `CountTask` class because it delegates counting to CountOperation and handles the resulting success, failire, or cancellation. 
CountOperation performs counting across index shards, making it a good candidate dependency to mock in a unit test. 

The tests use a real CountTask and TestingRowConsumer, while CountOperation is replced with a Mockito Mock. This allows the tests to control
counting outcomes so that they aren't dependent on the actual counting logic, which is tested separately in CountOperationTests. It also
removes the need to set up any real index shards, which would make the tests slower and more complex. 


### New test cases and rationale.


I added three test methods to the existing `CountTaskTest` class

- **`testCountFailureBeforeFutureIsReturned`**  
  The mocked `CountOperation.count()` throws an exception before returning
  a future. The test verifies that the consumer receives the exact original
  exception. This covers the immediate-failure catch block, while the
  existing failure test covers the case where the future is returned and then fails.
  
- **`testKillBeforeStartedDoesNotCount`**  
  The task is killed before starting, followed by an attempt to start it.
  The test verifies that the consumer receives the expected
  `JobKilledException` and that the mocked counting dependency has no
  interactions. A cancelled task should report its failure and avoid
  performing unnecessary work. The existing cancellation test covers
  killing a task after counting has already started, therefore adding
  a useful boundary case.

- **`testZeroCountProducesOneResultRow`**  
  The mocked counting operation returns a successfully completed future
  containing `0L`. The test verifies that the consumer receives exactly
  one row containing that value. Zero is a valid counting result and
  should produce a result row. The existing success test checks task 
  closure and memory accounting but does not assert the returned row. 
  This test adds a check that a valid count of zero produces 
  exactly one row containing 0L.
  

### Mocking strategy

The tests create a Mockito mock of `CountOperation` and pass it into the
real `CountTask` through its constructor. This isolates task behavior
from the index-shard setup and counting logic used by the real dependency.

The `CountTask` and `TestingRowConsumer` remain real, so the assertions
check how the task delivers results and failures to its consumer.
The `CompletableFuture` used in the zero-count test is also real.

Each test uses the mock differently:

- The immediate failure test uses `when(...).thenThrow(error)` to make
  `count()` throw before returning a future. Since no future is returned,
  the `whenComplete(...)` callback is never registered, and the task should
  immediately report the exception to its consumer.

- The cancellation-before-start test does not stub `count()`, because
  counting should never begin. After killing the task and attempting
  to start it, `verifyNoInteractions(countOperation)` checks that the
  task made no calls to its counting dependency.

- The zero-count test uses `when(...).thenReturn(future)` with
  `CompletableFuture.completedFuture(0L)`. This provides a predictable
  successful result and shows the task's behavior when the count is zero. 
  The test verifies that the consumer receives exactly one row containing
  `0L`, the expected result for a count of zero.
  

### Test results


| Run | Tests run | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Baseline: two original methods | 2 | 2 | 0 | 0 | 0 |
| Updated: all CountTask methods | 5 | 5 | 0 | 0 | 0 |

The updated run includes the two original methods and all three newly
added methods. All three new tests also passed when run individually.
The project's Checkstyle checks passed.

Saved results:
- [Baseline Surefire report](evidence/counttask-before/surefire/io.crate.execution.jobs.CountTaskTest.txt)
- [Updated Surefire report](evidence/counttask-after/surefire/io.crate.execution.jobs.CountTaskTest.txt)

See the [README](README.md) for test and coverage reproduction instructions.


### Coverage improvement analysis

The baseline ran only the two original CountTask test methods. The updated
run included those methods and the three new tests. Both runs used identical
production code and the same Java version. Fresh JaCoCo execution data was
used for each run.

| Metric | Baseline | After | Improvement |
|---|---:|---:|---:|
| Lines | 30/35 (85.71%) | 34/35 (97.14%) | +11.43 percentage points |
| Branch outcomes | 7/8 (87.50%) | 8/8 (100.00%) | +12.50 percentage points |
| Instructions | 122/140 (87.14%) | 136/140 (97.14%) | +10.00 percentage points |

Together, the new tests cover four additional executable lines and the
remaining branch outcome. The zero-count doesn't necessarily increase
line or branch coverage, but it does verify that a valid count of zero 
produces exactly one result row containing 0L, which is a useful
boundary case that was not previously tested.

The remaining uncovered line is the return statement in `name()`.

Saved coverage evidence:
- [Baseline JaCoCo CSV](evidence/counttask-before/jacoco.csv)
- [Updated JaCoCo CSV](evidence/counttask-after/jacoco.csv)
- [Baseline JaCoCo XML](evidence/counttask-before/jacoco.xml)
- [Updated JaCoCo XML](evidence/counttask-after/jacoco.xml)

See the [README](README.md) to reproduce both runs and open the HTML reports.