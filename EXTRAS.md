================================================================================
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