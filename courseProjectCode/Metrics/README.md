# CrateDB Quality Metrics Guide

Collects production Java lines of code (LOC) and comment density per file and module. Collects unit test counts, test suite counts, and automated test coverage.

## Requirements

- JDK 26
- Internet access for Maven (first run)

## Run

From the repository root:

**Windows PowerShell**
```powershell
.\mvnw.cmd -pl courseProjectCode compile exec:java "-Dexec.mainClass=metrics.ProjectMetricsCollector" "-Dexec.args=." "-Dcheckstyle.skip"
```

**macOS / Linux**
```sh
./mvnw -pl courseProjectCode compile exec:java \
  -Dexec.mainClass=metrics.ProjectMetricsCollector \
  -Dexec.args=. \
  -Dcheckstyle.skip
```

## Output

Collector sources: `courseProjectCode/src/main/java/metrics/`

Reports are written to `courseProjectCode/Metrics/`:

- `MetricsPerFile.csv`: line counts and comment density per file.
- `MetricsPerModule.csv`: aggregated counts and comment density per module.

## Scope

- Uses a hardcoded module list copied from the repository’s root `pom.xml`.