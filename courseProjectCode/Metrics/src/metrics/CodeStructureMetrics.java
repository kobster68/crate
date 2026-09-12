package metrics;

import java.io.File;
import java.io.IOException;

public class CodeStructureMetrics {

    public static void linesOfCode() {
        File outputDir = new File("output");
        outputDir.mkdirs();

        ProcessBuilder processBuilder = new ProcessBuilder(
            "cloc",
            "--csv",
            "../..",
            "--by-file",
            "--include-lang=Java",
            "--out=output/cloc.csv"
        );

        processBuilder.inheritIO();

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