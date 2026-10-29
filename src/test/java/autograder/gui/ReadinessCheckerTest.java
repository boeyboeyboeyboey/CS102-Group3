package autograder.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadinessCheckerTest {
    @TempDir
    Path temporaryDirectory;

    private final ReadinessChecker checker = new ReadinessChecker();

    @Test
    void readyInputsLoadSettingsAndCountSubmissions() throws Exception {
        Readiness readiness = checker.check(new TestSetup(temporaryDirectory).inputs());

        assertTrue(readiness.ready());
        assertNotNull(readiness.settings());
        assertEquals("6 submissions found",
                readiness.status(InputField.SUBMISSIONS).orElseThrow().message());
        assertEquals("Will be created", readiness.status(InputField.OUTPUT).orElseThrow().message());
    }

    @Test
    void outputUnderWritableParentIsNotCreated() throws Exception {
        TestSetup setup = new TestSetup(temporaryDirectory);

        checker.check(setup.inputs());

        assertFalse(Files.exists(setup.output));
        assertFalse(Files.exists(setup.output.getParent()));
    }

    @Test
    void missingSubmissionsFolderIsReported() throws Exception {
        TestSetup setup = new TestSetup(temporaryDirectory);
        GuiInputs inputs = new GuiInputs(setup.config, temporaryDirectory.resolve("nope"),
                setup.scoreSheet, setup.output);

        Readiness readiness = checker.check(inputs);

        assertFalse(readiness.ready());
        assertEquals("Folder not found", readiness.status(InputField.SUBMISSIONS).orElseThrow().message());
        assertTrue(readiness.status(InputField.SCORE_SHEET).orElseThrow().ok());
    }

    @Test
    void folderWithoutZipsIsReported() throws Exception {
        TestSetup setup = new TestSetup(temporaryDirectory);
        Path empty = Files.createDirectories(temporaryDirectory.resolve("empty"));
        Files.writeString(empty.resolve("notes.txt"), "x");

        Readiness readiness = checker.check(new GuiInputs(setup.config, empty,
                setup.scoreSheet, setup.output));

        assertEquals("No student .zip files found",
                readiness.status(InputField.SUBMISSIONS).orElseThrow().message());
        assertFalse(readiness.ready());
    }

    @Test
    void missingScoreSheetIsReported() throws Exception {
        TestSetup setup = new TestSetup(temporaryDirectory);

        Readiness readiness = checker.check(new GuiInputs(setup.config, setup.submissions,
                temporaryDirectory.resolve("none.csv"), setup.output));

        assertEquals("File not found", readiness.status(InputField.SCORE_SHEET).orElseThrow().message());
    }

    @Test
    void outputInsideSubmissionsFolderIsAnOutputError() throws Exception {
        TestSetup setup = new TestSetup(temporaryDirectory);

        Readiness readiness = checker.check(new GuiInputs(setup.config, setup.submissions,
                setup.scoreSheet, setup.submissions.resolve("results")));

        FieldStatus output = readiness.status(InputField.OUTPUT).orElseThrow();
        assertFalse(output.ok());
        assertEquals("Output can't be inside the submissions folder", output.message());
        assertFalse(readiness.ready());
    }

    @Test
    void outputOverlappingProtectedInputIsMappedToOutputByTheLoader() {
        FieldStatus status = ReadinessChecker.mapFailure(new IllegalArgumentException(
                "grading directory overlaps another input/output: /x"));

        assertEquals(InputField.OUTPUT, status.field());
        assertFalse(status.ok());
    }

    @Test
    void messagelessFailureFallsBackToClassName() {
        FieldStatus status = ReadinessChecker.mapFailure(new IllegalStateException());

        assertEquals(InputField.CONFIG, status.field());
        assertEquals("IllegalStateException", status.message());
    }

    @Test
    void brokenConfigReportsOnlyTheConfigField() throws Exception {
        TestSetup setup = new TestSetup(temporaryDirectory);

        Readiness readiness = checker.check(new GuiInputs(temporaryDirectory.resolve("missing.properties"),
                setup.submissions, setup.scoreSheet, setup.output));

        assertEquals(1, readiness.statuses().size());
        assertEquals(InputField.CONFIG, readiness.statuses().get(0).field());
        assertFalse(readiness.ready());
    }
}
