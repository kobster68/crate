package metrics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CodeStructureMetrics {

    public static void linesOfCode() {
        // Create output directory if it doesn't exist
        File outputDir = new File("output");
        outputDir.mkdirs();

        // Define the root path of the repository
        Path repoRoot = Paths.get("../..");

        // Find all production Java directories (src/main/java) in the repository
        List<String> productionJavaDirs;

        try (Stream<Path> paths = Files.walk(repoRoot)) {
            productionJavaDirs = paths
                .filter(Files::isDirectory)
                .filter(path -> path.endsWith(Paths.get("src", "main", "java")))
                .filter(path -> !path.toString().contains("benchmarks"))
                .map(Path::toString)
                .toList();
        } catch (IOException e) {
            System.err.println("Error while searching for production Java directories: " + e.getMessage());
            return;
        }

        // Prepare the command to run cloc
        List<String> command = new ArrayList<>();

        command.add("cloc");
        command.add("--csv");
        command.add("--by-file");
        command.add("--include-lang=Java");
        command.add("--out=output/cloc.csv");

        // Add all production Java directories to the command
        command.addAll(productionJavaDirs);

        // Execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // See the output of the cloc command in the console
        processBuilder.inheritIO();

        System.out.println("Running cloc...");

        // Start the process and wait for it to finish
        try {
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("cloc failed with exit code: " + exitCode);
            }
        } catch (IOException e) {
            System.err.println("Could not launch cloc.");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("cloc execution was interrupted.");
        }
    }

    public static void commentDensity() {
        throw new UnsupportedOperationException("Unimplemented method 'commentDensity'");
    }
    
}