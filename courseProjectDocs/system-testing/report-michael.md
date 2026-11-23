# System Testing Report

## Michael - Order workflow

### Test Scope and Coverage

I added three black-box system tests in [test_order_workflow.py](../../courseProjectCode/system-testing/test_order_workflow.py). They use Python's `unittest` framework to send SQL requests through a running CrateDB instance's public HTTP interface.

The tests cover creating a table, inserting orders, reading stored data, updating an order's payment status, and deleting an order. They check that later queries show the expected data, paid-order count, and total amount.

### Test Case Summary

All three cases use the same setup code. CrateDB must be running at `http://127.0.0.1:44200/_sql`. Before each test, the setup creates a uniquely named table and inserts these orders.

| Order ID | Status | Amount |
| --- | --- | --- |
| 1 | paid | 40 |
| 2 | paid | 60 |
| 3 | pending | 25 |

The setup refreshes the table before querying. Each test gets its own table, and cleanup removes it afterward.

| Title | Preconditions | Test steps | Expected results |
| --- | --- | --- | --- |
| Paid order summary | Fresh table with the three orders above | Read all orders, then query the paid-order count and total | All three stored orders match the input. There are 2 paid orders totaling 100. |
| Mark a pending order as paid | Fresh table with order 3 pending | Set order 3 to paid, refresh the table, and query the paid-order summary | Exactly 1 row is updated. There are 3 paid orders totaling 125. |
| Delete a paid order | Fresh table with order 1 paid and order 3 pending | Delete order 1, refresh the table, and query the remaining orders and paid-order summary | Exactly 1 row is deleted. Orders 2 and 3 remain. There is 1 paid order totaling 60. |

### Execution and Results

I ran the tests using Python 3.14.0 and `unittest` against a local CrateDB 6.5.0-SNAPSHOT instance. The server used Java 26.0.2.1 and a 512 MB heap.

From the project root, I ran the following command.

```powershell
$env:CRATE_HTTP_URL = "http://127.0.0.1:44200"
python courseProjectCode/system-testing/test_order_workflow.py -v
```

| Test case | Result |
| --- | --- |
| Paid order summary | Passed |
| Mark a pending order as paid | Passed |
| Delete a paid order | Passed |

All 3 tests passed with 0 failures, 0 errors, and 0 skipped. The saved [test output](evidence/michael.txt) shows the results. Another query confirmed that cleanup left zero test tables.

### Combined Verification

I also ran Kobe's test and my three tests together against CrateDB 6.5.0-SNAPSHOT, build `35c2ccb`, built from the `system-testing` checkout. All 4 tests passed, with 0 failures, 0 errors, and 0 skipped. Cleanup left zero test tables. The command is in the [README](README.md), and the [combined output](evidence/combined.txt) records the run.

## Group Contributions

### Michael Andrews

Contributed three system tests for reading, updating, and deleting orders through CrateDB's HTTP SQL interface. Used a common setup with a fresh table for each test, checked the expected results and cleanup, and documented the test cases and results.
