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

## Run Only My New Tests

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
