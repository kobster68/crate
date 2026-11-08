# Static Analysis Report

## Tool used

We used PMD 7.28.0 through the Maven PMD plugin 3.28.0.
PMD checks Java source code for possible problems, including unused code.

For my contribution, I checked the server module before and after
the cleanup using JDK 26.0.2.1.

## Key findings

### Michael - AllRangeOperator

PMD reported two `UnusedPrivateField` warnings in
`AllRangeOperator.Comparison`: `innerOpName` and `inverseInnerOpName`.

Both fields were assigned values in the constructor but were never read.
They added extra fields and setup code without contributing to the
comparison behavior. This was a code smell because it left unnecessary
code for someone maintaining the class to understand.

## Fix summary

### Michael - AllRangeOperator

I removed the two unused fields and their constructor assignments.
I also removed the unused `inverseComparison` constructor parameter
and its argument from the four enum declarations.

The comparison methods and operator names were unchanged.

After the cleanup:
- Server PMD findings decreased from 798 to 796.
- Both targeted `UnusedPrivateField` warnings were gone.
- All 15 tests in `AllOperatorTest` passed.
- Checkstyle reported zero violations.

## Group Contributions

### Kobe LaPrade

- Added PMD to the Maven build.
- Removed the unused `columnPolicy` field and its assignments from
  `TableElementsAnalyzer`.
- Saved before and after PMD reports.

### Michael Andrews

- Reviewed the PMD findings for `AllRangeOperator`.
- Removed two unused fields and the unused constructor parameter.
- Verified the cleanup with PMD, `AllOperatorTest`, and Checkstyle.
- Documented the finding, fix, and results.
