# Grading architecture

## What runs today

The entry point loads an external properties file, reads the supplied class list, processes the submission ZIPs, checks ownership and required files, prepares corrected working copies where safe, and reports problems. It then compiles each part separately, executes individual instructor cases, aggregates marks and exports the score sheet with diagnostic reports.

The archive and supplied inputs are evidence. Extraction output is a raw copy. Corrections belong in a separate working copy, with recorded reasons. Conflicting ownership must not be converted into a guessed grade. An extracted archive, a valid question, and a graded student are separate states.

## Responsibilities

- `App`: accepts the config filename, shows progress and the final summary
- `AppConfig`: immutable settings and public loading facade; `AppConfigLoader` coordinates loading, `AssessmentConfigLoader` reads assessment definitions, `PropertyParsers` validates values, and `ConfigPathPolicy` protects input/output paths
- `AssessmentDefinition` / `QuestionDefinition`: describe the assessment without fixed Java question names or marks
- `ScoreSheetRepository`: validates the supplied class list and retains its original CSV data
- `SafeZipExtractor`: bounded ZIP extraction with unsafe path and corruption rejection
- `SubmissionProcessor`: coordinates the batch and records per-submission outcomes
- `SubmissionValidator`: selects an unambiguous question root and checks question folders
- `IdentityResolver`: coordinates evidence collection, roster matching, correction planning and working-copy application through focused collaborators
- `WorkingCopyPreparer`: creates a canonical copy under the roster username and applies approved identity repairs
- `CorrectionLog` / `AnomalyLog`: keep corrections separate from problems
- `SubmissionResult`: shares the original/working locations, identity, status, problems, and corrections
- `CsvCodec` / `CsvScoreReportWriter`: parse/write the supplied schema safely using the pipeline’s eligible computed scores
- `ProgressListener` / `ConsoleProgressListener`: separate progress events from their presentation
- `MarkdownFeedbackGenerator` / `FeedbackConfig` (feature 5, in review): write one Markdown feedback file per graded submission from recorded case results, preparation problems and corrections; a failed feedback file is reported without affecting grading or CSV exports

Small immutable data classes pass results between components. Lists exposed by result models cannot be modified by consumers. Composition keeps responsibilities separate. Interfaces allow later feature implementations to fit into the pipeline without changing its core responsibilities. Java class inheritance is not being added artificially; the supplied instructor tester classes themselves extend student question classes, which the later compilation design must respect.

## Modularisation boundaries

The public entry points remain stable. Implementation collaborators are package-private within their existing packages; shared domain models and bonus contracts retain their original signatures.

| Package | Coordinator | Focused collaborators |
| --- | --- | --- |
| `config` | `AppConfig.load` / `AppConfigLoader` | Assessment loading, property parsing and symlink-aware path policy |
| `submission` | `IdentityResolver` | `IdentityEvidenceCollector`, `RosterIdentityMatcher`, `HeaderCorrectionPlanner`, `HeaderCorrectionApplier`; copied `IdentityEvidence` transfer data |
| `grading` | `GradingPipeline` | Private per-submission/per-question execution helpers and `GradingDiagnosticsWriter` for completed CSV projections; eligibility stays in the pipeline |
| `report.html` | `OfflineHtmlReportGenerator` | Summary, table/details, cases and analysis renderers; one escaper, decimal formatter, submission presentation helper and immutable `QuestionSection` |
| `report.feedback` | `MarkdownFeedbackGenerator` | `MarkdownDiagnosticFormatter` for recorded diagnostics and `FeedbackFileWriter` for safe unique filenames/CREATE_NEW persistence |

The generator still embeds the same CSS and JavaScript. Report classes render completed evidence; they do not rerun cases, alter scores or decide export eligibility. The Markdown facade retains its existing NOT_RUN-derived notice because its public contract does not include the run's withholding map. Section composition remains a justified size exception; see [CODE_REVIEW.md](CODE_REVIEW.md) for checks and other exceptions.

## Bonus connections

