# Integration Testing

## Setup

Use Java JDK 26.0.2.1 and set `JAVA_HOME` to that JDK's folder. Open PowerShell in the project root, where `mvnw.cmd` is located. Maven is included through the wrapper.

If the server dependencies have not been built and installed locally, run this setup command first. Maven needs an internet connection to download any missing dependencies.

```powershell
.\mvnw.cmd -T1 -pl server -am "-DskipTests" install
```

## Run Kobe's Test

[FilteredSelectIntegrationTest.java](../../server/src/test/java/io/crate/integrationtests/FilteredSelectIntegrationTest.java) creates a table, inserts sample students, filters for SWEN-777, and verifies the ordered results from the real CrateDB integration-test environment.

```powershell
.\mvnw.cmd -B -T1 -pl server "-Dtest=FilteredSelectIntegrationTest" test
```

Expected results are 1 test passed, 0 failures, 0 errors, and 0 skipped.

## Run Michael's Test and the Baseline Checks

The test `test_count_star_with_bound_filter_argument` in [RestSQLActionIntegrationTest.java](../../server/src/test/java/io/crate/integrationtests/RestSQLActionIntegrationTest.java) sends a parameterized `COUNT(*)` query through CrateDB's HTTP interface. It sends two different names in the JSON arguments and checks that each request returns the correct count.

```powershell
.\mvnw.cmd -B -T1 -pl server "-Dtest=RestSQLActionIntegrationTest#test_count_star_with_bound_filter_argument+test_sets_type_hints_based_on_params,CountStarIntegrationTest" test
```

This runs Michael's new test and the five existing baseline checks. Expected results are 6 tests passed, 0 failures, 0 errors, and 0 skipped. This is the test selection used for Michael's individual results.

## Run Both Contributions and the Baseline Checks

```powershell
.\mvnw.cmd -B -T1 -pl server "-Dtest=FilteredSelectIntegrationTest,RestSQLActionIntegrationTest#test_count_star_with_bound_filter_argument+test_sets_type_hints_based_on_params,CountStarIntegrationTest" test
```

This runs both new tests and the five existing checks. Expected results are 7 tests passed, 0 failures, 0 errors, and 0 skipped.

## Results and Reports

The detailed results are saved in these files for the tests selected in each run.

```text
server/target/surefire-reports/io.crate.integrationtests.FilteredSelectIntegrationTest.txt
server/target/surefire-reports/io.crate.integrationtests.RestSQLActionIntegrationTest.txt
server/target/surefire-reports/io.crate.integrationtests.CountStarIntegrationTest.txt
```

Kobe's report is [report.md](report.md). Michael's report is [report-michael.md](report-michael.md).
