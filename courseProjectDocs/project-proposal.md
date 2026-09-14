# Project Proposal: CrateDB Quality Analysis

## Project Overview

CrateDB is an open-source SQL database written in Java. It supports HTTP and PostGreSQL.

### Team Members

- Kobe LaPrade
- Michael Andrews

### Team Repository

https://github.com/kobster68/crate

### Project Goal

Evaluate CrateDB’s maintainability and testability using programmatically collected metrics.

## Key Quality Metrics

### Maintainability

- **Lines of Code (LOC):** Measure production Java code size per file and module.
- **Comment Density:** Calculate comment-only lines divided by total lines as a percentage.

Some Metrics we plan to collect in the future are: 
  - Cyclomatic complexity
  - Coupling / Cohesion between modules
  - Potentially outlining certain code smells

These metrics help identify large files and modules and compare how many comments there are.

### Testability

- **Unit Test Counts:** Count the test cases reported by the testing system.
- **Test Coverage:** Measure the percentage of production code lines tested.

These metrics help identify areas that may need more testing. Reported tests include both unit and integration tests.

### Collection Approach

Use Java to collect source metrics and export per-file and per-module results as CSV reports. The collector uses JavaParser to distinguish comments from code.

Collect test results and JaCoCo line coverage in `TestabilityPerModule.csv`. Further documentation on how to reproduce the results is in `courseProjectCode/Metrics/README.md`.

### Analysis

The current CSV reports contain 3,586 production Java files with 336,001 code lines. Comments make up about 22.71% of total lines.

The `server` module contains 294,706 code lines, about 87.71% of the collected code. Its size makes it an important area for further analysis.

`AstBuilder.java` has the most code lines in the per-file report, with 2,492. Its comment density is 2.11%, which is suprisingly low for such a large source file.

The testability report records 11,556 tests across 1,277 suites, including 6 failures, 13 errors, and 107 skipped tests. These results are somewhat influenced by limitations with the test enviroment.

Reported line coverage varies across modules. The `server` module has 84.99% coverage, while `libs/shared` has 22.85% and `libs/opendal` has 5.15%. The lower-coverage modules are possible areas for additional testing. Six modules have no test reports.

These findings come from `MetricsPerFile.csv`, `MetricsPerModule.csv`, and `TestabilityPerModule.csv`
