# Mutation Testing Report

## Kobe's Mutation Testing Setup

The project did not previously have mutation testing configured. PIT 1.30.0 was added to `server/pom.xml` and configured to mutate `io.crate.expression.scalar.TimeZoneParser` using `TimeZoneParserTest`.

Reports are generated at `server/target/pit-reports/index.html`. Saved evidence is in:

- `evidence/pit-mutation-before/`
- `evidence/pit-mutation-after/`

### Mutation Score Results

| Measurement | Initial | Final |
|---|---:|---:|
| Line coverage | 83% (20/24) | 88% (21/24) |
| Mutation coverage | 50% (4/8) | 63% (5/8) |
| Test strength | 57% (4/7) | 71% (5/7) |

The final report shows improvement after adding tests for named time zones and positive offsets.

### Mutants Killed

The original tests caught incorrect behavior for null input, UTC, invalid offsets, and returned time zones. I added `test_named_timezone_is_resolved`, which catches errors in named time-zone handling. This increased mutation coverage from 50% to 63%.

### Mutants Surviving

Two changes were not detected, one affects the leading `+` sign, and the other affects the offset calculation. My positive-offset tests run these parts of the code, but PIT still reports both changes as undetected. Changing the `+` handling does not currently cause a test to fail.

## Michael's Mutation Testing Setup

extended the existing PIT setup with the `mutation-counttask` Maven profile in `server/pom.xml`.
This profile mutates `io.crate.execution.jobs.CountTask`and runs `CountTaskTest`.

Generated HTML reports are located in:
`server/target/pit-counttask-reports/index.html`.
XML reports are located in: 
`server/target/pit-counttask-reports/mutations.xml`.

Saved reports:

- [Initial report](evidence/pit-counttask-before/index.html)
- [Final report](evidence/pit-counttask-after/index.html)

### Mutation Score Results

| Measurement | Initial | Final |
|---|---:|---:|
| Line coverage | 97.2% (35/36) | 97.2% (35/36) |
| Mutation coverage | 84.6% (11/13) | 92.3% (12/13) |
| Test strength | 91.7% (11/12) | 100% (12/12) |

Mutation coverage and test strength include mutants detected
through timeouts.

### New Test and Rationale

`testAsyncCountFailureIsDeliveredToConsumer` verifies that an
asynchronous counting failure reaches the consumer.

The existing `testClose` checked that the task closed and its
completion future contained the exception, but did not check
whether the consumer actually received the failure.

The new test starts the task, completes the counting future
with an exception, and verifies that the consumer reports the same
exception within one second. This detects the mutant that removes
`consumer.accept(null, failure)`.

### Mutant Outcomes & Analysis

| Outcome | Initial | Final |
|---|---:|---:|
| Killed | 7 | 8 |
| Timed out | 4 | 4 |
| Survived | 1 | 0 |
| No coverage | 1 | 1 |

PIT generated 13 mutants in both runs. The mutant that removed the `consumer.accept(null,failure)` line
changed from SURVIVED to KILLED after the new test was added.

Mutation coverage improved by about 7.7 percentage points. Line coverage stayed unchanged because 
the new test added assertions for a failure case that was already tested by existing tests.
The remaining uncovered mutant replaces the return value of the `name()` function with an empty string.
None of the tests call this method. Therefore, final test strength is 100%, but the overall mutation coverage 
is 92.3%

## Group Contributions

### Kobe LaPrade

- Used previously tested `TimeZoneParser` for my mutation-testing target.
- Added PIT to maven.
- Added new tests for TimeZoneParser to cover mutants
- Ran the initial and final PIT reports.
- Saved before/after evidence

### Michael

- Used `CountTask` as my mustation testing target.
- Added a Maven profile `mutation-counttask` to the PIT configuration
- Added `testAsyncCountFailureIsDeliveredToConsumer` to help cover mutants
- Compared before and after reports, confirming that the surviving mutant was killed
