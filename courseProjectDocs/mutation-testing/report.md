# Mutation Testing Report

## Kobe's Mutation Testing Setup

The project did not previously have mutation testing configured. PIT 1.30.0 was added to `server/pom.xml` and configured to mutate `io.crate.expression.scalar.TimeZoneParser` using `TimeZoneParserTest`.

Reports are generated at `server/target/pit-reports/index.html`. Saved evidence is in:

- `evidence/pit-mutation-before/`
- `evidence/pit-mutation-after/`

### Mutation Score Results

| Measurement | Initial | Final |
|---|---:|---:|
| Line coverage | 83% (20/24) | 88% (21/24) |
| Mutation coverage | 50% (4/8) | 63% (5/8) |
| Test strength | 57% (4/7) | 71% (5/7) |

The final report shows improvement after adding tests for named time zones and positive offsets.

### Mutants Killed

The original tests caught incorrect behavior for null input, UTC, invalid offsets, and returned time zones. I added `test_named_timezone_is_resolved`, which catches errors in named time-zone handling. This increased mutation coverage from 50% to 63%.

### Mutants Surviving

Two changes were not detected, one affects the leading `+` sign, and the other affects the offset calculation. My positive-offset tests run these parts of the code, but PIT still reports both changes as undetected. Changing the `+` handling does not currently cause a test to fail.

## Michael's Mutation Testing Setup

TODO

## Group Contributions

### Kobe LaPrade

- Used previously tested `TimeZoneParser` for my mutation-testing target.
- Added PIT to maven.
- Added new tests for TimeZoneParser to cover mutants
- Ran the initial and final PIT reports.
- Saved before/after evidence
