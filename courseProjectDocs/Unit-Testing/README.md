# How to Run the Tests

## Before You Start

Install Java JDK 26.0.2.1 and set `JAVA_HOME` to its folder.
You need an internet connection for Maven to download dependencies.

Open PowerShell in the project's main folder, containing `mvnw.cmd`.

Check Java:

```powershell
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

The version should show Java 26. Maven is included through the wrapper.
The commands below compile the code before running tests.

Before running the server tests, build and install the server and its required library modules. 
This also builds the shared test-support classes. Tests are skipped during this setup step.

```powershell
.\mvnw.cmd -T1 -pl server -am "-DskipTests" install
```

## Run TimeZoneParser Tests

```powershell
.\mvnw.cmd -T1 -pl server -am test "-Dtest=TimeZoneParserTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

This runs the five tests in `TimeZoneParserTest.java`.

The command also builds the modules needed by `server`. The
`failIfNoSpecifiedTests` setting lets those modules continue even though
they do not contain our test class.

Look for the line showing tests run, failures, errors, and skipped tests.
The detailed results are saved in:

```text
server/target/surefire-reports/io.crate.expression.scalar.TimeZoneParserTest.txt
```

## Run Hex Tests

From the repository root, run:

```powershell
.\mvnw.cmd -T1 -pl libs/shared -am test "-Dtest=HexTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```
This runs the seven tests in `HexTest.java`. Expected results:
7 tests run, 0 failures, 0 errors, and 0 skipped.

The detailed results are saved in:

```text
libs/shared/target/surefire-reports/io.crate.common.HexTest.txt
```

## Run Mocking and Stubbing Tests

From the repository root:

```bash
./mvnw -T1 -pl server -am test \
  "-Dtest=TransportHandshakerMockStubTests" \
  "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected result: 2 tests passed. Results are saved in
`server/target/surefire-reports/`.


## Run CountTask Tests

From the repository root, after completing the server dependency setup run: 

```powershell
.\mvnw.cmd -T1 -pl server test "-Dtest=CountTaskTest"
```

This runs all five methods in `CountTaskTest.java`: two existing tests
and three new tests covering immediate counting failure, cancellation
before starting, and a successful count of zero.

Expected results: 5 tests run, 0 failures, 0 errors, and 0 skipped.

The detailed results are saved in:

```text
server/target/surefire-reports/io.crate.execution.jobs.CountTaskTest.txt
```

## Reproduce CountTask Baseline Coverage

From the repository root, after completing the server dependency setup, run:

```powershell
.\mvnw.cmd -T1 -pl server test jacoco:report "-Dtest=CountTaskTest#testClose+testKillOperationFuture" "-Djacoco.append=false"
```

This runs only the two original CountTask tests, excluding the three new
methods without removing their source code.

Expected results: 2 tests run, 0 failures, 0 errors, and 0 skipped.

The `jacoco.append=false` setting starts fresh coverage data so previous
test runs do not affect this measurement.

Open the generated CountTask coverage report:

```text
server/target/site/jacoco/io.crate.execution.jobs/CountTask.html
```

Save the coverage report and the CountTaskTest Surefire results before
running the after-coverage command, which will overwrite these reports.


## Generate CountTask Coverage After Adding Tests

From the repository root, after completing the server dependency setup, run:

```powershell
.\mvnw.cmd -T1 -pl server test jacoco:report "-Dtest=CountTaskTest" "-Djacoco.append=false"
```

This runs all five CountTask tests: the two original methods and
the three new methods.

Expected results: 5 tests run, 0 failures, 0 errors, and 0 skipped.

The command generates fresh coverage data for comparison with the
CountTask baseline. Open:

```text
server/target/site/jacoco/io.crate.execution.jobs/CountTask.html
```

Use the same source revision and Java version for both runs.
Compare the CountTask class coverage in this report with the saved
baseline report.

## Reproduce Shared-Module Coverage Before HexTest

To run the existing shared-module tests while excluding HexTest:

```powershell
.\mvnw.cmd -T1 -pl libs/shared -am clean test jacoco:report "-Dtest=!HexTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected results: 62 tests run, 0 failures, 0 errors, and 0 skipped.

The `!HexTest` filter excludes the seven new tests without removing
their source file. This reproduces the original test selection.

Save the generated reports before running the next section:
its `clean` step will delete them.

Use the same source revision and Java version for both runs so
the comparison measures the effect of adding HexTest.

## Run Shared-Module Tests and Generate Coverage 

From the repository root, run:

```powershell
.\mvnw.cmd -T1 -pl libs/shared -am clean test jacoco:report
```

This runs all tests in `libs/shared`, including the seven Hex tests.
Expected results: 69 tests run, 0 failures, 0 errors, and 0 skipped.

The clean step removes previous build results and coverage data.
Save any existing reports you need before running it.

Open `libs/shared/target/site/jacoco/index.html` to view module coverage.
For Hex coverage, open
`libs/shared/target/site/jacoco/io.crate.common/Hex.html`.

## Run the Full Baseline Test Suite

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' "-Dmaven.test.failure.ignore=true" test
```

This runs the existing tests and our new tests. It uses the same module
selection as the baseline: `benchmarks` and `courseProjectCode` are excluded.
It can take several hours.

The command continues if tests fail, so a Maven success message does not
mean all tests passed. Check the results in each module's
`target/surefire-reports` folder.

Our test file includes the same license header as the existing CrateDB
source files. The project's Checkstyle checks require this header, so it
was added to prevent a missing-header error before the tests run.
Both commands keep Checkstyle enabled.

## Create Coverage Reports

After running the full suite, run:

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' jacoco:report
```

Open `server/target/site/jacoco/index.html` to view server coverage.
Other modules with coverage data have reports in the same location
inside their own folders.
