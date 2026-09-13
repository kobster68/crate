package metrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Read one module's Surefire test counts and JaCoCo line coverage. */
public class TestabilityCollector {

    public record TestCounts(int tests, int failures, int errors, int skipped) {

    }

    public record ModuleCounts(int suites, TestCounts counts) {

    }

    public record LineCoverage(int missedLines, int coveredLines) {
        public double coveragePct() {
            int totalLines = missedLines + coveredLines;
            // No executable lines means the percentage is undefined, not zero coverage.
            return totalLines == 0 ? Double.NaN : coveredLines * 100.0 / totalLines;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && args[0].equals("--all-modules")) {
            if (args.length != 2) {
                throw new IllegalArgumentException("Usage: --all-modules <repository path>");
            }
            writeModulesCsv(Path.of(args[1]), ProjectMetricsCollector.getModuleNames());
            return;
        }
        if (args.length >= 1 && args[0].equals("--modules")) {
            if (args.length < 3) {
                throw new IllegalArgumentException("Usage: --modules <repository path> <module> [more modules]");
            }
            writeModulesCsv(Path.of(args[1]), Arrays.asList(args).subList(2, args.length));
            return;
        }
        if (args.length < 1 || args.length > 3) {
            System.err.println("Usage: TestabilityCollector <Surefire reports directory> [JaCoCo XML file] [output CSV file]");
            System.exit(1);
        }
        Path reportsDirectory = Path.of(args[0]);
        ModuleCounts module = readModuleCounts(reportsDirectory);
        System.out.println("Report directory: " + reportsDirectory);
        System.out.println("Suites: " + module.suites());
        System.out.println("Tests: " + module.counts().tests());
        System.out.println("Failures: " + module.counts().failures());
        System.out.println("Errors: " + module.counts().errors());
        System.out.println("Skipped: " + module.counts().skipped());
        if (args.length >= 2) {
            LineCoverage coverage = readLineCoverage(Path.of(args[1]));
            System.out.println("Missed lines: " + coverage.missedLines());
            System.out.println("Covered lines: " + coverage.coveredLines());
            if (Double.isNaN(coverage.coveragePct())) {
                System.out.println("Line coverage: N/A (no executable lines)");
            } else {
                System.out.printf("Line coverage: %.2f%%%n", coverage.coveragePct());
            }
            if (args.length == 3) {
                writeCsv(Path.of(args[2]), reportsDirectory, module, coverage);
            }
        }
    }

