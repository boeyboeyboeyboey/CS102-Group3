package autograder.api;

/** Stage names passed to {@link ProgressListener#onProgress}. */
public final class ProgressStages {
    public static final String INGESTION = "ingestion";
    public static final String GRADING = "grading";
    public static final String REPORTING = "reporting";

    private ProgressStages() {
    }
}