Feature 3 implements `BoilerplateDetector` with `StarterCodeDetector` in `autograder.analysis.boilerplate`. It compares only configured answer sources and student-written companions, reading bounded UTF-8 inputs and reporting per-file failures without stopping other comparisons. `JavaSourceTokens` ignores comments/formatting outside literals and computes token edit distance. `BoilerplateConfig` validates external enablement, starter path, similarity threshold and resource limits. After grading/CSV export, the App hook attaches immutable findings to a new `BatchResult`, writes `starter-analysis.csv` and supplies those findings to the existing HTML renderer. Grades, withholding, shared interfaces and the grading pipeline are unchanged. High whole-file similarity is a review heuristic; Unicode escapes and unsupported/malformed input require manual comparison. Feature 3 is REVIEW pending Sera's acceptance; see [testing instructions](STARTER_CODE_TESTING.md).

| Contract | Input | Output |
| --- | --- | --- |
| PlagiarismDetector | Submissions and assessment | Similarity findings with evidence |
| HtmlReportGenerator | GradingReportContext (batch and withholding decisions), output directory | Generated report path |
| BoilerplateDetector | Submission and starter directory | Findings identifying the source file and reason |
| TestCaseProvider | Assessment | Independently runnable test definitions |
| FeedbackGenerator | Student grading results and output directory | Feedback file path |
| ProgressListener | Stage, item, completed count and total | Display update |

Feature 2 implements HTML reporting; Gabriel's feature 5 implements Markdown feedback and remains REVIEW after its merge into main. The other bonus implementations remain TODO. Sera and William retain ownership of their implementation logic. The current runners are explicit source resources; a TestCaseDefinition describes the runner class and arguments to execute, not a general Java source parser.

## Offline HTML reporting

After the pipeline has written its CSVs, `App` supplies the immutable `GradingReportContext` to the HTML interface. The context copies the run's authoritative withholding map and rejects unrelated submissions or empty reasons. Reporting does not decide eligibility or rerun tests. Summary averages and medians use exported submissions only; withheld computed scores remain labelled diagnostics. Missing roster submissions are not inferred from batch results, and empty analysis lists do not imply that an analysis ran.

`HtmlReportConfig` loads enablement and a safe `.html` basename from external properties. Generation defaults to disabled when older configurations omit these settings. `OfflineHtmlReportGenerator` encapsulates rendering in `autograder.report.html`, with classpath assets embedded into one UTF-8 file. Student-controlled text is escaped for HTML; filtering and sorting operate on rendered DOM data. The report opens directly from disk without external dependencies. An enabled report-write failure returns a nonzero CLI status after printing completed CSV locations; it does not remove those reports.

Feature 2 is DONE after Boey's clean-build/run and displayed-dashboard acceptance. See [offline_html_dashboard_architecture.md](../offline_html_dashboard_architecture.md) for implementation details, actual OOP principles and Slide Deck Talking Points.

Boey approved a question heatmap revision on 2026-10-12. A package-private immutable `QuestionOutcome` assembles report-local scores, status counts, case-count evidence and a credit-band classification from existing models. The renderer uses it in the existing question columns and groups recorded diagnostics by configured question order. Safe ordinal/index anchors let browser code open and focus the matching group without changing active controls. Missing/mismatched evidence and all-NOT_RUN questions receive neutral treatment; every recorded case remains available, including unexpected question IDs. This revision changes presentation only: the shared models, eligibility decisions, CSV reports and pipeline contracts retain their existing responsibilities.

## Similarity analysis (feature 1, REVIEW)

The merged Kiya implementation tokenizes Java source and composes token, n-gram and similarity helpers under the plagiarism package. The detector integration preserves the two-argument GradingPipeline constructor, which delegates to a no-op detector, and the three-argument GradingPipeline overload, which accepts an injected PlagiarismDetector. During pipeline integration, compare once after grading using the original full submission list and assessment. Similarity findings are written to their dedicated CSV after submissions.csv and before score export. This optional analysis does not affect scoring or export eligibility; I/O or runtime comparison failures are reported and grading continues with no findings. See [PR5_MERGE_RESOLUTION.md](PR5_MERGE_RESOLUTION.md) for the exact CSV schema, conflict record and verification status.

## Scoring foundation