    public static void writeModulesCsv(Path repository, List<String> moduleNames) throws Exception {
        Path output = repository.resolve("courseProjectCode/Metrics/TestabilityPerModule.csv");
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("Module,Status,Suites,Tests,Failures,Errors,Skipped,MissedLines,CoveredLines,LineCoveragePct");
            writer.newLine();
            for (String name : moduleNames.stream().distinct().sorted().toList()) {
                Path moduleDirectory = repository.resolve(name);
                Path reports = moduleDirectory.resolve("target/surefire-reports");
                Path coverageReport = moduleDirectory.resolve("target/site/jacoco/jacoco.xml");

                // Missing reports are unavailable measurements, not zero tests or coverage.
                ModuleCounts counts = hasTestReports(reports) ? readModuleCounts(reports) : null;
                LineCoverage coverage = Files.isRegularFile(coverageReport) ? readLineCoverage(coverageReport) : null;
                String status = counts == null ? "No test reports"
                    : coverage == null ? "No coverage report" : "Reports read";
                String testFields = counts == null ? ",,,,"
                    : counts.suites() + "," + counts.counts().tests() + "," + counts.counts().failures()
                        + "," + counts.counts().errors() + "," + counts.counts().skipped();
                String coverageFields = coverage == null ? ",,"
                    : coverage.missedLines() + "," + coverage.coveredLines() + ","
                        + (Double.isNaN(coverage.coveragePct()) ? ""
                            : String.format("%.2f", coverage.coveragePct()));
                writer.write("\"" + name.replace("\"", "\"\"") + "\"," + status + "," + testFields + "," + coverageFields);
                writer.newLine();
                System.out.println(name + ": " + status);
            }
        }
        System.out.println("CSV written to: " + output.toAbsolutePath());
    }

    private static boolean hasTestReports(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                .anyMatch(path -> path.getFileName().toString().startsWith("TEST-")
                    && path.getFileName().toString().endsWith(".xml"));
        }
    }

    public static ModuleCounts readModuleCounts(Path reportsDirectory) throws Exception {
        List<Path> reports;
        try (Stream<Path> paths = Files.list(reportsDirectory)) {
            // Filter for files that start with "TEST-" and end with ".xml", which are the Surefire report files.
            reports = paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith("TEST-"))
                .filter(path -> path.getFileName().toString().endsWith(".xml"))
                .sorted()
                .toList();
        }

        if (reports.isEmpty()) {
            throw new IllegalArgumentException("No Surefire reports found in: " + reportsDirectory);
        }

        int totalTests = 0;
        int totalFailures = 0;
        int totalErrors = 0;
        int totalSkipped = 0;
        // Build the totals by reading each report and summing the counts.
        for (Path report : reports) {
            TestCounts counts = readTestCounts(report);
            totalTests += counts.tests();
            totalFailures += counts.failures();
            totalErrors += counts.errors();
            totalSkipped += counts.skipped();
        }

        TestCounts totals = new TestCounts(totalTests, totalFailures, totalErrors, totalSkipped);
        return new ModuleCounts(reports.size(), totals);
    }

    public static void writeCsv(Path output, Path reportsDirectory,
                                ModuleCounts module, LineCoverage coverage) throws IOException {
        Path destination = output.toAbsolutePath().normalize();
        Files.createDirectories(destination.getParent());
        String coveragePct = Double.isNaN(coverage.coveragePct()) ? ""
            : String.format("%.2f", coverage.coveragePct());
        // Quote the path so commas or quotation marks cannot break the CSV columns.
        String reportPath = "\"" + reportsDirectory.toAbsolutePath().normalize()
            .toString().replace("\"", "\"\"") + "\"";

        try (BufferedWriter writer = Files.newBufferedWriter(destination)) {
            writer.write("ReportDirectory,Suites,Tests,Failures,Errors,Skipped,MissedLines,CoveredLines,LineCoveragePct");
            writer.newLine();
            writer.write(reportPath + "," + module.suites() + "," + module.counts().tests()
                + "," + module.counts().failures() + "," + module.counts().errors()
                + "," + module.counts().skipped() + "," + coverage.missedLines()
                + "," + coverage.coveredLines() + "," + coveragePct);
            writer.newLine();
        }
        System.out.println("CSV written to: " + destination);
    }

    public static LineCoverage readLineCoverage(Path reportPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        // JaCoCo declares an external DTD, but we can read its XML without loading it.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document document = factory.newDocumentBuilder().parse(reportPath.toFile());
        Element report = document.getDocumentElement();
        if (!report.getTagName().equals("report")) {
            throw new IllegalArgumentException("Expected a JaCoCo <report> element");
        }

        // Visit only direct children: package/class counters overlap with these totals.
        for (Node child = report.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element
                    && element.getTagName().equals("counter")
                    && element.getAttribute("type").equals("LINE")) {
                int missed = Integer.parseInt(element.getAttribute("missed"));
                int covered = Integer.parseInt(element.getAttribute("covered"));
                return new LineCoverage(missed, covered);
            }
        }
        throw new IllegalArgumentException("No report-level LINE counter found in: " + reportPath);
    }

    public static TestCounts readTestCounts(Path reportPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder().parse(reportPath.toFile());

        // The outermost XML element should be <testsuite>.
        Element suite = document.getDocumentElement();
        if (!suite.getTagName().equals("testsuite")) {
            throw new IllegalArgumentException("Expected a Surefire <testsuite> report");
        }

        // XML attributes are strings, so convert them to numbers.
        int tests = Integer.parseInt(suite.getAttribute("tests"));
        int failures = Integer.parseInt(suite.getAttribute("failures"));
        int errors = Integer.parseInt(suite.getAttribute("errors"));
        int skipped = Integer.parseInt(suite.getAttribute("skipped"));

        return new TestCounts(tests, failures, errors, skipped);
    }
}
