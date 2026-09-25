# Unit Testing Report

## New Test Cases and Rationale

Five unit tests were added in
`server/src/test/java/io/crate/expression/scalar/TimeZoneParserTest.java`.
They use JUnit 4 and AssertJ to test the time-zone parser directly.

| Test | What it checks and why |
|---|---|
| `test_null_timezone_is_rejected` | Passing `null` throws `IllegalArgumentException` with a clear message. This checks how the parser handles a missing value directly. |
| `test_out_of_range_offset_minutes_are_rejected` | `+02:60` is rejected with the expected error message. The minutes are numeric but outside the valid range. |
| `test_positive_offset_includes_minutes` | `+02:30` produces a fixed offset of 150 minutes ahead of UTC. This checks that the extra 30 minutes are included. |
| `test_negative_offset_applies_sign_to_minutes` | `-02:30` produces a fixed offset of 150 minutes behind UTC. This checks that the negative sign applies to both hours and minutes. |
| `test_non_numeric_offset_minutes_are_rejected` | `+02:xx` throws the expected validation error. This checks invalid text in the minutes field. |

Existing date-function tests exercise this parser indirectly, including
whole-hour offsets. These tests focus on the parser itself and its edge
cases, without starting a database.

## New Test Results

All five new tests passed. These results cover only `TimeZoneParserTest`,
not the full project test suite.

- Run date: September 24, 2026 (saved report timestamp).
- Test execution time: 0.825 seconds.
- Errors: 0.
- Skipped tests: 0.

Results were verified from the Maven Surefire report at
`server/target/surefire-reports/io.crate.expression.scalar.TimeZoneParserTest.txt`.
See [README.md](README.md) for instructions to reproduce the run.

### Tests Run

5

### Tests Passed

5

### Tests Failed

0

## Coverage Improvement

TODO