`TestCaseDefinition` names a globally unique test, question, runner class, arguments, and configured maximum points. `TestCaseResult` distinguishes pass, fail, timeout, compilation error, runtime error, and not-run. Only a passed case earns its configured points. `StudentGrade` sums recorded case scores using decimal arithmetic and rejects duplicate case identifiers. `BatchResult` groups assessment, grades and analysis findings for report features.

The pipeline records all expected cases, including NOT_RUN for withheld submissions and COMPILE_ERROR for each case in an uncompiled part. Unique roster matches with name warnings can be graded; unresolved identities, duplicate submissions, failed preparation and grading infrastructure failures withhold export. Omitted rows retain the template's numerator. A separate `submissions.csv` records the export decision and warnings; `cases.csv` preserves diagnostics. Scores use the existing decimal aggregation and CSV writer.

## Execution design

`InstructorTestCaseProvider` validates the fixed five question IDs, original tester content and case counts, then supplies 20 definitions with configured weights. Five explicit Java adapter resources preserve the original inputs, expected values and comparisons. Original testers stay unchanged. This avoids a brittle Java-text splitter and the extra complexity of an AST rewriter. New or edited testers require reviewed adapters; this is not William's dynamic case feature.

`DynamicTestCaseProvider` supplies five deterministic property-check cases per question. `CombinedTestCaseProvider` keeps each question's existing maximum and distributes it across the fixed and dynamic cases, including a final decimal remainder so the five question maxima still sum to 20. Dynamic cases therefore affect scores without increasing the score-sheet denominator.

`StudentCompiler` creates an isolated source/class workspace per part, copies only the selected answer and required companions, adds supplied support classes and the adapter, and launches javac with annotation processing disabled. Student compiled classes are never reused. A failed part leaves other questions and students independent.

`CaseExecutor` creates a fresh directory and fixture copies per case and starts a new JVM. The adapter writes PASSED, FAILED or ERROR to a dedicated result file after evaluating the assertion. The parent requires a successful process exit and a valid result; printed text has no scoring authority. Missing/invalid results are runtime errors. Timeout takes precedence over any written result. Expected exceptions in Q2b still pass their corresponding cases; unexpected exceptions retain zero marks and runtime diagnostics.

`ProcessRunner` closes stdin, drains both output pipes concurrently while limiting stored bytes, enforces the configured deadline, and attempts process/descendant cleanup. Interruptions propagate rather than being treated as student failures. `GradingPipeline` distinguishes infrastructure I/O errors from student test failures, preserves remaining case records and withholds affected exports.

`ExecutionConfig` loads tool paths, compilation/case deadlines, output capture size, grading directory and report name. It rejects overlap with configured inputs and preparation outputs. Each grading run is a fresh directory so a previous report cannot be mistaken for this run's output.

These processes are not a security sandbox. The supported setting is controlled course grading; hostile code can use the current account's privileges, tamper with files/protocol or launch children. Strong adversarial isolation must be supplied externally. Descendant cleanup is best effort where the OS permits enumeration and termination. Fixed adapters also intentionally reset student static state per case to make timeouts fair; a stateful answer can differ from the original tester's single-process run.

## Configuration and simplicity

Follow [CODING_STYLE.md](CODING_STYLE.md) for class/method responsibilities and size review guidelines. [CODE_REVIEW.md](CODE_REVIEW.md) records the merged-main review and proposed extraction boundaries; M1-M5 were implemented and checked before PR #1 integration; the current PR #5 merge verification and human acceptance are pending. See [PR5_MERGE_RESOLUTION.md](PR5_MERGE_RESOLUTION.md).

Externalize settings the instructor or team may change: paths, assessment identifiers, required files, case counts, marks, matching rules, extraction limits, and execution limits. Language/protocol mechanics such as zero-based indexes, CSV quoting characters, and a success exit code are not configurable grading policy. Keep comments minimal, lowercase, and without full stops; agents must not add AI attribution comments.

## Dependency choices

Java 17 standard library provides file/ZIP/process primitives. JUnit Jupiter is a test-only dependency; there are no third-party application runtime libraries in the current foundation. Maven compiler, test, and JAR plugins build the application. JUnit is EPL-2.0 and the selected Apache Maven plugins use Apache-2.0; the final team release must verify the complete dependency/license inventory if libraries change.
