package autograder.demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Question folder contents of one real submission, located regardless of wrapper folders. */
final class DonorArchive {
    private static final Pattern QUESTION = Pattern.compile("Q[0-9]+");
    private final String username;
    private final Map<String, Map<String, byte[]>> questions = new TreeMap<>();

    private DonorArchive(String username) {
        this.username = username;
    }

    static DonorArchive read(Path zip, String username, List<String> questionFolders)
            throws IOException {
        DonorArchive donor = new DonorArchive(username);
        try (ZipFile file = new ZipFile(zip.toFile())) {
            for (ZipEntry entry : Collections.list(file.entries())) {
                donor.addEntry(file, entry, questionFolders);
            }
        }
        return donor;
    }

    private void addEntry(ZipFile file, ZipEntry entry, List<String> questionFolders)
            throws IOException {
        String name = entry.getName();
        if (entry.isDirectory() || name.contains("__MACOSX") || name.endsWith(".DS_Store")) {
            return;
        }
        String[] segments = name.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if (QUESTION.matcher(segments[i]).matches()) {
                if (questionFolders.contains(segments[i])) {
                    String relative = String.join("/",
                            Arrays.copyOfRange(segments, i + 1, segments.length));
                    try (InputStream input = file.getInputStream(entry)) {
                        questions.computeIfAbsent(segments[i], key -> new TreeMap<>())
                                .put(relative, input.readAllBytes());
                    }
                }
                return;
            }
        }
    }

    String username() {
        return username;
    }

    Map<String, byte[]> files(String question) {
        return questions.getOrDefault(question, Map.of());
    }
}
