package autograder.report;

/** File names of the reports written into each run folder, shared by writers, readers and config checks. */
public final class ReportFiles {
    public static final String CASES = "cases.csv";
    public static final String SUBMISSIONS = "submissions.csv";
    public static final String SIMILARITY = "similarity.csv";
    public static final String STARTER_ANALYSIS = "starter-analysis.csv";
    public static final String SANITY_CHECKS = "sanity-checks.csv";

    private ReportFiles() {
    }
}
