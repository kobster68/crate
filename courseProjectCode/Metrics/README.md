# CrateDB Quality Metrics

These collectors measure production Java line counts, comment density, test counts, and line coverage. The three CSV files in this folder are the results for the report.

## Before you start

Use JDK 26 (this checkout requests Temurin 26.0.2.1) and an internet connection for Maven to download dependencies. Set JAVA_HOME to your JDK folder and make sure Java is on PATH. Maven is included through the repository's `mvnw` wrapper.

Open PowerShell in the **crate repository root**, where `pom.xml` and `mvnw.cmd` are located. Run the following steps in order. On macOS/Linux, replace `.\mvnw.cmd` with `./mvnw`.

## 1. Collect maintainability metrics

```powershell
.\mvnw.cmd -pl courseProjectCode compile exec:java "-Dexec.mainClass=metrics.ProjectMetricsCollector" "-Dexec.args=." "-Dcheckstyle.skip"
```

This reads production Java files and writes:

- `courseProjectCode/Metrics/MetricsPerFile.csv`: code, comment, blank, and total lines per file.
- `courseProjectCode/Metrics/MetricsPerModule.csv`: line totals and comment density per module.

Comment density is comment-only lines divided by total physical lines, multiplied by 100. Files that JavaParser cannot parse use an approximate fallback; the collector prints a summary.

## 2. Run the project's tests

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' "-Dmaven.test.failure.ignore=true" clean test
```

This runs the project modules, excluding benchmarks and our collector. It removes previous build results first. Allow a few hours; our run took about 1 hour 46 minutes.

Surefire runs the tests and saves XML results in each module's `target/surefire-reports` folder. JaCoCo records which code the tests exercise. The command continues after test failures so other modules can produce results. **BUILD SUCCESS does not mean every test passed.** If compilation or setup fails, some modules may have incomplete or missing results.

## 3. Generate coverage reports

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' jacoco:report
```

JaCoCo converts the recorded coverage into reports, including each module's `target/site/jacoco/jacoco.xml`. Do not run `clean` between steps 2 and 3; it would delete the recorded data.

## 4. Collect testability metrics

```powershell
.\mvnw.cmd -pl courseProjectCode compile exec:java "-Dexec.mainClass=metrics.TestabilityCollector" "-Dexec.args=--all-modules ." "-Dcheckstyle.skip"
```

This reads the new XML reports and writes `courseProjectCode/Metrics/TestabilityPerModule.csv`. It records suites, tests, failures, errors, skipped tests, and line coverage for each selected module. It does not read older CSV files.

Line coverage is covered executable lines divided by covered plus missed executable lines, multiplied by 100. This is different from the physical line counts in the maintainability files.

## Reading the results

- Both collectors use the same fixed module list in `ProjectMetricsCollector.java`. Source code is in `courseProjectCode/src/main/java/metrics/`.
- Blank values mean the measurement is unavailable, not zero. `Reports read` means reports were found, not that all tests passed.
- Test counts come from the Surefire XML summary attributes. They include integration tests; repeated executions and flaky results are not counted separately.
- Our September 12, 2026 run produced reports for 19 of 25 modules. The CSV records 6 failures, 13 errors, and 107 skipped tests. Windows file/link handling, hostname resolution, and an Azure HTTP transport issue affected the run.
- These results came from checkout `f68eb0485e514478350b99d3ff08770bed4f6cfc` with the collector changes in this commit. Paths and test outcomes can differ on another machine. The committed CSVs preserve the collected results; rerunning overwrites them.
