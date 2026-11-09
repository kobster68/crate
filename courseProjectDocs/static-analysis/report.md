# SWEN-777: Software Quality Assurance

## Project Activity Report

**Activity:** Static Analysis & Code Smell Detection  
**Team:** Farm to Fridge (Team 3)

### Team Members
- Kobe LaPrade
- Michael Andrews

## Tool Used

PMD is a static analysis tool which we added to the root Maven build so it could analyze the CrateDB modules. PMD checks Java source code for possible bugs, unsafe patterns, and maintainability problems that may not be caught by normal compilation or tests.

The project already uses Checkstyle and Forbidden APIs through Maven. PMD added a different type of analysis by looking for code smells such as unused fields, unnecessary branches, and concurrency problems. Maven PMD Plugin 3.28.0 and PMD 7.28.0 were used because the project compiles with Java 26, which was initially a problem as the default PMD did not work correctly with Java 26.

## Analysis Scope and Process

PMD was first run only on the server module. Quickly, I determined the scope was too limited to provide useful findings. The PMD configuration was then moved to the root `pom.xml` so the analysis could run across the CrateDB modules.

The first project-wide attempt exposed two setup problems. PMD 7.17.0 could not read Java 26 class files, so it reported “unsupported class-file” errors. Later modules also could not resolve locally built CrateDB artifacts because the PMD goal does not install module JARs first.

These problems were fixed by overriding the PMD runtime with PMD 7.28.0 and installing the Maven artifacts with tests and Forbidden APIs checks skipped. The install reached all CrateDB modules but stopped at the separate “Course-Project-Code module”, which you may recognize as not really a module but our own documentation. This was excluded from the final PMD scan in order to avoid these errors.

The final scan used:

    ./mvnw -T1 -fae -pl '!courseProjectCode' pmd:pmd

The scan completed successfully across 26 CrateDB modules. The reports are saved in `evidence/pmd-full-project-before/`, with the shared HTML resources in `evidence/pmd-assets/`. The updated server report focused on the fix is saved in `evidence/pmd-server-after/`.

## Key Findings

### Kobe - TableElementsAnalyzer

The server report identified several types of problems found by PMD:

- **DoubleCheckedLocking:** PMD identified double-checked locking patterns that are not thread-safe.
- **AvoidBranchingStatementAsLastInLoop:** PMD identifies branches like ‘continue’ or ‘return’ at the end of loops.
- **UnusedPrivateField:** PMD identified a private field that was assigned but never read.
- **UnnecessaryImport:** PMD identified an import that became unused.

For the fix, I decided to choose one of the UnusedPrivateFields.

### Michael - AllRangeOperator

PMD reported two UnusedPrivateField warnings in `AllRangeOperator.Comparison`: `innerOpName` and `inverseInnerOpName`.

Both fields were assigned values in the constructor but were never read. They added extra code without affecting how the comparisons worked.

## Fix Summary

### Kobe - TableElementsAnalyzer

The unused field finding in `TableElementsAnalyzer.java` was selected because it was small and low risk. It seemed to be some leftover code from an old refactor. The `RefBuilder` class contained a `columnPolicy` field that was assigned but never read, so the field and its two assignments were removed.

Removing the field made the `io.crate.sql.tree.ColumnPolicy` import unused, so I removed that as well. Checkstyle passed after the change, and the final server PMD scan completed successfully without the unused-field or unused-import finding for this code.

A focused run of the tests successfully passed, confirming that the code still works.

### Michael - AllRangeOperator

I removed both unused fields and their assignments. I also removed the `inverseComparison` parameter and the second arguments from all four enum declarations, since that parameter was only used to set one of the removed fields. The comparison methods and operator names stayed the same.

I compiled the server module before each PMD scan and used the same settings for both runs. After the cleanup:

- Server PMD findings decreased from 798 to 796
- Both unused-field warnings were gone
- All 15 tests in `AllOperatorTest` passed

## Group Contributions

### Kobe LaPrade

Added and configured PMD, ran the project-wide scan, saved the before and after evidence, reviewed the findings, and fixed the unused `columnPolicy` field and import in `TableElementsAnalyzer`.

### Michael Andrews

Ran PMD on the server module and reviewed the findings for `AllRangeOperator`. Removed the unused fields and constructor parameter, checked the fix with PMD and ran `AllOperatorTest`, and then documented the results.