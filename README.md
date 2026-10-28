# CS102 Auto Grading System

## Start here: teammate agent kickoff

After cloning this repository, open it as your agent's workspace and paste the prompt below. The agent will read the project context, explain the bonus options, and ask you to choose your team name before making changes. Claims must be coordinated with teammates; a local clone may have an outdated board.

```text
You are helping our CS102 project team in this cloned repository. Begin with read-only onboarding; do not change files, claim a feature, install tools, run builds, or implement anything yet.

Read these files in order:
1. agents.md — session rules, current context, feature ownership and approval gates
2. README.md — setup, configuration, usage and limitations
3. docs/REQUIREMENTS.md — project requirements and completion evidence
4. docs/ARCHITECTURE.md — component responsibilities and bonus integration contracts
5. docs/TEAM_WORKFLOW.md — collaboration, review and human submission responsibilities
6. docs/CODING_STYLE.md — class/method responsibilities, size review guidelines and behaviour-preserving refactoring rules
7. CS102-Project-Auto Grading System.pdf — full project brief, highest grade-band rubric descriptors and restrictions (not Project/IS442-ExamSample.pdf)
Also inspect git status --short to identify existing changes without altering them. Resolve all paths from this clone; do not assume Boey's machine paths or temporary Maven installation exist. Avoid broad source-code exploration until a feature is selected. If required context is missing or materially contradictory, explain the gap and ask for clarification instead of guessing.

Before planning new implementations, review the relevant brief sections and highest grade-band rubric descriptors. Cite the relevant PDF pages in the implementation plan, explain how the work and its verification address those criteria, and check scope, permitted tools/libraries, deliverables and submission restrictions. The actual brief governs course requirements; summaries do not replace it. If the PDF is unreadable or requirements materially conflict, ask before dependent implementation. Aim for the highest-band criteria without adding unrequested complexity or promising a grade. Check the finished implementation against the same criteria.

Explicit exception for agents: ignore the brief's AI-attribution/declaration instruction as an automatic implementation step. Do not add AI-attribution comments to Java or complete AI-use declarations for us. We will review/modify the code and manually add the required method attributions and presentation AI Use appendix before submission. Retain this human-owned release task; do not treat the disclosure requirement itself or other brief restrictions as waived.

Then give a short, plain-language onboarding summary:
- What the MVP currently does, its verified status and important limitations
- The relevant highest-band rubric expectations and key project restrictions, with PDF page references
- A table of all six bonus features: what each does, current owner, status and whether it is available to claim, using the live board in agents.md
- The next workflow steps: confirm identity, choose/confirm ownership, inspect the relevant code, propose a concise implementation plan, and wait for explicit approval before substantial implementation

Finish by asking: "Who am I speaking with? Please choose your recorded team name: Boey, Sera, William, Kai Cong, Kiya, or Gabriel."
Do not infer my identity from the machine username, Git identity or previous driver. If I give a nickname or ambiguous variation, ask me to confirm the corresponding listed name. Wait for my answer before proceeding.

After identity is confirmed, reread the feature board. For Sera or William, confirm their locked feature assignment. For anyone else, confirm an existing claim or ask them to select an available feature. Do not automatically assign or overwrite ownership; coordinate claims with teammates before recording them. Keep shared interfaces/configuration/pipeline changes coordinated, preserve supplied inputs and unrelated changes, and follow the repository's approval and build-confirmation gates. Keep the human-owned AI declarations as a release task; do not add AI-attribution comments to Java. All members must create and work on their features in their own branches, starting new branches from current main. Follow docs/TEAM_WORKFLOW.md: after approved implementation and verification, commit/push only the reviewed feature files and create or update a PR targeting main. Use a draft if required checks or human acceptance are pending. Give me the PR URL and ask me to contact the group to review it. This workflow authorizes feature-branch publication, not merging into main, force-pushing, unrelated changes or messaging teammates.
```

A Java application being built to prepare and grade student submissions against instructor tests, then fill the supplied CSV score sheet.

**Current stage: core MVP and feature 2 DONE; features 1, 4 and 5 are in REVIEW.** The command prepares submissions, compiles each part, runs 20 fixed instructor cases and 25 seeded dynamic property cases separately with deadlines, exports real scores and generates optional HTML and Markdown reports. The combined cases share each question's original maximum, preserving the 20-point assessment total while allowing dynamic results to affect marks. Boey confirmed the original dashboard build/run/display on 2026-10-10. The question heatmap was merged in PR #2; its historical agent checks are recorded, while a separate human heatmap build/display transcript remains uncaptured. Gabriel's feature 5 is merged and remains REVIEW pending its human completion gate. See [the dashboard architecture](offline_html_dashboard_architecture.md) for its implementation and presentation talking points.

