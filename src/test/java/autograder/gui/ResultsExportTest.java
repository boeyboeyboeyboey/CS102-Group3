package autograder.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsExportTest {
    private static final String CSV = "Name,Score\r\nchee,11.5";

    @TempDir
    Path root;

    private Artifact scores() throws Exception {
        Path run = Files.createDirectories(root.resolve("run"));
        return new Artifact("Scores", Files.writeString(run.resolve("a.csv"), CSV), Artifact.View.TABLE);
    }

    @Test
    void exportsEachCsvAndOneWorkbookOfTableReports() throws Exception {
        Path html = Files.writeString(root.resolve("d.html"), "<html>");
        Path out = Files.createDirectories(root.resolve("out"));

        ResultsExport.write(List.of(new Artifact("Dashboard", html, Artifact.View.EXTERNAL), scores()), out);

        assertEquals(CSV, Files.readString(out.resolve("a.csv")));
        assertFalse(Files.exists(out.resolve("d.html")));
        try (ZipFile zip = new ZipFile(out.resolve(ResultsExport.WORKBOOK_NAME).toFile())) {
            assertNotNull(zip.getEntry("xl/worksheets/sheet1.xml"));
            assertNull(zip.getEntry("xl/worksheets/sheet2.xml"));
        }
    }

    @Test
    void exportingIntoTheRunFolderKeepsItsCsvs() throws Exception {
        Artifact scores = scores();
        Path run = scores.path().getParent();

        assertEquals(0, ResultsExport.filesToReplace(List.of(scores), run));
        ResultsExport.write(List.of(scores), run);

        assertEquals(CSV, Files.readString(scores.path()));
        assertTrue(Files.exists(run.resolve(ResultsExport.WORKBOOK_NAME)));
    }

    @Test
    void countsOnlyFilesThatWouldBeReplaced() throws Exception {
        Path out = Files.createDirectories(root.resolve("out"));
        Files.writeString(out.resolve("a.csv"), "old");
        Files.writeString(out.resolve(ResultsExport.WORKBOOK_NAME), "old");

        assertEquals(2, ResultsExport.filesToReplace(List.of(scores()), out));
    }

    @Test
    void exportNeedsAtLeastOneTable() {
        Artifact folder = new Artifact("Run folder", root, Artifact.View.EXTERNAL);
        assertFalse(ResultsExport.available(List.of(folder)));
        assertTrue(ResultsExport.available(List.of(new Artifact("Scores", root, Artifact.View.TABLE))));
    }
}
