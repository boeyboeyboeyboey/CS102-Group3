FEATURE 1: Token-Based Plagiarism Detection (AST / Token N-Gram Engine) (Kiya)
================================================================================
Goal:
Detect code plagiarism and logic copying across student submissions, ensuring resistance against superficial obfuscation such as renaming variables, changing comment styles, or altering formatting[cite: 1, 2].

Architecture & OOP Design:
- Package: autograder.service.analysis
- Create an interface PlagiarismDetector with method:
  PlagiarismReport analyzeCohort(List<Submission> submissions)
- Implement TokenWinnowingPlagiarismDetector implementing PlagiarismDetector.
- Integration: Plugs into the grading pipeline after all student zip archives are extracted and normalized, running cross-submission analysis before final report export.

Technical Implementation Details:
1. Tokenization Pipeline: Read each student's Java source files and convert them into a normalized token stream (using Java's StreamTokenizer, regex, or a lightweight lexer).
   - Strip all comments, block headers, and whitespace.
   - Replace user-defined variable, method, and class names with a generic token (e.g., `ID`).
   - Retain language keywords, operators, and control flow tokens (`for`, while, if, return, +, `==`).
2. Winnowing / N-Gram Comparison:
   - Generate n-grams of tokens (e.g., k=5) and hash them into fingerprinted sequences.
   - Apply the Winnowing algorithm (or Jaccard similarity index) to compute pairwise similarity percentages between every student pair.
3. Configuration (`config.properties`):
   - plagiarism.similarity.threshold=0.75 (flags pairs exceeding 75% similarity).
   - plagiarism.kgram.size=5
4. Output:
   - Generate a pairwise matrix and flag suspicious pairs with file names and matched token ranges.
   - Feed flagged incidents into the anomaly reporting system[cite: 1, 2].

--------------------------------------------------------------------------------

================================================================================
FEATURE 2: Interactive HTML/Dashboard Report Generator
================================================================================
Goal:
Generate an offline, zero-dependency interactive HTML dashboard (`reports/grading_report.html`) alongside the required CSV export[cite: 1, 2]. This satisfies instructor usability and Nielsen's heuristic of clear status visibility[cite: 1, 2].

Architecture & OOP Design:
- Package: autograder.report
- Create an interface ReportExporter with method:
  void export(GradingContext context, Path outputPath)
- Implement HtmlDashboardReportExporter alongside the existing CsvScoreSheetExporter.
- Integration: Invoked at the end of the batch grading pipeline using the aggregated BatchGradingResult model.

Technical Implementation Details:
1. Self-Contained Template: Emit a single, self-contained HTML file with embedded modern CSS and lightweight vanilla JavaScript (no external CDN dependencies so it opens offline on any instructor laptop)[cite: 1, 2].
2. Dashboard Components:
   - Cohort Summary: Total submissions, average score, median score, total execution time, and anomaly count[cite: 1, 2].
   - Visual Charts: SVG-based score distribution bar chart and question pass-rate breakdown (Q1a, Q1b, Q2a, Q2b, Q3)[cite: 1, 2].
   - Filterable Results Table: Searchable table of students with sorting by score, status, and detected anomalies[cite: 1, 2].
   - Expandable Detail Drawers: Clicking a student row expands to reveal raw compilation errors, test failure logs, and runtime metrics.
3. Configuration (`config.properties`):
   - report.html.enabled=true
   - report.html.output.path=Project/grading_report.html

--------------------------------------------------------------------------------
FEATURE 3: Starter Code / Boilerplate Diff Detector (Sera)
================================================================================
Goal:
Detect submissions where students submitted unmodified starter skeletons, accidentally included default template files without logic, or made zero functional progress[cite: 1, 2].

Architecture & OOP Design:
- Package: autograder.service.submission
- Create an interface SubmissionIntegrityChecker with method:
  IntegrityResult checkIntegrity(Submission submission, Path starterTemplateDir)
- Implement TemplateDiffIntegrityChecker.
- Integration: Runs immediately after archive extraction during structural anomaly validation[cite: 1, 2].

Technical Implementation Details:
1. Template Baseline: Ingest the reference starter folder (`Project/RenameToYourUsername`) as a ground-truth baseline template[cite: 1, 2].
2. Normalization & Comparison:
   - Read corresponding Java files (e.g., student Q1a.java vs starter `Q1a.java`).
   - Strip student identification headers (Name, Email ID) and blank lines from both files[cite: 1, 2].
   - Calculate line-level diffs or Levenshtein edit distance between the student logic and the starter code.
3. Detection Logic:
   - If a file is 100% identical to the template: Flag with severity `CRITICAL: UNTOUCHED_STARTER_FILE`[cite: 1, 2].
   - If file change is under a configurable threshold (e.g., < 3 lines of functional change): Flag with severity WARNING: MINIMAL_IMPLEMENTATION.
4. Output: Record findings as non-fatal StructuralAnomaly events, logging them to the CLI and reports without crashing the pipeline[cite: 1, 2].
5. Configuration (`config.properties`):
   - integrity.starter.path=Project/RenameToYourUsername
   - integrity.min.edit.distance=5

--------------------------------------------------------------------------------

