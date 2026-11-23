# System Testing

## Michael - Order workflow

The three tests in `test_order_workflow.py` check reading, updating, and deleting orders through CrateDB's public HTTP SQL interface. Each test creates its own table and removes it afterward. [Test file](../../courseProjectCode/system-testing/test_order_workflow.py)

## Kobe - Filtered student search

The test in `test_system.py` creates student records, filters for SWEN-777, and checks the column alias, matching names, and their order. Its table also has a unique name and is removed after the test. [Test file](../../courseProjectCode/system-testing/test_system.py)

## Before You Start

Use Python 3.14 and Java JDK 26.0.2.1, with `JAVA_HOME` set to the JDK folder. Both scripts use Python's standard library. Maven is included through the project wrapper.

Open PowerShell in the project root, where `mvnw.cmd` is located. Use a local CrateDB instance that allows creating and dropping test tables in the `doc` schema. The packaged configuration gives the `crate` user trusted access from localhost.

## Build CrateDB

```powershell
.\mvnw.cmd -B -T1 -pl app,jlink-jdk -am "-DskipTests" package
```

This builds the application and its bundled Java runtime. The test suites are skipped during this packaging step.

Extract the generated `crate-*.zip` from `app/target/` into a folder whose full path has no spaces, such as `C:\crate-system-test`. The extracted CrateDB folder contains `bin`, `config`, `jdk`, and `lib`.

## Start the Local Server

Open a terminal inside the extracted CrateDB folder and run the following commands.

```powershell
$env:CRATE_HEAP_SIZE = "512m"
cmd /d /c bin\crate.bat -Ccluster.name=swen777-system-testing -Cnode.name=swen777-system-node -Cnetwork.host=127.0.0.1 -Cdiscovery.type=single-node -Chttp.port=44200 -Cpsql.port=44300 -Ctransport.tcp.port=44301
```

This starts a single local node with a 512 MB heap. The HTTP SQL endpoint is `http://127.0.0.1:44200/_sql`.

Wait for the server's log to show that the node has started. Keep this window open while running the tests. Press Ctrl+C in this window to stop the server when finished.

## Run the Tests

Open a second terminal window in the project root. Set the server address, then run all four tests.

```powershell
$env:CRATE_HTTP_URL = "http://127.0.0.1:44200"
python -m unittest discover -s courseProjectCode/system-testing -p "test*.py" -v
```

Both scripts read `CRATE_HTTP_URL`. Set it to the server's base address, without `/_sql`. If it is unset, both scripts use `http://127.0.0.1:4200`.

Expected results are 4 tests passed, 0 failures, 0 errors, and 0 skipped. The output lists each case and ends with `Ran 4 tests` and `OK`.

### Run Michael's Tests Separately

In the same test terminal, run the following command.

```powershell
python courseProjectCode/system-testing/test_order_workflow.py -v
```

Expected results are 3 tests passed. The paid-order summaries are 2 orders totaling 100 for the read test, 3 totaling 125 after the update, and 1 totaling 60 after the deletion. Each case starts from a fresh copy of the same three orders.

### Run Kobe's Test Separately

```powershell
python courseProjectCode/system-testing/test_system.py -v
```

Expected results are 1 test passed. The query returns the column `student` and the rows Alice, then Bob. Kara is excluded.

On Linux/WSL, use `./mvnw` to build, start `bin/crate` with the same `-C` options, and use `python3` for the test commands. Set the address with `export CRATE_HTTP_URL=http://127.0.0.1:44200`.

## Reports and Saved Results

- Kobe's [report](report.md) and [saved test output](evidence/kobe.txt).
- Michael's [report](report-michael.md) and [saved test output](evidence/michael.txt).
- [Combined four-test output](evidence/combined.txt).

The saved output files include the individual runs and the combined four-test run. Failed assertions or connection errors produce a nonzero exit code. Each test registers cleanup to drop its own table after execution, including when an assertion fails.
