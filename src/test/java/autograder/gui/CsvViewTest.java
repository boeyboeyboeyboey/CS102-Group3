package autograder.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvViewTest {
    @Test
    void emptySimilarityAndSanityReportsSaySoPlainly() {
        assertEquals("No pairs reached the similarity threshold.",
                CsvView.emptyMessage(Path.of("run-1", "similarity.csv")));
        assertEquals("No sanity checks were flagged.",
                CsvView.emptyMessage(Path.of("run-1", "sanity-checks.csv")));
        assertEquals(CsvView.EMPTY, CsvView.emptyMessage(Path.of("run-1", "cases.csv")));
    }
}