================================================================================
FEATURE 4: Dynamic Parameterized Test Case Ingestion (William)
================================================================================
Goal:
Mitigate the risk of students hardcoding return values to pass static test inputs[cite: 1, 2]. Allow dynamic test case injection and randomized test seed values without modifying original tester files on disk[cite: 1, 2].

Architecture & OOP Design:
- Package: autograder.service.testing
- Create an interface TestCaseAdapter with method:
  Path generateExecutableTestSuite(QuestionConfig question, Path originalTester, Path scratchDir)
- Implement ParameterizedTestAdapter implementing TestCaseAdapter.
- Integration: Used by CompilationService and ExecutionService when preparing the temporary per-case execution sandboxes.

Technical Implementation Details:
1. Test Parameterization Schema: Support externalizing test cases into a structured file (`testcases.properties` or data files) containing varied arguments and expected return values.
2. Dynamic Adapter Generation:
   - Instead of running a single monolithic test file, read the base tester class from `Project/Tester-Files/`[cite: 1, 2].
   - Generate temporary runner source files in an isolated scratch directory that programmatically instantiate student classes and inject varied seed data (e.g., randomized boundary cases, negative integers, alternative string fixtures)[cite: 1, 2].
3. Isolation & Compilation: Compile the generated adapter in the temporary workspace alongside student code, run it within the JVM execution timeout, and parse outputs cleanly via markers (`AUTOGRADER_SCORE=X`).
4. Configuration (`config.properties`):
   - testing.parameterized.enabled=true
   - testing.random.seed=42
   - `testing.case.timeout.seconds=5`[cite: 1, 2]

--------------------------------------------------------------------------------
FEATURE 5: Granular Student Feedback Markdown Generator
================================================================================
Goal:
Generate individual, transparent feedback files for each student (`reports/feedback/<student_username>.md`) detailing exactly why marks were awarded or lost, including sanitized compiler messages, assertion failures, and runtime logs.

Architecture & OOP Design:
- Package: autograder.report
- Create an interface FeedbackGenerator with method:
  void generateFeedback(StudentResult result, Path outputDir)
- Implement MarkdownFeedbackGenerator implementing FeedbackGenerator.
- Integration: Hooked into the end of each student's grading lifecycle inside BatchGradingService.

Technical Implementation Details:
1. Capture Diagnostic Artifacts: For each student, collect:
   - Resolved identity and archive anomaly log (e.g., "Normalized folder structure from flat layout")[cite: 1, 2].
   - Per-question score breakdown (e.g., Q1a: 2/3, `Q2a: 5/5`)[cite: 1, 2].
   - Test case execution results: Status (PASS / FAIL / TIMEOUT / COMPILE_ERROR).
   - Sanitized error messages: Stack traces truncated to the first relevant student line, compiler errors stripped of host-system absolute file paths.
2. Markdown Formatting:
   - Output clean tables showing Question | Max | Awarded | Status.
   - Code blocks containing failed test inputs and expected vs actual values where available.
   - Clear diagnostic notes for timeouts (e.g., "Execution exceeded 5.0s limit - possible infinite loop")[cite: 1, 2].
3. Configuration (`config.properties`):
   - feedback.markdown.enabled=true
   - feedback.output.dir=Project/reports/feedback

--------------------------------------------------------------------------------

================================================================================
FEATURE 6: Interactive Terminal UI (TUI) Dashboard
================================================================================
Goal:
Transform the standard console output into a clean, interactive terminal dashboard that adheres to Nielsen's heuristic of "Visibility of System Status"[cite: 1, 2], showing real-time batch progress, pipeline stage badges, and live anomaly counters without relying on heavy graphical GUI frameworks[cite: 1, 2].

Architecture & OOP Design:
- Package: autograder.ui
- Create an interface GradingProgressMonitor with methods:
  void onBatchStart(int totalSubmissions)
  void onStudentStart(String studentId, int currentIndex)
  void onStageUpdate(String studentId, PipelineStage stage)
  void onAnomalyDetected(String studentId, Anomaly anomaly)
  void onStudentComplete(String studentId, StudentScore score)
  void onBatchComplete(BatchSummary summary)
- Implement TerminalDashboardUI implementing GradingProgressMonitor.
- Integration: Passed as an event listener into BatchGradingService.

Technical Implementation Details:
1. Rendering Engine: Use standard ANSI escape codes (`\033[...`) to control cursor positioning, clear lines, and render color badges (`[PASS]` in green, [FAIL] in red, [WARN] in yellow, [TIMEOUT] in magenta).
2. Live Visual Elements:
   - Progress Bar: Real-time progress bar across total cohort (`[=========>          ] 3/6 Submissions (50%)`).
   - Active Pipeline Indicator: Visual step progression (`[EXTRACT] -> [VALIDATE] -> [COMPILE] -> [TEST]`).
   - Live Anomaly Feed: Dedicated 4-line rolling viewport showing detected folder/naming anomalies as they happen in real time[cite: 1, 2].
3. Graceful Degradation: Inspect terminal capabilities; if running in a non-interactive shell, IDE sub-window, or Windows DOS prompt lacking ANSI support, fall back automatically to clean plain-text log statements.
4. Configuration (`config.properties`):
   - ui.tui.enabled=true
   - ui.ansi.colors=true
   - ui.refresh.rate.ms=100
================================================================================