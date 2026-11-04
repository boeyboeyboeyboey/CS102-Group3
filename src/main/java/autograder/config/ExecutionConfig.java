package autograder.config;

import autograder.model.QuestionDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public record ExecutionConfig(Path directory, String reportFile, String javaExecutable,
                              String javacExecutable, long compileTimeoutMillis,
                              long caseTimeoutMillis, int maxOutputBytes, int workers) {
    public static final int MAX_WORKERS = 32;
    private static final String AUTO_WORKERS = "auto";
    // compared ignoring case because macos and windows filesystems treat Cases.csv as cases.csv
    private static final List<String> RESERVED_REPORT_FILES = List.of(
            "cases.csv", "submissions.csv", "similarity.csv", "starter-analysis.csv",
            "sanity-checks.csv");

    public ExecutionConfig(Path directory, String reportFile, String javaExecutable,
                           String javacExecutable, long compileTimeoutMillis,
                           long caseTimeoutMillis, int maxOutputBytes) {
        this(directory, reportFile, javaExecutable, javacExecutable, compileTimeoutMillis,
                caseTimeoutMillis, maxOutputBytes, 1);
    }

    public static ExecutionConfig load(Path file, AppConfig app) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        Path baseDirectory = file.toAbsolutePath().normalize().getParent();
        return load(properties, baseDirectory, app);
    }

    static ExecutionConfig load(Properties properties, Path baseDirectory, AppConfig app)
            throws IOException {
        return load(properties, baseDirectory, app, WorkerLimits.system());
    }

    // limits are a parameter so tests can resolve auto without the real cpu count
    static ExecutionConfig load(Properties properties, Path baseDirectory, AppConfig app,
                                WorkerLimits limits) throws IOException {
        Path directory = AppConfig.canonicalize(baseDirectory.resolve(required(properties, "grading.directory")));
        List<Path> protectedPaths = new ArrayList<>(List.of(app.getInputDirectory(),
                app.getExtractionDirectory(), app.getWorkingDirectory(), app.getScoreSheetPath(),
                app.getAnomalyLog(), app.getCorrectionLog()));
        for (QuestionDefinition question : app.getAssessment().getQuestions()) {
            protectedPaths.add(question.getTesterFile());
            protectedPaths.addAll(question.getSupportFiles());
            protectedPaths.addAll(question.getFixtures());
        }
        for (Path protectedPath : protectedPaths) {
            if (directory.startsWith(protectedPath) || protectedPath.startsWith(directory)) {
                throw new IllegalArgumentException("grading directory overlaps another input/output: "
                        + protectedPath);
            }
        }
        String report = required(properties, "grading.report.file");
        if (!report.endsWith(".csv") || report.contains("/") || report.contains("\\")
                || report.contains(":") || isReservedReportFile(report)) {
            throw new IllegalArgumentException("grading.report.file must be a distinct CSV filename");
        }
        long outputBytes = positive(properties, "execution.output.max.bytes");
        if (outputBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("execution.output.max.bytes is too large");
        }
        return new ExecutionConfig(directory, report, executable(properties, "execution.java", baseDirectory),
                executable(properties, "execution.javac", baseDirectory),
                positive(properties, "execution.compile.timeout.ms"),
                positive(properties, "execution.case.timeout.ms"), (int) outputBytes, workers(properties, limits));
    }

    public ExecutionConfig withWorkers(int workers) {
        if (workers <= 0) {
            throw new IllegalArgumentException("configuration must be positive: execution.workers");
        }
        if (workers > MAX_WORKERS) {
            throw new IllegalArgumentException("execution.workers must be at most " + MAX_WORKERS);
        }
        return new ExecutionConfig(directory, reportFile, javaExecutable, javacExecutable,
                compileTimeoutMillis, caseTimeoutMillis, maxOutputBytes, workers);
    }

    private static int workers(Properties properties, WorkerLimits limits) {
        String value = properties.getProperty("execution.workers");
        if (value == null || value.isBlank()) {
            return 1;
        }
        if (value.trim().equalsIgnoreCase(AUTO_WORKERS)) {
            return limits.autoWorkers();
        }
        long workers = positive(properties, "execution.workers");
        if (workers > MAX_WORKERS) {
            throw new IllegalArgumentException("execution.workers must be at most " + MAX_WORKERS);
        }
        return (int) workers;
    }

    private static boolean isReservedReportFile(String report) {
        for (String reserved : RESERVED_REPORT_FILES) {
            if (reserved.equalsIgnoreCase(report)) {
                return true;
            }
        }
        return false;
    }

    private static String executable(Properties properties, String key, Path baseDirectory) {
        String value = required(properties, key);
        if (value.equals("JDK")) {
            String name = key.equals("execution.java") ? "java" : "javac";
            if (System.getProperty("os.name").startsWith("Windows")) {
                name += ".exe";
            }
            return Path.of(System.getProperty("java.home"), "bin", name).toString();
        }
        if (value.contains("/") || value.contains("\\")) {
            return baseDirectory.resolve(value).normalize().toString();
        }
        return value;
    }

    private static long positive(Properties properties, String key) {
        try {
            long value = Long.parseLong(required(properties, key));
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid positive integer: " + key, exception);
        }
        throw new IllegalArgumentException("configuration must be positive: " + key);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required configuration value: " + key);
        }
        return value.trim();
    }
}
