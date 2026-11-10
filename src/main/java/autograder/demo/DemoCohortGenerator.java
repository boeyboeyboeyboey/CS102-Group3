package autograder.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Builds a demo cohort of real plus synthetic submissions to exercise parallel grading. */
public final class DemoCohortGenerator {
    static final String DEFAULT_CONFIG = "config.properties";
    static final String DEFAULT_OUTPUT = "build/demo-cohort";
    static final String SHEET_NAME = "IS442-ScoreSheet.csv";
    private static final int DEFAULT_COUNT = 94;
    private static final long DEFAULT_SEED = 102;
    private static final String EOL = "\r\n";
    private static final String KEY_INPUT_DIRECTORY = "input.directory";
    private static final String KEY_SCORE_SHEET = "score.sheet.path";
    private static final String KEY_ARCHIVE_REGEX = "identity.archive.username.regex";
    private static final String KEY_QUESTIONS = "question.directories.required";
    private static final String SUBMISSIONS_DIRECTORY = "submissions";
    private static final String DONORS_LOG = "donors.csv";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String ARCHIVE_PREFIX = "2023-2024-";
    private static final int SEED_MULTIPLIER = 31;
    private static final int SEED_OFFSET = 7;

    private DemoCohortGenerator() {
    }

    public static void main(String[] args) throws IOException {
        int count;
        long seed;
        try {
            count = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_COUNT;
            seed = args.length > 1 ? Long.parseLong(args[1]) : DEFAULT_SEED;
        } catch (NumberFormatException e) {
            count = -1;
            seed = 0;
        }
        if (args.length > 2 || count < 1 || count > SyntheticRoster.MAX_COUNT) {
            System.err.println("usage: DemoCohortGenerator [count 1-" + SyntheticRoster.MAX_COUNT
                    + "] [seed]");
            System.exit(1);
            return;
        }
        Properties config = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(DEFAULT_CONFIG))) {
            config.load(input);
        }
        Path output = Path.of(DEFAULT_OUTPUT);
        generate(config, Path.of("."), output, count, seed);
        System.out.println("Demo cohort written to " + output + " (" + count + " synthetic)");
    }

    /** Resolves configured inputs against baseDirectory and rebuilds outputDirectory. */
    static void generate(Properties config, Path baseDirectory, Path outputDirectory, int count,
            long seed) throws IOException {
        Path inputDirectory = baseDirectory.resolve(config.getProperty(KEY_INPUT_DIRECTORY));
        Path sheet = baseDirectory.resolve(config.getProperty(KEY_SCORE_SHEET));
        Pattern archiveName = Pattern.compile(config.getProperty(KEY_ARCHIVE_REGEX));
        List<String> questions = List.of(config.getProperty(KEY_QUESTIONS)
                .split("\\s*,\\s*"));
        recreate(outputDirectory);
        Path submissions = Files.createDirectories(outputDirectory.resolve(SUBMISSIONS_DIRECTORY));
        List<DonorArchive> donors = copyRealArchives(inputDirectory, submissions, archiveName,
                questions);
        List<String> sheetLines = Files.readAllLines(sheet, StandardCharsets.UTF_8);
        IdRange ids = nextIds(sheetLines);
        Set<String> reserved = donors.stream().map(DonorArchive::username)
                .collect(Collectors.toSet());
        List<SyntheticRoster.Identity> roster = SyntheticRoster.generate(count, seed, reserved,
                ids.next(), ids.width());
        writeSheet(outputDirectory.resolve(SHEET_NAME), sheet, roster);
        writeSynthetic(submissions, outputDirectory.resolve(DONORS_LOG), roster, questions,
                donors, seed);
    }

    private static List<DonorArchive> copyRealArchives(Path inputDirectory, Path submissions,
            Pattern archiveName, List<String> questions) throws IOException {
        List<Path> zips;
        try (Stream<Path> listing = Files.list(inputDirectory)) {
            zips = listing.filter(path -> path.getFileName().toString().endsWith(ZIP_EXTENSION))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
        }
        List<DonorArchive> donors = new ArrayList<>();
        for (Path zip : zips) {
            Matcher matcher = archiveName.matcher(zip.getFileName().toString());
            if (!matcher.matches()) {
                throw new IOException("archive name not recognised: " + zip.getFileName());
            }
            Files.copy(zip, submissions.resolve(zip.getFileName()));
            donors.add(DonorArchive.read(zip, matcher.group(1), questions));
        }
        return donors;
    }

    private record IdRange(long next, int width) {
    }

    private static IdRange nextIds(List<String> sheetLines) {
        long max = 0;
        int width = 0;
        for (String line : sheetLines.subList(1, sheetLines.size())) {
            String id = line.split(",", -1)[0].replace("#", "");
            if (!id.isEmpty()) {
                max = Math.max(max, Long.parseLong(id));
                width = Math.max(width, id.length());
            }
        }
        return new IdRange(max + 1, width);
    }

    private static void writeSheet(Path target, Path original, List<SyntheticRoster.Identity> roster)
            throws IOException {
        StringBuilder rows = new StringBuilder();
        for (SyntheticRoster.Identity student : roster) {
            rows.append(student.id()).append(",#").append(student.username()).append(",_,")
                    .append(student.fullName()).append(',').append(student.email())
                    .append(",,20,#").append(EOL);
        }
        byte[] head = Files.readAllBytes(original);
        Files.write(target, head);
        if (head.length > 0 && head[head.length - 1] != '\n') {
            Files.write(target, EOL.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }
        Files.write(target, rows.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND);
    }

    private static void writeSynthetic(Path submissions, Path donorsCsv,
            List<SyntheticRoster.Identity> roster, List<String> questions,
            List<DonorArchive> donors, long seed) throws IOException {
        Random random = new Random(seed * SEED_MULTIPLIER + SEED_OFFSET);
        StringBuilder log = new StringBuilder("Username,Q1Donor,Q2Donor,Q3Donor\n");
        for (SyntheticRoster.Identity student : roster) {
            List<DonorArchive> chosen = new ArrayList<>();
            log.append(student.username());
            for (int i = 0; i < questions.size(); i++) {
                DonorArchive donor = donors.get(random.nextInt(donors.size()));
                chosen.add(donor);
                log.append(',').append(donor.username());
            }
            log.append('\n');
            SyntheticArchiveWriter.write(
                    submissions.resolve(ARCHIVE_PREFIX + student.username() + ZIP_EXTENSION), student,
                    questions, chosen);
        }
        Files.writeString(donorsCsv, log.toString(), StandardCharsets.UTF_8);
    }

    private static void recreate(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                    Files.delete(path);
                }
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        Files.createDirectories(directory);
    }
}
