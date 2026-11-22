# System Testing

Kobe's single system test in
[test_system.py](../../courseProjectCode/system-testing/test_system.py) uses
Python's standard-library `unittest` and HTTP client. No pip packages are required.
It sends SQL requests through a running CrateDB server's public `POST /_sql` API.

The workflow matches the previous integration assignment: create a table with
`name` and `course` string columns, insert Alice and Bob in SWEN-777 and Kara in
SWEN-755, refresh, then filter for SWEN-777 using `name AS student` and `ORDER BY
name`. The expected rows are Alice followed by Bob. The table has a generated
name and is dropped after execution.

## Prerequisites

- Python 3.10 or newer.
- A running, disposable local CrateDB instance with its HTTP port accessible.
- The instance must allow unauthenticated SQL requests and table creation/deletion
  in the `doc` schema. The test uses unauthenticated HTTP requests.

Use the CrateDB distribution built from the checkout when verifying this branch.
Set `JAVA_HOME` to JDK 26.0.2.1, as required by this checkout's Maven configuration.
If you do not have a distribution, build one from the repository root:

```powershell
.\mvnw.cmd -T1 package "-DskipTests"
```

Extract the resulting
`app/target/crate-*.zip` into a temporary directory. In a separate terminal, start
its `bin/crate.bat` (Windows) or `bin/crate` (Linux/WSL) with:

```text
-Cdiscovery.type=single-node -Cnetwork.host=127.0.0.1 -Chttp.port=4200
```

Wait until the server accepts requests. Keep it running during the tests, and
stop it with Ctrl+C afterward. The tests do not start or stop the server.

## Execute

From the repository root in PowerShell:

```powershell
python -u courseProjectCode/system-testing/test_system.py
```

The default server is `http://127.0.0.1:4200`. To select another local instance:

```powershell
$env:CRATE_HTTP_URL = 'http://127.0.0.1:4420'
python -u courseProjectCode/system-testing/test_system.py
```

To select the case explicitly:

```powershell
python -u courseProjectCode/system-testing/test_system.py CrateSystemTests.test_filtered_student_search
```

On Linux/WSL, use `python3` and set the optional URL with
`export CRATE_HTTP_URL=http://127.0.0.1:4420`.

## Results and reproduction

A successful run reports `Ran 1 test` and `OK`, with process exit code 0:

```text
test_filtered_student_search ... ok
...
Ran 1 test in <elapsed time>s

OK
```

Assertion failures or connection errors produce a nonzero exit code; an unavailable
server is an error rather than a skipped or passing test.

To save an execution transcript in PowerShell:

```powershell
python -u courseProjectCode/system-testing/test_system.py 2>&1 | Tee-Object -FilePath courseProjectDocs/system-testing/execution.txt
$LASTEXITCODE
```

Record the date, Python version, server version/build, command, exit code, and
observed results in [report.md](report.md). A pre-existing build can be used for
an initial check, but record its version and do not present it as testing the
current source revision.

The test creates a randomly named `doc.system_test_<uuid>` table and
registers cleanup after creation. The insert is followed by
`REFRESH TABLE` before searches. This avoids depending on automatic refresh timing.
Cleanup errors fail the run. If the process is forcibly terminated, its test table
may remain; inspect and remove only that run's table before disposing of the instance.

The latest recorded run passed on November 19, 2026, against CrateDB
6.5.0-SNAPSHOT, build `ee7a3c5`: one test in 2.241 seconds, exit code 0.
See [report.md](report.md) for the environment and startup options and
[execution.txt](execution.txt) for the captured output.
