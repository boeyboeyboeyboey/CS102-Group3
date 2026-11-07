package autograder.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoCohortGeneratorTest {
    private static final Set<String> ALLOWED_DIFFERENCES = Set.of("input.directory",
            "score.sheet.path", "extraction.directory", "working.directory", "grading.directory",
            "anomaly.log", "correction.log");
    private static final String SHEET_HEAD = "OrgDefinedId,Username,Last Name,First Name,Email,"
            + "Calculated Final Grade Numerator,Calculated Final Grade Denominator,"
            + "End-of-Line Indicator\r\n"
            + "#01400001,#ping.lee.2023,_,PING LEE,ping.lee.2023@computing.smu.edu.sg,,20,#\r\n"
            + "#01400002,#chee.teo.2022,_,TEO CHEE,chee.teo.2022@computing.smu.edu.sg,,20,#\r\n";

    @TempDir
    Path directory;

    @Test
    void generatesArchivesSheetAndDonorsWithSyntheticIdentity() throws Exception {
        Properties config = prepareInputs();
        Path output = directory.resolve("out");

        DemoCohortGenerator.generate(config, directory, output, 3, 102);

        List<String> sheet = Files.readAllLines(output.resolve("IS442-ScoreSheet.csv"));
        assertEquals(6, sheet.size());
        assertEquals(5, Files.list(output.resolve("submissions")).count());
        List<String> donors = Files.readAllLines(output.resolve("donors.csv"));
        assertEquals("Username,Q1Donor,Q2Donor,Q3Donor", donors.get(0));
        String username = donors.get(1).split(",")[0];
        assertTrue(sheet.get(3).startsWith("#01400003,#" + username + ",_,"));
        Path zip = output.resolve("submissions/2023-2024-" + username + ".zip");
        try (ZipFile file = new ZipFile(zip.toFile())) {
            Set<String> names = new TreeSet<>();
            file.stream().forEach(entry -> names.add(entry.getName()));
            assertTrue(names.contains(username + "/Q1/Q1a.java"));
            assertTrue(names.contains(username + "/Q2/data.txt"));
            assertTrue(names.stream().noneMatch(name -> name.contains("DS_Store")));
            String source = new String(file.getInputStream(
                    file.getEntry(username + "/Q1/Q1a.java")).readAllBytes(),
                    StandardCharsets.ISO_8859_1);
            assertTrue(source.contains("Email ID: " + username + "@computing.smu.edu.sg"));
        }
    }

    @Test
    void sameSeedGivesIdenticalDonorsAndRoster() throws Exception {
        Properties config = prepareInputs();
        Path first = directory.resolve("a");
        Path second = directory.resolve("b");

        DemoCohortGenerator.generate(config, directory, first, 8, 7);
        DemoCohortGenerator.generate(config, directory, second, 8, 7);

        for (String name : List.of("donors.csv", "IS442-ScoreSheet.csv")) {
            assertEquals(Files.readString(first.resolve(name)), Files.readString(second.resolve(name)));
        }
    }

    @Test
    void demoPropertiesDiffersFromConfigOnlyInOutputAndInputPaths() throws IOException {
        Properties config = load("config.properties");
        Properties demo = load("demo.properties");

        Set<String> keys = new TreeSet<>(config.stringPropertyNames());
        keys.addAll(demo.stringPropertyNames());
        Set<String> differing = new TreeSet<>();
        for (String key : keys) {
            if (!Objects.equals(config.getProperty(key), demo.getProperty(key))) {
                differing.add(key);
            }
        }
        assertEquals(new TreeSet<>(ALLOWED_DIFFERENCES), differing);
        assertEquals("build/demo-cohort/submissions", demo.getProperty("input.directory"));
        assertTrue(demo.getProperty("grading.directory").startsWith("build/demo-run/"));
    }

    private Properties prepareInputs() throws IOException {
        Path input = Files.createDirectories(directory.resolve("in"));
        Files.writeString(directory.resolve("sheet.csv"), SHEET_HEAD);
        writeDonor(input.resolve("2023-2024-ping.lee.2023.zip"), "", "/*\r\n * Name: Ping\r\n"
                + " * Email ID: ping.lee.2023\r\n */\r\nclass Q1a {}\r\n");
        writeDonor(input.resolve("2023-2024-chee.teo.2022.zip"), "wrapper/", "class Q1a {}\r\n");
        Properties config = new Properties();
        config.setProperty("input.directory", "in");
        config.setProperty("score.sheet.path", "sheet.csv");
        config.setProperty("identity.archive.username.regex",
                "^(?:2023-2024-)?([a-z]+(?:\\.[a-z]+)*\\.\\d{4})\\.zip$");
        config.setProperty("question.directories.required", "Q1,Q2,Q3");
        return config;
    }

    private static void writeDonor(Path zip, String wrapper, String q1Source) throws IOException {
        Map<String, String> files = Map.of(
                wrapper + "Q1/Q1a.java", q1Source,
                wrapper + "Q2/data.txt", "data",
                wrapper + "Q2/.DS_Store", "junk",
                "__MACOSX/" + wrapper + "Q3/x", "junk",
                wrapper + "Q3/Q3.java", "class Q3 {}");
        try (OutputStream output = Files.newOutputStream(zip);
                ZipOutputStream archive = new ZipOutputStream(output)) {
            for (Map.Entry<String, String> file : new TreeMap<>(files).entrySet()) {
                archive.putNextEntry(new ZipEntry(file.getKey()));
                archive.write(file.getValue().getBytes(StandardCharsets.ISO_8859_1));
                archive.closeEntry();
            }
        }
    }

    private static Properties load(String file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(file))) {
            properties.load(input);
        }
        return properties;
    }
}