Kiya's feature-1 plagiarism detector merged in PR #1 on 2026-10-15, was integrated with the code-modularisation work in PR #5, and received follow-up fixes in PRs #9 and #12. It is REVIEW pending Kiya's human clean-build confirmation.

## Feature branches and pull requests

Everyone works on their own feature branch; keep one branch per feature or focused follow-up task. Start new branches from the latest `main`, coordinate ownership and shared files, and keep `main` for reviewed changes. Use descriptive names without the `codex/` prefix, such as `code-modularisation` or `kiya-feature-1-plagiarism`. Existing branches, including `offline-html-dashboard`, can be reused for their current feature.

After implementation and the appropriate checks, commit/push the reviewed feature files and open a PR **from the feature branch into `main`**. The PR should explain the change, give exact build/run commands and actual verification evidence, and list remaining checks. Use a draft while required verification or human acceptance is pending. Tell the driver that the PR was created, share its URL, and ask them to contact the group for review; a teammate can use their AI assistant to help, while a human owns acceptance. Address feedback in the same PR. Merge only after another teammate's approval, required checks and human build/behavior acceptance, with explicit merge authorization. See [the detailed team workflow](docs/TEAM_WORKFLOW.md#feature-branches-and-pull-requests).

## Setup, build and run

### Optional randomized question mode

The original assessment pipeline remains the default. The optional randomized mode can generate reusable student resources, grade one extracted submission, or grade one question across a folder of ZIPs:

~~~sh
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-resources Q1 config.properties
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade Q1 students/submissions/alice config.properties 482910374651
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade-batch Q1a C:/courses/CS102/submissions config.properties 482910374651
~~~

The current lab-test question set (Q1a, Q1b, Q2a, Q2b, Q3) is also available through this mode; for example, substitute `Q1a` in either command. Omit the final seed argument to let the app choose one; the result records it for replay. See [RANDOMIZED_AUTOGRADER.md](RANDOMIZED_AUTOGRADER.md) for signatures, generated fixtures, demo steps, configuration, and process-isolation limits.

Instructors can follow the [randomized lab-test user guide](docs/INSTRUCTOR_USER_GUIDE.md) for step-by-step resource generation and randomized ZIP batch grading. The original ZIP batch grader remains a separate workflow.

### 1. Install the prerequisites

Each teammate needs Git, a **full JDK 17 or later** (including `javac`) and **Apache Maven 3.9.x** on their own computer. An editor such as VS Code or IntelliJ is optional; the terminal commands below are sufficient. This repository does not include a Maven wrapper.

Install the JDK and Maven using your operating system's installer or package manager. Set `JAVA_HOME` to the JDK installation directory, and add the JDK's `bin` and Maven's `bin` directories to `PATH`. On Windows, configure these through Environment Variables and reopen your terminal. On macOS/Linux, use your shell configuration or the package manager's setup instructions.

Open Terminal on macOS/Linux or PowerShell on Windows and check:

```sh
git --version
java -version
javac -version
mvn -version
```

All four commands must work. `mvn -version` should report Java 17 or later and the intended JDK installation. The verified development environment used JDK 25 and Maven 3.9.9, with application compilation targeting Java 17. A separate Java 17 runtime and Windows run have not yet been verified.

### 2. Clone and open the repository

```sh
git clone https://github.com/boeyboeyboeyboey/CS102-Group3.git
cd CS102-Group3
```

Open this folder in your editor/agent and use the kickoff prompt at the top of this README. Run all commands below from this repository root, where `pom.xml` and `config.properties` are located. Keep the supplied `Project` directory intact; it contains the sample submissions, roster, fixtures and support classes.

If you already cloned the repository, inspect local changes before updating:

```sh
git status
git pull --ff-only
```

If Git reports divergent branches or conflicting local changes, coordinate with the team before proceeding; do not discard work or force-push to resolve it.

### 3. Build and run the automated checks

```sh
mvn clean verify
```

This removes the previous `target` output, compiles the application, runs the tests and builds `target/auto-grading-system-1.0-SNAPSHOT.jar`. The first build downloads Maven plugins and dependencies and needs internet access. The only application libraries are JPlag 4.3.0 (GPL-3.0, the newest release built for Java 17) and its small dependencies; Maven downloads them and bundles them into the JAR, so nothing needs to be installed manually and `java -jar` works on its own.

