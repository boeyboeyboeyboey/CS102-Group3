package autograder.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes one synthetic submission archive from donor question folders. */
final class SyntheticArchiveWriter {
    private static final long FIXED_TIME = 1_700_000_000_000L;

    private SyntheticArchiveWriter() {
    }

    static void write(Path zip, SyntheticRoster.Identity student, List<String> questions,
            List<DonorArchive> donors) throws IOException {
        try (OutputStream output = Files.newOutputStream(zip);
                ZipOutputStream archive = new ZipOutputStream(output)) {
            for (int i = 0; i < questions.size(); i++) {
                String question = questions.get(i);
                for (Map.Entry<String, byte[]> file : donors.get(i).files(question).entrySet()) {
                    byte[] content = file.getValue();
                    if (file.getKey().endsWith(".java")) {
                        content = rewrite(content, student);
                    }
                    ZipEntry entry = new ZipEntry(
                            student.username() + "/" + question + "/" + file.getKey());
                    entry.setTime(FIXED_TIME);
                    archive.putNextEntry(entry);
                    archive.write(content);
                    archive.closeEntry();
                }
            }
        }
    }

    private static byte[] rewrite(byte[] content, SyntheticRoster.Identity student) {
        String text = new String(content, StandardCharsets.ISO_8859_1);
        return HeaderRewriter.rewrite(text, student.headerName(), student.email(),
                student.username()).getBytes(StandardCharsets.ISO_8859_1);
    }
}
