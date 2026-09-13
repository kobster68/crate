/**
 * Copyright 2026 Michael Andrews
 * 
 * 
 * 
 */

package metrics;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration.LanguageLevel;



public class ProjectMetricsCollector {
    
    private static String baseDir;
    //Get the file paths of each module listed in the Pom File
    private static String pomFile = " <modules>\r\n"
            + "        <module>libs/shared</module>\r\n"
            + "        <module>libs/dex</module>\r\n"
            + "        <module>libs/es-x-content</module>\r\n"
            + "        <module>libs/cli</module>\r\n"
            + "        <module>libs/guice</module>\r\n"
            + "        <module>libs/sql-parser</module>\r\n"
            + "        <module>libs/pgwire</module>\r\n"
            + "        <module>libs/azure-testing</module>\r\n"
            + "        <module>libs/opendal</module>\r\n"
            + "        <module>server</module>\r\n"
            + "        <module>plugins/es-analysis-common</module>\r\n"
            + "        <module>plugins/es-analysis-phonetic</module>\r\n"
            + "        <module>plugins/es-discovery-ec2</module>\r\n"
            + "        <module>plugins/es-repository-s3</module>\r\n"
            + "        <module>plugins/es-repository-url</module>\r\n"
            + "        <module>plugins/es-repository-azure</module>\r\n"
            + "        <module>plugins/repository-gcs</module>\r\n"
            + "        <module>plugins/dns-discovery</module>\r\n"
            + "        <module>plugins/cr8-copy-s3</module>\r\n"
            + "        <module>plugins/crate-copy-azure</module>\r\n"
            + "        <module>extensions/functions</module>\r\n"
            + "        <module>extensions/lang-js</module>\r\n"
            + "        <module>extensions/jmx-monitoring</module>\r\n"
            + "        <module>jlink-jdk</module>\r\n"
            + "        <module>app</module>\r\n"
            + "        <module>benchmarks</module>\r\n"
            + "        <module>courseProjectCode</module>\r\n"
            + "    </modules>";
    
    private static List<String> modules;

    // Both collectors use this method so their module selection stays consistent.
    public static List<String> getModuleNames() {
        Pattern moduleTagRegex = Pattern.compile("<module>((?!benchmarks|courseProjectCode).*?)</module>");
        Matcher tagMatcher = moduleTagRegex.matcher(pomFile);
        List<String> moduleNames = new ArrayList<>();
        while (tagMatcher.find()) {
            moduleNames.add(tagMatcher.group(1));
        }
        return moduleNames;
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: <crate repo filepath>");
            System.exit(1);
        }
        baseDir = args[0];
        
        // Set the JavaParser language level to Java 22.
        StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(LanguageLevel.JAVA_22)
            .setTabSize(1);

        modules = getModuleNames();
        //Build each Module path relative to provided base directory
        List<Path> modulePaths = modules.stream().map(module -> Path.of(baseDir,module)).collect(Collectors.toList());

