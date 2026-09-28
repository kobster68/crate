# Unit Testing Report

## New Test Cases and Rationale

### Kobe LaPrade — TimeZoneParser

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

### Kobe's recorded TimeZoneParser results

All five new tests passed. These results cover only `TimeZoneParserTest`,
not the full project test suite.

- Run date: September 24, 2026 (saved report timestamp).
- Test execution time: 0.825 seconds.
- Errors: 0.
- Skipped tests: 0.

Results were verified from the Maven Surefire report at
`server/target/surefire-reports/io.crate.expression.scalar.TimeZoneParserTest.txt`.
See [README.md](README.md) for instructions to reproduce the run.

#### Tests Run

5

#### Tests Passed

5

#### Tests Failed

0

### Coverage Comparison

These tests add direct coverage for cases that were previously tested only indirectly.

The null test checks that a missing time zone is rejected. The positive and negative offset tests check the HH:MM format in both directions, including minutes and negative values. The last two tests check invalid input: one uses an impossible minute value, and the other uses nonnumeric minutes. Both verify that the parser returns the expected error.

The null test increased coverage because it reaches the parser’s dedicated null-check branch. The positive and negative tests add coverage for minute parsing and negative-offset handling. The invalid-input tests re-use the same exception-handling code, but they protect different validation cases.

## Michael - Hex Tests

Seven unit tests were added in
`libs/shared/src/test/java/io/crate/common/HexTest.java` using JUnit Jupiter
and AssertJ. They call the utility Hex.java directly, without a database, network connection, or mocks.
Hex.java is a utility responsible for hexadecimal encoding, decoding, and validation. It already had
indirect coverage through server tests, but it lacked dedicated tests in `libs/shared`. These additions provide
dedicated `shared` module tests for encoding options, boundary values, and error handling.

| Test | Expected result and rationale |
|---|---|
| `testLowercaseEncodingPreservesByteBoundaries` | Bytes `0, 127, -128, -1` encode to `007f80ff`. These values check zero, the largest positive byte, the smallest negative byte, and a byte with all its bits set. This checks that signed bytes are handled correctly  using the default lowercase alphabet. |
| `testUppercaseEncoding` | The same bytes encode to `007F80FF` when uppercase is used. Checks the alternate output alphabet without changing the byte values. This would check if the uppercase option was somehow ignored. |
| `testDecodeMixedCase` | `aB01` decodes to bytes `-85, 1`. Checks that uppercase and lowercase digits can appear together and that the decoded bytes keep their order, thanks to the explicit expected array. |
| `testDecodeEmptyInput` | An empty string produces an empty byte array. Checks the empty string boundary value. The decoding loop should execute zero times and return without an exception or any extra bytes.  |
| `testDecodeRejectsOddLength` | `abc` throws exactly `IllegalStateException` with `Odd number of characters.` Each byte requires two hexadecimal digits, so an odd length input means an incomplete pair. All three are valid hex digits, which isolates the length validation.  |
| `testDecodeReportsInvalidCharacterPosition` | `0g` throws `IllegalStateException` identifying `g` at index 1. Checks the rejection of a byte with an invalid digit. This checks both rejection of invalid digits and whether the error correctly identifies the offending character and position. |
| `testStripHexFlagReportsOriginalCharacterPosition` | A value with the prefix `\x` followed by `0g` reports `g` at index 3. Checks invalid-digit rejection and position again but this time after removing the two character prefix, which checks for potential offset errors. |

### Test results

Run date: September 27, 2026.

| Scope | Suites | Run | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|---:|
| `libs/shared`, before HexTest | 10 | 62 | 62 | 0 | 0 | 0 |
| `libs/shared`, after HexTest | 11 | 69 | 69 | 0 | 0 | 0 |
| New `HexTest` cases, included in the after run | 1 | 7 | 7 | 0 | 0 | 0 |

The new test class took 0.019 seconds according to Surefire. The seven tests are included in the 69.

Evidence: [before test counts](evidence/hex-before/test-counts.csv),
[after test counts](evidence/hex-after/test-counts.csv), and the
[HexTest Surefire report](evidence/hex-after/surefire/io.crate.common.HexTest.txt).
Each evidence directory also contains the Maven log and the individual
Surefire text summaries.

## Coverage Improvement

### Controlled before/after comparison for Hex Tests

The before run happened prior
to creating `HexTest.java`. The after run included its seven tests. Production source and build configuration were identical.

Both runs used the same command from the repository root:

```powershell
.\mvnw.cmd -B -T1 -pl libs/shared -am clean test jacoco:report
```

`clean` removed previous execution data and test reports before each run.
JaCoCo reports were generated immediately afterward, without another clean.
The before evidence was taken out of `target` before the after run.

| metric | Before covered / total | Before | After covered / total | After | Change (percentage points) |
|---|---:|---:|---:|---:|---:|
| Hex lines | 0 / 37 | 0.00% | 30 / 37 | 81.08% | +81.08 |
| Hex branches | 0 / 16 | 0.00% | 12 / 16 | 75.00% | +75.00 |
| libs/shared lines | 366 / 1,602 | 22.85% | 396 / 1,602 | 24.72% | +1.87 |
| libs/shared branches | 165 / 768 | 21.48% | 177 / 768 | 23.05% | +1.56 |

Percentages are covered / (covered + missed) times 100. The seven tests exercise 30 additional lines
and 12 additional branches. Comparing the per-class CSV rows shows that
only Hex's coverage changed.

Saved reports: [before JaCoCo CSV](evidence/hex-before/jacoco.csv),
[after JaCoCo CSV](evidence/hex-after/jacoco.csv),
[before JaCoCo XML](evidence/hex-before/jacoco.xml), and
[after JaCoCo XML](evidence/hex-after/jacoco.xml).

### Comparison with the course baseline

The September 19 [course baseline](../Setup/testCoverage.html) records
`libs/shared` at 366 / 1,602 covered lines (22.85%) and 165 / 768 covered
branches (21.48%). The fresh before run reproduces these figures exactly,
so the module improvements above also apply relative to that baseline.

The historical combined coverage was 82.05% lines and 68.18% branches
across the modules. This contribution reran only `libs/shared`.
