# Metrics Collection

These scripts collect maintainability and testability metrics for the SWEN 777
Software Quality Assurance analysis of CrateDB. Run the commands below from the
repository root in PowerShell.

## Prerequisites

- Windows PowerShell
- cloc 2.10
- Java 26.

### Collection Procedure

For code structure metrics, run the powershell file:

```powershell
.\courseProjectCode\Metrics\code-structure-metrics.ps1
```

this outputs LoC metrics to 'production-java-cloc.csv' and 
comment density to 'production-java-modules.csv'

For testability, first generate fresh test and coverage reports:

```powershell
.\mvnw.cmd clean test jacoco:report
```

Then summarize the reports with the powershell file:

```powershell
.\courseProjectCode\Metrics\testability-metrics.ps1
```

The script does not run its own tests, it simply reads test reports.