        //Collect production java file data from each module's src/main/java directory
        List<FileData> prodJavaFiles = new ArrayList<FileData>();
        for (int i = 0; i < modulePaths.size(); i++) {
            Path path = modulePaths.get(i);
            String moduleName = modules.get(i);
            if (Files.isDirectory(Path.of(path.toString(), "src", "main", "java")))
                try (Stream<Path> stream = Files.walk(Path.of(path.toString(), "src", "main", "java"))) {
                    stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(p -> {
                            prodJavaFiles.add(new FileData(p, moduleName));
                        });
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        // Collect production file LoC and create a FileMetrics object for each file . 
        List<FileMetrics> prodFileMetrics = new ArrayList<FileMetrics>();
        int fallbackFiles = 0;
        int unreadableFiles = 0;

        for (FileData fileData : prodJavaFiles) {
            try {
                String source = Files.readString(fileData.filePath());
                List<String> lines = source.lines().toList();
                List<String> linesWithoutComments = maskComments(source, lines, fileData.filePath());
                if (linesWithoutComments == null) {
                    fallbackFiles++;
                }

                int totalLines = lines.size();
                int codeLines = 0;
                int commentLines = 0;
                int blankLines = 0;

                for (int i = 0; i < lines.size(); i++) {
                    String trimmedLine = lines.get(i).trim();

                    if (trimmedLine.isEmpty()) {
                        blankLines++;
                    } else if (linesWithoutComments != null) {
                        if (linesWithoutComments.get(i).isBlank()) {
                            commentLines++;
                        } else {
                            codeLines++;
                        }
                    } else {
                        if (isCommentOnlyLine(trimmedLine)) {
                            commentLines++;
                        } else {
                            codeLines++;
                        }
                    }
                }
                FileLoc fileLoc = new FileLoc(totalLines, codeLines, commentLines, blankLines);
                prodFileMetrics.add(new FileMetrics(fileData, fileLoc));
            } catch (IOException e) {
                unreadableFiles++;
                e.printStackTrace();
            }
        }

       
        // Produce CSV files for Maintainability metrics
        writeMaintainabilityCSV(prodFileMetrics);
    }
    
    
    private static void writeMaintainabilityCSV(List<FileMetrics> files) {
        Path perFilePath = Path.of(baseDir, "courseProjectCode", "Metrics", "MetricsPerFile.csv");
        Path perModulePath = Path.of(baseDir, "courseProjectCode", "Metrics", "MetricsPerModule.csv");
        // Group file metrics by module
        Map<String, List<FileMetrics>> fileMetricsByModule = files.stream()
                .collect(Collectors.groupingBy(FileMetrics::module));
        // Create a ModuleLoc record for each module
        List<ModuleLoc> moduleMetrics = fileMetricsByModule.entrySet().stream()
                .map(entry -> toModuleLoc(entry.getKey(), entry.getValue()))
                .toList();
        // Create the CSV files if they don't exist
        try {
            if (!Files.exists(perModulePath))
                Files.createFile(perModulePath);
            if (!Files.exists(perFilePath))
                Files.createFile(perFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Write Per module metrics  
        try (BufferedWriter writer = Files.newBufferedWriter(perModulePath)) {
            // Write Header
            writer.write("Module,Files,CodeLines,CommentLines,BlankLines,TotalLines,CommentDensityPct");
            // For each module write metrics
            for (ModuleLoc m : moduleMetrics) {
                writer.write(System.lineSeparator());
                writer.write(
                    m.module() + ","
                    + m.files() + ","
                    + m.codeLines() + ","
                    + m.commentLines() + ","
                    + m.blankLines() + ","
                    + m.totalLines() + ","
                    + m.commentDensityPct()
                );
            }   
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Write Per file metrics
        try (BufferedWriter writer = Files.newBufferedWriter(perFilePath)) {
            // Write Header
            writer.write("FilePath,CodeLines,CommentLines,BlankLines,TotalLines,CommentDensityPct");
            // For each file write metrics
            for (FileMetrics f : files) {
                writer.write(System.lineSeparator());
                writer.write(
                    f.fileData().filePath() + ","
                    + f.fileLoc().codeLines() + ","
                    + f.fileLoc().commentLines() + ","
                    + f.fileLoc().blankLines() + ","
                    + f.fileLoc().totalLines() + ","
                    + (f.fileLoc().totalLines() == 0 ? 0.0 : f.fileLoc().commentLines() * 100.0 / f.fileLoc().totalLines())
                );
            } 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Turn a list of File Metrics into a single ModuleLoc record to aggregate file metrics by module. 
     */
    private static ModuleLoc toModuleLoc(String module, List<FileMetrics> metrics) {
        int files = metrics.size();
        int totalLines = metrics.stream().mapToInt(m -> m.fileLoc().totalLines()).sum();
        int codeLines = metrics.stream().mapToInt(m -> m.fileLoc().codeLines()).sum();
        int commentLines = metrics.stream().mapToInt(m -> m.fileLoc().commentLines()).sum();
        int blankLines = metrics.stream().mapToInt(m -> m.fileLoc().blankLines()).sum();
        double commentDensityPct = totalLines == 0
                ? 0.0
                : commentLines * 100.0 / totalLines;
        return new ModuleLoc(
                module,
                files,
                totalLines,
                codeLines,
                commentLines,
                blankLines,
                commentDensityPct
                );
    }
    
    
    private static List<String> maskComments(String source, List<String> lines, Path filePath) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            List<StringBuilder> maskedLines = lines.stream()
                .map(StringBuilder::new)
                .collect(Collectors.toList());
            // Get all comments in the compilation unit, including those in nested nodes
            List<Comment> comments = new ArrayList<>();
            cu.getComment().ifPresent(comments::add);
            comments.addAll(cu.getAllContainedComments());
            // Replace only comment characters with spaces. Keep code on mixed lines.
            // Parser line/column positions are one-based and the end is inclusive.
            for (Comment comment : comments) {
                comment.getRange().ifPresent(range -> {
                    for (int line = range.begin.line; line <= range.end.line; line++) {
                        StringBuilder maskedLine = maskedLines.get(line - 1);
                        int start = line == range.begin.line ? range.begin.column - 1 : 0;
                        int end = line == range.end.line ? range.end.column : maskedLine.length();
                        for (int column = start; column < end; column++) {
                            maskedLine.setCharAt(column, ' ');
                        }
                    }
                });
            }
            return maskedLines.stream().map(StringBuilder::toString).toList();
        } catch (ParseProblemException e) {
            // Keep collecting with the original approximation for unsupported syntax.
            System.err.println("[FALLBACK] Approximate line counts for: " + filePath
                + " - " + e.getMessage().lines().findFirst().orElse("Parse failed"));
            return null;
        }
    }

    private static boolean isCommentOnlyLine(String trimmedLine) {
        // Approximation used only when JavaParser cannot parse the file.
        return trimmedLine.startsWith("//")
                || trimmedLine.startsWith("/*")
                || trimmedLine.startsWith("*")
                || trimmedLine.endsWith("*/");
    }


}


