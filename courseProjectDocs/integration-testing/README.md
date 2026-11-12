# Running the (Kobe's) Integration Test

From the repository root, run:

```bash
./mvnw -pl server "-Dtest=FilteredSelectIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

The test creates a table, inserts sample students, filters for SWEN-777, and verifies the ordered results returned by the real CrateDB integration-test environment.
