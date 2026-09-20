# How to Build and Run Tests

## Before You Start

Install Java JDK 26.0.2.1 and set `JAVA_HOME` to its folder.
You also need an internet connection.

Open PowerShell in the project's main folder, containing `mvnw.cmd` (you will need to run this).

## 1. Check Java

Run:

```powershell
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

The version should show Java 26. CrateDB is built and ran in Java 26.

## 2. Build the Project

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' -DskipTests clean package
```

This builds the project. Tests are run separately in the next step.
The commands exclude the benchmarks and course project code, which otherwise throws an error.

## 3. Run the Tests

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' "-Dmaven.test.failure.ignore=true" test
```

This can take up to several hours. The command continues even if tests fail.

If you experience "AccessDenied", open a administrator PowerShell and try again.

Test results are saved in each module's `target/surefire-reports` folder.

## 4. Create the Coverage Reports

Coverage shows how much code the tests ran. Execute this command:

```powershell
.\mvnw.cmd -T1 -fae -pl '!benchmarks,!courseProjectCode' jacoco:report
```

Run this after the tests, otherwise it will not work.

Open `target/site/jacoco/index.html` inside a module's folder to view
its specific coverage report.

## Saved Results

This Setup folder contains:

- `testResults.html` — a summary of passed, failed, and skipped tests.
- `testCoverage.html` — a summary of code coverage.
- `report.md` — the environment setup and observations.