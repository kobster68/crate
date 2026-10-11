# Mutation Testing

## For Kobe's Tests

Use JDK 26, then run the focused tests:

```bash
./mvnw -pl server '-Dtest=TimeZoneParserTest' test
```

Run PIT for `TimeZoneParser`:

```bash
./mvnw -pl server test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage
```

View the report at `server/target/pit-reports/index.html`.

## For Michael's Tests

PIT v1.30.0 is used, along with JDK 26, to mutation test `CountTask` using `CountTaskTest`.
The Maven profile `mutation-counttask` selects `CountTask` for mutation testing and uses `CountTaskTest` 
to detect the mutants.

run these commands from the root of the project:


prepare the server module first:

```powershell
./mvnw.cmd -pl server -am "-DskipTests" install
```

Run the unit tests for `CountTaskTest`:

```powershell
./mvnw.cmd -pl server '-Dtest=CountTaskTest' test
```

Run PIT for `CountTask`:

```powershell
./mvnw.cmd -pl server -Pmutation-counttask test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage
```

View the generated HTML report at 
`server/target/pit-counttask-reports/index.html`.

Mutation results are at
`server/target/pit-counttask-reports/mutations.xml`.

Saved reports for comparison:

- [Before the new test](evidence/pit-counttask-before/index.html)
- [After the new test](evidence/pit-counttask-after/index.html)

## Reproducing the before and after results

If you want to reproduce the baseline, comment out `testAsyncCountFailureIsDeliveredToConsumer`, including its @Test annotation, and run the PIT command.
The expected score is 11/13

When you un-comment the test and run the PIT command, the expected score is 12/13. The new test kills the mutant that was previously surviving.