# Mutation Testing

## For Kobe's Tests

Use JDK 26, then run the focused tests:

```bash
./mvnw -pl server '-Dtest=TimeZoneParserTest' test
```

Run PIT for `TimeZoneParser`:

```bash
./mvnw -pl server test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage
```

View the report at `server/target/pit-reports/index.html`.
