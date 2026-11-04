package autograder.gui;

import autograder.grading.GradingRun;
import autograder.model.AssessmentDefinition;
import autograder.model.BatchResult;
import autograder.model.QuestionDefinition;
import autograder.model.StudentGrade;
import autograder.model.StudentIdentity;
import autograder.model.SubmissionResult;
import autograder.model.SubmissionStatus;
import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultsTableModelTest {
    @Test
    void exposesStudentScoreAndNote() {
        ResultsTableModel model = new ResultsTableModel(List.of(
                new ResultsTableModel.Row("Chee", 12.0, ""),
                new ResultsTableModel.Row("Xing", 1.0, "Duplicate submission")));
        assertEquals(2, model.getRowCount());
        assertEquals(3, model.getColumnCount());
        assertEquals("Student", model.getColumnName(0));
        assertEquals("Score", model.getColumnName(1));
        assertEquals("Note", model.getColumnName(2));
        assertEquals("Xing", model.getValueAt(1, 0));
        assertEquals(1.0, model.getValueAt(1, 1));
        assertEquals("Duplicate submission", model.getValueAt(1, 2));
        assertEquals(Double.class, model.getColumnClass(1));
    }

    @Test
    void fromBlanksScoreAndLabelsNoteForWithheldStudents() {
        SubmissionResult graded = submission("graded.zip");
        SubmissionResult withheld = submission("withheld.zip");
        TestCaseDefinition test = new TestCaseDefinition("Q1a-1", "Q1a", "Runner", List.of(), 1);
        StudentGrade gradedGrade = new StudentGrade(graded,
                List.of(new TestCaseResult(test, TestStatus.PASSED, 10, "", "")));
        StudentGrade withheldGrade = new StudentGrade(withheld,
                List.of(new TestCaseResult(test, TestStatus.PASSED, 10, "", "")));
        QuestionDefinition question = new QuestionDefinition("Q1a", "Q1", "Q1a.java", Path.of("Q1aTester.java"),
                1, 1, List.of(), List.of(), List.of());
        AssessmentDefinition assessment = new AssessmentDefinition("exam", "Exam", List.of(question));
        GradingRun run = new GradingRun(
                new BatchResult(assessment, List.of(gradedGrade, withheldGrade), List.of(), List.of()),
                Path.of("run"),
                Map.of(withheld, "Duplicate submission"));

        ResultsTableModel model = ResultsTableModel.from(run);

        assertEquals(2, model.getRowCount());
        assertEquals(Double.valueOf(gradedGrade.getTotalScore()), model.getValueAt(0, 1));
        assertEquals("", model.getValueAt(0, 2));
        assertNull(model.getValueAt(1, 1));
        assertEquals("Withheld: Duplicate submission", model.getValueAt(1, 2));
    }

    private static SubmissionResult submission(String archive) {
        StudentIdentity identity = new StudentIdentity("", "", "", List.of(), List.of(), List.of(),
                null, "test fixture", List.of());
        return new SubmissionResult(Path.of(archive), null, null, null, identity,
                List.of(), List.of(), List.of(), SubmissionStatus.READY, true);
    }
}
