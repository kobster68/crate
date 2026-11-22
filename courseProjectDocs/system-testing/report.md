# System Testing Report

## Test Scope and Coverage

Kobe's single black-box system test, `test_filtered_student_search`, validates
the student search workflow against a running CrateDB server through its public
HTTP SQL API. All setup, actions, observations, and cleanup use SQL requests.

Coverage matches Kobe's previous `FilteredSelectIntegrationTest`: create a table
with `name` and `course` string columns; insert Alice and Bob in SWEN-777 and
Kara in SWEN-755; refresh; select `name AS student` for SWEN-777 ordered by name;
verify Alice and Bob are returned. A generated table name isolates the HTTP test
from existing tables, and cleanup drops it afterward.

The query is:

```sql
SELECT name AS student
FROM doc.system_test_<uuid>
WHERE course = 'SWEN-777'
ORDER BY name;
```

`<uuid>` represents the generated suffix used for that execution. The test checks
HTTP 200 responses, an insert count of 3, the column name `student`, rows
`[["Alice"], ["Bob"]]`, and a query row count of 2. Kara must be excluded.

This initial suite uses a single node. It does not cover authentication, the Admin
UI, PostgreSQL clients, restart durability, distributed recovery, or performance.

## Test Case Summary

| ID / Title | Pre-conditions | Test Steps | Expected Results |
| --- | --- | --- | --- |
| ST-01: Filtered student search | Running CrateDB HTTP endpoint; permission to create/drop tables in `doc`; unique test table | 1. Create a student table. 2. Insert Alice and Bob in SWEN-777 and Kara in SWEN-755. 3. Refresh. 4. Select `name AS student` for SWEN-777, ordered by name. 5. Drop the table. | Insert affects 3 rows. Query returns column `student` and exactly Alice, Bob in that order; row count is 2. Kara is excluded. Cleanup succeeds. |

## Execution and Results

Executed on November 19, 2026, on Windows 11 with Python 3.14.7 and Eclipse
Adoptium JDK 26.0.2.1+1. The server was the existing local distribution
`crate-6.5.0-2026-11-05-17-02-ee7a3c5.zip`, reporting version 6.5.0-SNAPSHOT,
build `ee7a3c5`. This verifies the suite against that build; the current branch
was not rebuilt for this run.

The archive was extracted under the ignored `target/system-testing-runtime/`
directory. Its `bin/crate.bat` was started with:

```text
-Cdiscovery.type=single-node -Cnetwork.host=127.0.0.1 -Chttp.port=4420 -Cpsql.port=5543 -Ctransport.tcp.port=4430 -Ccluster.name=course-system-testing
```

Executed from the repository root:

```powershell
$env:CRATE_HTTP_URL = 'http://127.0.0.1:4420'
python -u courseProjectCode/system-testing/test_system.py 2>&1 | Tee-Object -FilePath courseProjectDocs/system-testing/execution.txt
$LASTEXITCODE
```

The current single-case suite passed: **1 test, 0 failures, 0 errors**, in
2.241 seconds, with Python exit code **0**. The [execution transcript](execution.txt)
contains this run's output. Cleanup completed successfully. No behavioral
deviations were observed. The temporary server was stopped after execution.

| Case | Outcome | Deviations / failures |
| --- | --- | --- |
| ST-01 | Passed | None observed |

## Group Contributions

Kobe selected the workflow from his previous integration assignment, specified
the test data and separate course-project location, and limited this contribution
to one test. Implementation, comments, documentation, and local execution were
completed with Codex assistance. Student review remains pending.

| Student | Contribution status |
| --- | --- |
| Kobe | ST-01: defined the student search scenario and data, requested matching the earlier integration test, and directed documentation updates; student review pending. |
| Michael | No system test contribution recorded in this folder yet. |

This folder currently contains Kobe's one case. The assignment requires at least
one case per student, so Michael's contribution remains to be added before the
group submission.
