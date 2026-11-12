# Integration Testing Report

## Summary

The integration test is `FilteredSelectIntegrationTest` in the server module. It uses CrateDB's real integration-test framework instead of mocking the database or query components. The test connects table creation and storage with SQL parsing, query planning, filtering, ordering, and result delivery.

The scenario creates a table, inserts students, and runs a `SELECT` query that filters rows by course, renames the selected column with `AS`, and orders the results by name.

## Test Data

The test creates a `users` table with `name` and `course` columns. It inserts three rows directly through the integration-test SQL executor: Alice and Bob belong to SWEN-777, while Kara belongs to SWEN-755. After refreshing the table, the test queries only SWEN-777 and expects Alice and Bob in alphabetical order.

The input data is generated inside the test, so it does not depend on external files or an existing database. The expected output is defined by the test assertion and contains the two matching names.

## Command and Results

The targeted Maven command is:

```bash
./mvnw -pl server "-Dtest=FilteredSelectIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Checkstyle passed, the test classes compiled, and the integration test completed successfully.

## Bug Reports

No bugs were identified with the addition of the integration test.
