# Baseline Build and Test Report

## Environment

- Java: Eclipse Temurin JDK 26.0.2.1
- Build tool: Maven Wrapper with Maven 3.9.16

## Build Result

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' -DskipTests clean package
```

The build completed successfully for all 26 selected projects in 6 minutes. Tests were skipped during this build step and are recorded separately below.

## Test Suite Summary

Maven Surefire ran the existing unit and integration tests. No separate UI test suite was found.

- Tests run: 11,556
- Passed: 11,429
- Failed: 7
- Errors: 13
- Skipped: 107

Maven completed the full test run because test failures were allowed so that all modules could be tested.

## Coverage Summary

JaCoCo produced coverage reports for 19 of the 25 modules.

- Line coverage: 82.05%
- Branch coverage: 68.18%

## Observations

- Six modules did not produce test or coverage reports. Missing reports do not mean 0% coverage.
- Several failures were related to Windows file links, hostname resolution, and Azure test setup.
- Coverage varies between modules. The main `server` module had 85.04% line coverage.
- Detailed results are available in `testResults.html` and `testCoverage.html`.