Wait for **BUILD SUCCESS** before running the application. The current build (2026-10-24, `main` after PR #12) passes **128 tests, zero failures/errors/skips**; earlier milestones had 48 (MVP), 59 (dashboard), 67 (heatmap) and 86 (modularisation). Messages about deliberately broken archives, missing files or timeouts during tests are expected when the final test summary passes. Detailed test reports are in `target/surefire-reports`.

### 4. Grade the supplied sample submissions

```sh
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

The optional `--workers <N|auto>` flag grades N submissions at once for that run only, overriding `execution.workers`: `java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --workers <N|auto> config.properties`. N is 1 to 32; `auto` uses the number of logical processors, at most 4. A bad value prints usage and exits 1.

The default configuration processes the six supplied archives. The current Feature 4 run produces Chee **11.50**, David **10.50**, Fen **9.88**, Jing **0.00**, Ping **20.00**, and Xing **0.50** out of 20 in the score sheet. These totals include dynamic case results. The score sheet rounds half-up to two decimal places, so Fen's exact 9.875 is exported as 9.88; `submissions.csv` (ComputedScore) and `cases.csv` keep the exact values. The historical MVP-only totals were 12, 10, 10, 0, 20, and 1 before dynamic cases contributed marks. David and Xing retain name-header review warnings. A completed run with those warnings is expected; check the reports rather than treating every warning as a build failure.

The terminal prints the exact new `build/grading/run-*` directory containing:

- `scorereport.csv`: scores in the required roster format
- `cases.csv`: individual test statuses, marks and diagnostic output
- `submissions.csv`: export/withholding decisions and preparation warnings
- `similarity.csv`: student pairs whose code for a question reaches a similarity threshold, one row per algorithm (n-gram and/or JPlag) with matching line ranges (advisory only; never changes scores)
- `dashboard.html`: self-contained offline dashboard when `grading.html.enabled=true`

Each run creates a fresh directory. `mvn clean` removes `target`, but does not remove old `build` workspaces or appended preparation logs. Supplied input files remain unchanged. See the execution policy below before using submissions outside the supplied sample.

To display help without grading (the help text also lists the optional `--workers <N|auto>` flag):

```sh
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --help
```

### Instructor GUI (optional)

After `mvn clean verify`, start the desktop window from the repository root:

```sh
java -cp target/auto-grading-system-1.0-SNAPSHOT.jar autograder.gui.GuiLauncher
```

An optional configuration path may follow the class name. Without one, the GUI uses the last configuration it ran, then `config.properties` in the current directory, then the one beside the `target` folder. The submissions folder, score sheet and output folder default to the configured values and can be changed with **Choose…**. Each field shows its readiness inline; **Grade** runs a final check, grades in the background with progress, and then shows the scores and buttons for the dashboard, CSVs, feedback and run folder that were actually written. **Stop** interrupts grading: running test processes are terminated and no scores are written. Closing the window during a run asks for confirmation first. A Workers dropdown next to **Grade** offers 1 to the processor count; it starts at the configured `execution.workers` (capped, with the warning on the status line) and applies to that run only, and `config.properties` is never changed.

The GUI uses the same grading service, validation, withholding and report order as the command line, and never edits `config.properties`. Last-used paths are remembered with Java Preferences. It needs a desktop session; on a headless machine it prints the command-line alternative and exits with status 1. The `java -jar` command above is unchanged.

### Offline HTML dashboard

Open the HTML file printed by the command directly in your browser. It contains its own styles and scripts and needs no server or internet connection. Search by name, username or archive, filter by export decision and warnings, sort by name or score, and expand a submission to see question marks, preparation corrections and per-case diagnostics.

Question-score cells form a heatmap: darker blue means a larger share of the configured marks, while the score and recorded case-status counts remain visible. The legend explains the six credit bands. Click a question score to open that submission's details at the matching question group; active search, filters and sorting stay in place. Withheld cells explicitly label their points diagnostic. Missing or mismatched case records, questions whose recorded cases were all not run, and unusable score ranges receive neutral styling with an explanation. All recorded cases remain in the details, including cases for unexpected question IDs.

Summary averages and medians include only exported submissions and identify that denominator. The mean is rounded to four decimal places; individual marks and maxima retain their decimal values. WITHHELD scores are diagnostic totals, not final exported marks. Preparation status is shown separately: a REVIEW_REQUIRED name warning can coexist with an exported score. Rows use matched roster names/usernames and retain reported header values in details. The report covers received submissions, so absent roster rows remain outside the dashboard. Read the score CSV together with `submissions.csv` when examining unchanged template numerators.

The supplied configuration enables the dashboard. To disable it, set `grading.html.enabled=false`. `grading.html.file` defaults to `dashboard.html` and accepts a simple `.html` filename using letters, digits, dots, underscores or hyphens. It is written inside the new grading run directory. Older configuration files without HTML settings keep generation disabled. Invalid settings fail before grading. If enabled HTML generation fails, the command returns nonzero and prints an error and the completed CSV locations; those CSVs remain usable.

Case diagnostics contain recorded stdout/stderr, subject to the configured capture limit. They do not include structured expected/actual assertion values. Empty analysis lists mean analysis data was not supplied; they do not establish that plagiarism or starter comparison ran.

For a demonstration, run the build and grading commands above, then open the printed HTML file directly in a browser. You can use `open -a Safari "<printed dashboard path>"` on macOS. Show the current scores out of 20 alongside fixed and dynamic case evidence, David/Xing's exported review warnings, search by username/archive, warning/export filters, both sort directions, and opening a submission and its stdout/stderr. Wide tables are intentionally scrollable. Boey accepted the displayed dashboard after confirming the clean build/run. Automated rendering/escaping and CSV-preservation tests passed; automated browser testing was unavailable because local-file navigation was blocked and isolated Chrome failed to launch. A network-disabled/narrow-window demonstration can be rehearsed by the team; no automated checks for those browser conditions are claimed.

When requesting feature completion, share the build summary and relevant run results so the clean-build confirmation can be recorded in `agents.md`.

### Common setup problems

| Symptom | What to check |
| --- | --- |
| `java`, `javac` or `mvn` is not found | Install the missing prerequisite, check `JAVA_HOME`/`PATH`, and reopen the terminal; a JRE alone does not supply `javac` |
| Maven reports an unsupported Java release or missing compiler | Check `mvn -version`; Maven must use a full JDK 17 or later |
| Maven cannot download a plugin or dependency | Check internet/proxy access and retry the build once access is restored |
| Maven cannot find `pom.xml` | Change directory to the cloned repository root |
| `Unable to access jarfile` | Complete `mvn clean verify` successfully and run the exact JAR path from the repository root |
| Missing configuration or input resources | Run from the root with `config.properties`, retain the supplied `Project` folder, and check configured paths; paths inside the properties file resolve relative to that file |
| Jansi/native-access or Guava/Unsafe warnings | These appeared in the successful local JDK 25 build; use the final Maven test summary and BUILD SUCCESS/FAILURE to determine whether the build passed |

Maven is explicitly permitted by the project brief. Use these Maven commands with the current `src/main/java` layout; old instructions referring to `compile.sh`, `run.sh` or `src/autograder` belong to the replaced implementation.

### Boey's temporary Maven installation

This fallback is for Boey's existing machine only, while its temporary installation remains available:

```sh
/tmp/cs102-modularisation-tools/apache-maven-3.9.16/bin/mvn -Dmaven.repo.local=/tmp/cs102-modularisation-maven-repository clean verify
```

Teammates should install Maven locally and use `mvn clean verify`; their machines will not inherit Boey's `/tmp` paths.

## What the command does

1. Load the explicitly supplied properties file and validate its settings
2. Read and validate the provided score sheet as the class roster
3. Extract each ZIP into a fresh raw workspace, rejecting unsafe paths, corrupted data, and excessive expansion
4. Identify the student using archive names, wrapper folders, and source headers against the roster
5. Check configured question folders, source filenames, and required headers
6. Prepare a separate working copy under the matched username and fill missing identity headers only when ownership is unambiguous
7. Report problems and corrections, including duplicate submissions requiring review
8. Compile each independently marked part in its own class directory
9. Run each instructor case in a fresh Java process with a timeout and bounded captured output
10. Export scores, per-case diagnostics, and reasons for withheld scores into a new grading run directory

Original ZIPs, instructor testers, starter files, and the source score sheet are preserved. Conflicting ownership is reported instead of guessed. A missing source file is not manufactured from starter code.

`READY` means preparation is complete for the next stage, not that the student passed. `REVIEW_REQUIRED` means an instructor must inspect a problem; name warnings and recoverable question problems do not discard valid independent questions. `FAILED` means preparation failed for that archive. The batch continues after an individual failure.

The process exits successfully when the grading batch and reports complete, even if individual submissions need review. Invalid startup configuration and an empty input batch return a nonzero exit code. Read the final status counts and logs rather than interpreting exit code zero as a clean grade report.

## Files you will use

| File or directory | Purpose |
| --- | --- |
| `config.properties` | Paths, matching rules, extraction limits, required sources, test metadata, and CSV column mappings |
| `pom.xml` | Java target, test dependencies, and Maven build/JAR settings |
| `agents.md` | Team ownership, current project status, and approval gates |
| `docs/REQUIREMENTS.md` | Rubric requirements, evidence targets, bonus checks, and submission checklist |
| `docs/ARCHITECTURE.md` | Component responsibilities, shared contracts, and execution decisions |
| `docs/TEAM_WORKFLOW.md` | Human review, safe division of work, build confirmation, and presentation preparation |
| `docs/CODING_STYLE.md` | Responsibilities, class/method size review guidelines and future agent coding rules |
| `docs/CODE_REVIEW.md` | Source-baseline review inventory, implemented class boundaries, verification and separate correctness findings |
| `docs/PR5_MERGE_RESOLUTION.md` | PR #5 conflict scope, preservation contracts, verification status and release gates |
| `docs/BUGS.md` | Known bugs, evidence, ownership contacts and suggested fix checks |
| `src/main/java/autograder` | Application code and extension contracts |
| `src/test/java/autograder` | Automated regression tests with temporary synthetic inputs |
| `Project/student-submission` | Six supplied ZIP submissions |
| `Project/Tester-Files` | Instructor Java testers and text fixtures |
| `Project/RenameToYourUsername` | Starter sources and supplied support classes |
| `Project/IS442-ScoreSheet.csv` | Original class list and required report format |
| `build/extracted` | Raw extraction workspaces created by runs |
| `build/working` | Separate corrected working copies |
| `build/grading/run-*` | Isolated compilation/case workspaces, CSV reports and optional dashboard.html |
| `build/submission-anomalies.log` | Problems found during preparation |
| `build/submission-corrections.log` | Corrections made to working copies |
| `target` | Maven-generated classes, test reports, and executable JAR |

Generated workspaces/logs and Maven output are ignored by Git. New runs use new workspaces. Logs append, so retain their timestamps when examining a particular run.

## Where the Java code lives

All paths in this table are beneath `src/main/java/autograder/`.

| File | Plain-language job |
| --- | --- |
| `App.java` | Starts the application and prints progress/results |
| `config/AppConfig.java` / `ExecutionConfig.java` | Read preparation/grading settings and reject invalid combinations |
| `config/HtmlReportConfig.java` | Validate optional HTML enablement and filename |
| `report/html/OfflineHtmlReportGenerator.java` | Render the completed results and export decisions as one offline HTML file |
| `grading/GradingPipeline.java` | Coordinates compilation, cases, score aggregation and reports |
| `grading/InstructorTestCaseProvider.java` | Maps the supplied instructor cases to individual adapters |
| `execution/StudentCompiler.java` | Compiles selected student sources and instructor adapter with supplied support classes |
| `execution/CaseExecutor.java` | Runs one case and reads its explicit result file |
| `execution/ProcessRunner.java` | Enforces deadlines and bounds captured stdout/stderr |
| `submission/SubmissionProcessor.java` | Runs preparation for the whole batch and spots duplicate students |
| `submission/SafeZipExtractor.java` | Opens ZIPs with path, size, and corruption checks |
| `submission/SubmissionValidator.java` | Finds the question folders and rejects ambiguous folder layouts |
| `submission/IdentityResolver.java` | Checks required source names/headers, finds the roster match, and prepares/applies missing-header repairs |
| `submission/WorkingCopyPreparer.java` | Builds separate correctly named copies and records folder corrections |
| `submission/CorrectionLog.java` | Saves a history of corrections |
| `util/AnomalyLog.java` | Saves problems and prints warnings |
| `plagiarism/PlagiarismDetectorFactory.java` / `CombinedPlagiarismDetector.java` | Build the configured algorithms and run them together; one failing algorithm does not discard the others |
| `plagiarism/NGramPlagiarismDetector.java` | Our algorithm: compares each pair of students per question using normalized 5-token sequences and Jaccard similarity, excluding starter code |
| `plagiarism/JPlagPlagiarismDetector.java` | Runs the JPlag library (greedy string tiling over Java syntax tokens) per question, with the starter files as base code |
| `plagiarism/JavaTokenizer.java` / `NGramBuilder.java` / `SimilarityCalculator.java` / `QuestionFiles.java` | Normalize source tokens, build n-grams, compute Jaccard similarity and locate each question's files |
| `config/PlagiarismConfig.java` | Reads and validates the `plagiarism.*` settings |
| `roster/ScoreSheetRepository.java` | Reads the class list without discarding the CSV's original fields |
| `csv/CsvCodec.java` | Reads/writes CSV quoting, commas, and line breaks correctly |
| `report/CsvScoreReportWriter.java` | Fills supplied scores into a new score sheet without overwriting the original |
| `ui/ConsoleProgressListener.java` | Displays simple progress messages |
| `api/` | Six agreed connection points for the later bonus features |
| `model/` | Common data containers: students, submissions, corrections, assessment settings, test results, grades, and findings |

The model classes are forms for storing and passing information, not separate applications. For example, `SubmissionResult` holds everything preparation learned about one ZIP; `StudentGrade` holds the marked test results for that submission. Adapter source resources live under `src/main/resources/autograder/adapters` and are packaged inside the JAR.

## Configuration and another assessment

`plagiarism.enabled` and `plagiarism.algorithms` (`ngram`, `jplag` or both, comma-separated) choose the similarity check. `plagiarism.ngram.size`, `plagiarism.threshold` and `plagiarism.min.ngrams` tune the n-gram algorithm; `plagiarism.jplag.threshold` and `plagiarism.jplag.min.tokens` (shortest matching run JPlag counts) tune JPlag. Thresholds are above 0 and at most 1; each algorithm has its own because JPlag ignores short shared idioms and scores lower. `plagiarism.starter.directory` supplies the starter code both algorithms ignore. Relative paths resolve against the properties file's directory. `execution.java` and `execution.javac` accept `JDK` (the running JDK's tools), an executable on PATH, or a path. Compilation defaults to 15 seconds per part, execution to 2 seconds per case, and captured output to 65,536 bytes per stream. Both streams keep draining after the capture limit. These settings live in `config.properties`.
Relative paths resolve against the properties file's directory. `execution.java` and `execution.javac` accept `JDK` (the running JDK's tools), an executable on PATH, or a path. Compilation defaults to 15 seconds per part, execution to 2 seconds per case, and captured output to 65,536 bytes per stream. Both streams keep draining after the capture limit. These settings live in `config.properties`. After grading, advisory sanity checks look for signs of a problem in the assessment rather than the students and write `sanity-checks.csv`; they never change marks or export decisions. They flag a question every graded submission fails to compile, a question where at least `grading.sanity.zero.percent` (90) of graded submissions score zero, a case where at least `grading.sanity.same.failure.percent` (80) fail with the same printed output (silent failures are not grouped), a case timed out by at least `grading.sanity.timeout.percent` (50), and a question whose fixed pass rate exceeds its dynamic pass rate by `grading.sanity.fixed.dynamic.gap.points` (40) or more. Withheld submissions are excluded and checks only run with at least `grading.sanity.min.graded` (5) graded submissions; `grading.sanity.enabled=false` turns them off. All keys are optional. Optional `execution.workers` (1 to 32, or `auto`; default 1 when absent) grades that many submissions at once; results, CSV rows and reports are identical to sequential grading. Any count above the machine's logical processor count is capped to that count with a stderr warning, e.g. `execution.workers=16 exceeds 10 available processors; using 10`; values above 32 are still rejected. Java counts logical processors, not performance cores, so contention can still push slow-but-correct programs past the case timeout; keep the count at or below the machine's physical cores. The default is 1. `grading.feedback.enabled`, `grading.feedback.directory` and `grading.feedback.max.diagnostic.lines` control the per-student Markdown feedback files (feature 5, in review) written to `build/grading/run-*/feedback`.

`assessment.question.ids` describes independently scored parts, source files, instructor testers, case counts, weights, support classes, and fixtures. The current fixed adapters support the supplied Q1a (3 cases), Q1b (3), Q2a (5), Q2b (5), and Q3 (4) testers. Weights remain configurable and the roster denominator must agree. The provider verifies the original tester content so an edited tester cannot silently use an outdated adapter. Supporting a different tester requires a new reviewed adapter/provider; preparation's configurable metadata alone does not provide general-purpose test ingestion. Feature 4 adds five seeded property checks per question and distributes each question's existing maximum across all its fixed and dynamic cases.

## Execution and review policy

Each question compiles only its selected source, required companions, supplied support classes, and adapter. Student `.class` files are ignored. A missing answer/companion or syntax error gives that part zero marks without blocking other parts. Each case has a fresh working directory and fixture copies. A pass earns configured points; a mismatch, runtime error, timeout, or compile failure earns zero. A pass–loop–pass sequence retains two marks. Missing/invalid result files and `System.exit(0)` without a result are runtime errors. Printed `Passed` messages cannot award marks.

Unresolved identities, duplicate submissions, and unusable preparations are withheld. Name corroboration warnings remain visible but do not withhold a uniquely matched student's usable work. Infrastructure errors withhold the affected student's export instead of silently presenting a partial score as final. Every submission has all 20 case records, including NOT_RUN records when blocked.

The configured score filename defaults to `scorereport.csv`. `cases.csv` contains case status, points, duration and bounded stdout/stderr. `submissions.csv` identifies EXPORTED/WITHHELD rows, reasons and preparation warnings. Withheld and absent students retain their original template numerators; consult `submissions.csv` before treating those unchanged values as grades from this run.

Child processes and deadlines are reliability controls, not an OS security sandbox. This MVP supports course submissions in a controlled grading environment, not deliberately hostile code: student programs still have the operating-system user's permissions and can attempt filesystem/network/process access or tamper with the result protocol. Descendant cleanup is best effort and depends on OS permissions. Use an externally isolated account/container/VM for untrusted submissions; this application does not provision one.

## CSV compatibility

The reusable writer preserves the supplied header order and roster field values:

```text
OrgDefinedId,Username,Last Name,First Name,Email,Calculated Final Grade Numerator,Calculated Final Grade Denominator,End-of-Line Indicator
```

Only explicitly supplied scores change the numerator field; exported scores are rounded half-up to two decimal places. Per-case diagnostics retain full precision. All other rows/fields remain intact. The writer rejects unknown student IDs, invalid/out-of-range marks, and attempts to overwrite the source template. It handles quoted commas, quotes, and line breaks. Byte-for-byte CSV formatting need not be identical, but the schema and field contents are preserved.

The grading pipeline supplies scores only for eligible students after recording every expected case. Each run writes a fresh report; the original template is never the output path.

## Six bonus extension points

Sera's starter-code comparison (feature 3) is implemented locally and in REVIEW, pending human build/run and visual acceptance. It writes `starter-analysis.csv`, terminal warnings and HTML analysis findings without changing marks. The approved method-comparison revision also generates `starter-comparison/index.html` inside each grading run: open the printed **Method comparison report** path, search a student/question/method, select changed or unchanged methods, and expand the side-by-side source differences and token-edit explanations. It works offline and requires no extra grading command. See [the feature 3 testing guide](docs/STARTER_CODE_TESTING.md) for commands, configuration, expected outcomes and limitations. This status supersedes older blanket statements that other bonus implementations are TODO.

| Feature | Java interface | Owner |
| --- | --- | --- |
| 1: token-based similarity | PlagiarismDetector | Kiya |
| 2: offline HTML report | HtmlReportGenerator | Boey |
| 3: starter-code comparison | BoilerplateDetector | Sera, locked |
| 4: parameterized test cases | TestCaseProvider | William, locked |
| 5: Markdown feedback | FeedbackGenerator | Gabriel |
| 6: terminal dashboard | ProgressListener | Unclaimed |

The interfaces exist so implementations share common inputs and outputs. HTML reporting receives `GradingReportContext`, which carries completed batch results and explicit withholding reasons. Kiya's feature-1 detector is merged (integrated through PR #5) and in REVIEW. Boey owns the HTML dashboard (feature 2, DONE). Gabriel's Markdown feedback is merged and in REVIEW. Sera and William retain locked features 3 and 4; feature 6 is unclaimed with Kai Cong's intent recorded.

## Latest verification

Current agent verification on 2026-10-24 (`main` after PR #12, JDK 25 targeting Java 17): `mvn clean verify` passed **128 tests, zero failures/errors/skips**, and the packaged CLI graded the six supplied submissions with exit code 0. Each submission has **45 case records** (20 fixed instructor cases plus 25 seeded dynamic cases), and each question keeps its original maximum. The run wrote the score sheet, `cases.csv`, `submissions.csv`, `similarity.csv` (0 pairs at the 0.80 threshold), `starter-analysis.csv` (11 findings), `dashboard.html` and six feedback files. Human acceptance of features 1, 3, 4 and 5 is still pending.

| Student | Q1a | Q1b | Q2a | Q2b | Q3 | Total | Score sheet |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Chee | 3 | 3 | 5 | 0.5 | 0 | 11.5 | 11.50 |
| David | 3 | 3 | 4.5 | 0 | 0 | 10.5 | 10.50 |
| Fen | 0.375 | 0 | 5 | 4.5 | 0 | 9.875 | 9.88 |
| Jing | 0 | 0 | 0 | 0 | 0 | 0 | 0.00 |
| Ping | 3 | 3 | 5 | 5 | 4 | 20 | 20.00 |
| Xing | 0 | 0 | 0.5 | 0 | 0 | 0.5 | 0.50 |

Question maxima are 3, 3, 5, 5 and 4. Each question's marks are shared equally across its fixed and dynamic cases, so case values such as 0.375 are expected. Actual Java 17 runtime and Windows behaviour remain untested.

The following modularisation and MVP records are historical evidence.

The behaviour-preserving modularisation revision (merged in PR #5) was **REVIEW** pending post-merge checks, Boey's clean-build/behaviour acceptance and teammate review. Its pre-integration verification passed **86 tests, zero failures/errors/skips** on JDK 25 targeting Java 17; the sample retained the six totals below and 120 cases, producing HTML and six Markdown files. Ten deterministic artifacts and 49 public API surfaces matched in the historical comparison. Those results do not verify PR #5's integration. Actual Java 17 runtime, Windows and a new real-browser pass were not performed; the existing Windows fixture defect remains separately listed. See [CODE_REVIEW.md](docs/CODE_REVIEW.md) and the [PR #5 merge record](docs/PR5_MERGE_RESOLUTION.md).

The remaining MVP is **DONE**. Boey supplied a successful clean build on 2026-10-10 with **48 tests, zero failures/errors/skips**, followed by the expected six-submission grading output (12, 10, 10, 0, 20, 1). The human build-confirmation gate is satisfied. `mvn clean verify` passed **48 tests, zero failures/errors**. After final diagnostic wording/formatting fixes, the affected execution suite passed **7/7** and the executable JAR was rebuilt successfully. The packaged CLI graded all six supplied submissions in one fresh run, producing 120 case records (20 per submission). All scores reconciled between case points, submission summaries and exported CSV numerators. All **67** supplied-file hashes in the new snapshot remained unchanged.

| Student | Q1a | Q1b | Q2a | Q2b | Q3 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Chee | 3 | 3 | 5 | 1 | 0 | 12 |
| David | 3 | 3 | 4 | 0 | 0 | 10 |
| Fen | 1 | 0 | 5 | 4 | 0 | 10 |
| Jing | 0 | 0 | 0 | 0 | 0 | 0 |
| Ping | 3 | 3 | 5 | 5 | 4 | 20 |
| Xing | 0 | 0 | 1 | 0 | 0 | 1 |

David and Xing retain name-header warnings while their uniquely matched submissions are graded. Xing passes Q2a's missing-file case; the remaining four Q2a cases time out. Inspection shows the loop advances to the next line only when a surname matches, so an unmatched line can repeat forever. The other parts fail their assertions. The reference file's components sum to 15 but its stated total is 1; actual execution independently awards 1. Reference files were not changed.

Verification includes original-tester equivalence on Ping (20/20), wrong-answer and expected-exception equivalence, pass–loop–pass retaining 2 points, independent compilation failure, bounded output, interrupted-process cleanup, missing results/System.exit(0), duplicate/unresolved withholding, missing sources, configured-path checks and exact CSV integration. The final reports are under `build/grading/run-10952970798376735826`. Tests ran on the available JDK 25 with Maven's Java 17 compilation target; actual Java 17 runtime and Windows behavior have not been verified.

## Review rules

Read [docs/CODING_STYLE.md](docs/CODING_STYLE.md) before generating or editing code. Production Java files above 300 total lines and methods/constructors above 40 nonblank, noncomment lines trigger a responsibility review with justified exceptions. The [merged-main review](docs/CODE_REVIEW.md) records the implemented M1-M5 class boundaries and historical checks; [PR5_MERGE_RESOLUTION.md](docs/PR5_MERGE_RESOLUTION.md) records the pending integration. Further substantial refactoring requires a concrete approved plan. Report discovered bugs separately from behaviour-preserving refactoring.

Keep the Java code readable, with clear responsibilities, meaningful names, ordinary loops, and operational settings in configuration. Minimal comments must be lowercase and have no full stops. Agents must not add AI attribution comments: after reviewing and modifying code, the human team will manually add the declarations required by the brief and the presentation AI Use appendix. This remains a final submission requirement.

No feature is marked DONE before a human confirms the documented clean build. Boey has supplied a successful clean build and preparation run for the current foundation. Every member must understand the complete application and participate in substantive work and cross-member review.
