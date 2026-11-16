# Integration Testing Report

## Michael - HTTP COUNT(*) test

### Test Design Summary

I added `test_count_star_with_bound_filter_argument` to `RestSQLActionIntegrationTest.java`. It sends a `COUNT(*)` query through the test cluster's HTTP interface and checks the response. The same SQL statement is sent twice with different names in the JSON arguments.

The test covers two module interactions.

- The `server` module uses `libs/es-x-content` to read the SQL statement and arguments from the JSON request.
- The `server` module uses `libs/sql-parser` to parse the SQL statement before binding the argument and executing the query.

I chose this case because it builds on my earlier CountTask testing. It checks that request parsing, argument binding, and counting work together. Changing the name between requests checks that CrateDB counts the correct matching rows. One name contains an apostrophe, which also checks that this character is handled correctly in the bound value.

### Test Data Preparation

The test creates `doc.count_by_name` with one text column called `name`. It inserts three rows directly in the test, so the input data and expected counts are known.

| Name | Rows inserted | Expected count when used as the argument |
| --- | --- | --- |
| `O'Reilly` | 2 | 2 |
| `Atwood` | 1 | 1 |

The insert statement uses `O''Reilly` to represent an apostrophe inside a SQL string. The stored name and the JSON argument both contain the single apostrophe in `O'Reilly`.

After inserting the rows, the test runs `REFRESH TABLE doc.count_by_name` to make them visible to queries. This lets the count checks run against the prepared data without waiting for an automatic refresh.

### Execution and Results

I ran the tests using Maven, JUnit, and Java JDK 26.0.2.1. The run included my new test and the five existing baseline checks. The [README](README.md) gives the command and the locations of the Surefire results.

| Test selection | Run | Passed | Failed | Errors | Skipped |
| --- | --- | --- | --- | --- | --- |
| New HTTP count test | 1 | 1 | 0 | 0 | 0 |
| Existing baseline checks | 5 | 5 | 0 | 0 | 0 |
| Total | 6 | 6 | 0 | 0 | 0 |

Both requests in the new test returned HTTP 200. The count was 2 for `O'Reilly` and 1 for `Atwood`, matching the prepared data. Maven reported `BUILD SUCCESS`, and Checkstyle reported zero violations. No defects were observed in these six checks.

### Combined Verification

I ran my new test together with Kobe's `FilteredSelectIntegrationTest` and the five existing checks. All 7 tests passed, with 0 failures, 0 errors, and 0 skipped. Maven reported `BUILD SUCCESS`, and Checkstyle reported zero violations. The combined command is included in the [README](README.md).

## Group Contributions

### Michael Andrews

Contributed the parameterized HTTP `COUNT(*)` test, reviewed the module interactions and expected results, and documented the test design, data preparation, execution results, and run instructions.
