# CS102 Auto Grading System

The CS102 Auto Grading System prepares and grades Java assessment submissions in batches. It validates ZIP structure and student identity, compiles each question independently, runs instructor tests with time limits, calculates marks, and exports the supplied score-sheet format. Reports show case results, submission decisions, and grading diagnostics.

The project includes a command-line workflow and an optional Swing instructor window. Optional reports include an offline HTML dashboard and per-student Markdown feedback. Similarity analysis and score sanity checks provide advisory information; they do not change marks. Dynamic test cases add coverage while keeping each question's configured maximum unchanged.

## Quick start

Only a JDK 17 or newer is needed; `javac -version` should report 17 or later, and `JAVA_HOME`, if set, must point to that JDK. The first compile downloads Maven 3.9.9 and the dependencies through the included Maven Wrapper, so it needs internet access and can take a few minutes.

| | macOS / Linux | Windows |
| --- | --- | --- |
| Compile | `sh compile.sh` | `compile.bat` |
| Open the instructor window | `sh run.sh` | `run.bat` |
| Grade from the command line | `sh run.sh config.properties` | `run.bat config.properties` |
| Command-line help | `sh run.sh --help` | `run.bat --help` |

The scripts can be started from any directory or by double-clicking (Windows), and always work from the project folder, so relative paths in arguments are resolved from the project folder; use absolute paths for files elsewhere. Keep the console open while the instructor window runs; closing it closes the window. With arguments, `run` passes them to the command-line application, for example `sh run.sh --workers 4 config.properties`. The compile scripts skip the automated tests; run `sh ./mvnw clean verify` (or `mvnw.cmd clean verify`) for the full test suite. The project uses the standard Maven layout, with sources under `src/main/java`.

### Demo cohort: 100 students

The six supplied submissions are too few to show parallel grading, so `demo` grades a larger cohort: the six real archives plus 94 synthetic students. Each synthetic student takes its Q1, Q2 and Q3 folders from randomly chosen real submissions, with the header name and email rewritten, so every program is real student code and each synthetic question score equals its source student's. A fixed seed makes every generation identical. Nothing under `Project/` or `config.properties` changes; the cohort is written to `build/demo-cohort/` and graded with `demo.properties`, which differs from `config.properties` only in its input, score sheet and output paths (results under `build/demo-run/grading/`).

| | macOS / Linux | Windows |
| --- | --- | --- |
| Grade the cohort (generated on first use) | `sh demo.sh` | `demo.bat` |
| Grade with 8 parallel workers | `sh demo.sh 8` | `demo.bat 8` |
| Rebuild with a different size | `sh demo.sh generate 24` | `demo.bat generate 24` |

`generate` takes the number of synthetic students (1-400, default 94) and an optional seed. Measured on a 10-core Mac (4 performance cores): 100 students took 11 min 21 s with 1 worker and 4 min 40 s with 8, with identical case verdicts, nobody withheld and the six real scores unchanged. A 30-student cohort (`generate 24`) took 74 s with 8 workers. About 117 cases time out by design, because synthetic students that reuse a hanging Q2 submission inherit its infinite loop. Recombined code also produces many similarity findings, which shows the plagiarism report working but makes `dashboard.html` about 7 MB.

On Windows, use 1 worker for now. A diagnostic on a Windows laptop showed that with 2 or more workers, Java start-up time counts against the 2-second case limit and some passing cases are reported as timed out.

To use the instructor window instead, run `sh run.sh` and set Config file to `demo.properties`. The window remembers previously used submission, score sheet and output folders, so also set them to `build/demo-cohort/submissions`, `build/demo-cohort/IS442-ScoreSheet.csv` and `build/demo-run/grading`.

## Setup, build and run

### Optional randomized question mode

The original assessment pipeline remains the default. The optional randomized mode can generate reusable student resources, grade one extracted submission, or grade one question across a folder of ZIPs:

~~~sh
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-resources Q1 config.properties
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade Q1 students/submissions/alice config.properties 482910374651
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade-batch Q1a C:/courses/CS102/submissions config.properties 482910374651
~~~

The current lab-test question set (Q1a, Q1b, Q2a, Q2b, Q3) is also available through this mode; for example, substitute `Q1a` in either command. Omit the final seed argument to let the app choose one; the result records it for replay. See [RANDOMIZED_AUTOGRADER.md](docs/RANDOMIZED_AUTOGRADER.md) for signatures, generated fixtures, demo steps, configuration, and process-isolation limits.

Instructors can follow the [randomized lab-test user guide](docs/INSTRUCTOR_USER_GUIDE.md) for step-by-step resource generation and randomized ZIP batch grading. The original ZIP batch grader remains a separate workflow.

### 1. Install the prerequisites

You need Git and a **full JDK 17 or later** (including `javac`). An editor such as VS Code or IntelliJ is optional. The repository includes the Maven Wrapper, which downloads Maven 3.9.9 and the required Maven plugins/dependencies on the first build, so a separate Maven installation is not required. Internet access is needed for that first build.

Install the JDK using your operating system's installer or package manager. Set `JAVA_HOME` to the JDK installation directory and add the JDK's `bin` directory to `PATH`. On Windows, configure these through Environment Variables and reopen your terminal. On macOS/Linux, use your shell configuration or the package manager's setup instructions.

Open Terminal on macOS/Linux or PowerShell on Windows and check:

```sh
git --version
java -version
javac -version
```

The Java commands should report JDK 17 or later. The project targets Java 17; recorded GitHub Actions checks have run on Temurin JDK 17 for Windows and macOS. The GUI and the Windows `compile.bat`/`run.bat` scripts have not been personally checked on Windows.

### 2. Clone and open the repository

```sh
git clone https://github.com/boeyboeyboeyboey/CS102-Group3.git
cd CS102-Group3
```

Open the repository root, where `pom.xml` and `config.properties` are located. The supplied `Project` directory contains sample submissions, the roster, fixtures and support classes.

If you already cloned the repository, inspect local changes before updating:

```sh
git status
git pull --ff-only
```

If Git reports divergent branches or local changes, preserve those changes before updating.

### 3. Build and run the automated checks

```sh
./mvnw clean verify
```

On Windows PowerShell, use `.\mvnw.cmd clean verify`.

This removes the previous `target` output, compiles the application, runs the tests and builds `target/auto-grading-system-1.0-SNAPSHOT.jar`. The first build downloads Maven 3.9.9, plugins and dependencies and needs internet access. The only application libraries are JPlag 4.3.0 (GPL-3.0, the newest release built for Java 17) and its small dependencies; Maven downloads them and bundles them into the JAR, so nothing needs to be installed manually and `java -jar` works on its own.

Wait for **BUILD SUCCESS** before running the application. The tests use temporary inputs and include expected failures such as malformed archives and timed-out student programs. Detailed test reports are in `target/surefire-reports`.

### 4. Grade the supplied sample submissions

```sh
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

The optional `--workers <N|auto>` flag grades N submissions at once for that run only, overriding `execution.workers`: `java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --workers <N|auto> config.properties`. N is 1 to 32; `auto` uses the number of logical processors, at most 4. A bad value prints usage and exits 1.

The default configuration processes the six supplied archives. The supplied run produces Chee **11.50**, David **10.50**, Fen **9.88**, Jing **0.00**, Ping **20.00**, and Xing **0.50** out of 20 in the score sheet. These totals include dynamic case results. The score sheet rounds half-up to two decimal places, so Fen's exact 9.875 is exported as 9.88; `submissions.csv` (ComputedScore) and `cases.csv` keep the exact values. David and Xing retain name-header review warnings. A completed run with those warnings is expected; check the reports rather than treating every warning as a build failure.

The terminal prints the exact new `build/grading/run-*` directory containing:

- `scorereport.csv`: scores in the required roster format
- `cases.csv`: individual test statuses, marks and diagnostic output
- `submissions.csv`: export/withholding decisions and preparation warnings
- `similarity.csv`: student pairs whose code for a question reaches a similarity threshold, one row per algorithm (n-gram and/or JPlag) with matching line ranges (advisory only; never changes scores)
- `dashboard.html`: self-contained offline dashboard when `grading.html.enabled=true`

Each run creates a fresh directory. `mvn clean` removes `target`, but does not remove old `build` workspaces or appended preparation logs. Supplied input files remain unchanged. See [Grading behavior and process limits](#grading-behavior-and-process-limits) before using submissions outside the supplied sample.

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

Case diagnostics contain recorded stdout/stderr, subject to the configured capture limit. They do not include structured expected/actual assertion values. An analysis report appears only when the corresponding analysis is enabled and completes.

For a demonstration, run the build and grading commands above, then open the printed HTML file directly in a browser. The dashboard works without a server or internet connection.

### Common setup problems

| Symptom | What to check |
| --- | --- |
| `java`, `javac` or `mvn` is not found | Install the missing prerequisite, check `JAVA_HOME`/`PATH`, and reopen the terminal; a JRE alone does not supply `javac` |
| Maven reports an unsupported Java release or missing compiler | Check `mvn -version`; Maven must use a full JDK 17 or later |
| Maven cannot download a plugin or dependency | Check internet/proxy access and retry the build once access is restored |
| Maven cannot find `pom.xml` | Change directory to the cloned repository root |
| `Unable to access jarfile` | Complete `mvn clean verify` successfully and run the exact JAR path from the repository root |
| Missing configuration or input resources | Run from the root with `config.properties`, retain the supplied `Project` folder, and check configured paths; paths inside the properties file resolve relative to that file |

The project uses the standard Maven directory layout, with application sources under `src/main/java`.

## What the command does

1. Load the explicitly supplied properties file and validate its settings
2. Read and validate the provided score sheet as the class roster
3. Extract each ZIP into a fresh raw workspace, rejecting unsafe paths, corrupted data, and excessive expansion
4. Identify the student using archive names, wrapper folders, and source headers against the roster
5. Check configured question folders, source filenames, and required headers
6. Prepare a separate working copy under the matched username and fill missing identity headers only when ownership is unambiguous
7. Report problems and corrections, including duplicate submissions requiring review
8. Compile each independently marked part in its own class directory
9. Run instructor cases with configured process isolation, time limits, and bounded captured output
10. Export scores, per-case diagnostics, and reasons for withheld scores into a new grading run directory

Original ZIPs, instructor testers, starter files, and the source score sheet are preserved. Conflicting ownership is reported instead of guessed. A missing source file is not manufactured from starter code.

`READY` means preparation is complete for the next stage, not that the student passed. `REVIEW_REQUIRED` means an instructor must inspect a problem; name warnings and recoverable question problems do not discard valid independent questions. `FAILED` means preparation failed for that archive. The batch continues after an individual failure.

The process exits successfully when the grading batch and reports complete, even if individual submissions need review. Invalid startup configuration and an empty input batch return a nonzero exit code. Read the final status counts and logs rather than interpreting exit code zero as a clean grade report.

## Files you will use

| File or directory | Purpose |
| --- | --- |
| `config.properties` | Paths, matching rules, extraction limits, required sources, test metadata, and CSV column mappings |
| `pom.xml` | Java target, test dependencies, and Maven build/JAR settings |
| `docs/REQUIREMENTS.md` | Functional requirements and project acceptance criteria |
| `docs/ARCHITECTURE.md` | Application components and execution design |
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
| `api/` | Interfaces used by the grading and reporting components |
| `model/` | Common data containers: students, submissions, corrections, assessment settings, test results, grades, and findings |

The model classes store information shared between application components. For example, `SubmissionResult` holds preparation results for one ZIP, and `StudentGrade` holds that submission's test results. Adapter source resources live under `src/main/resources/autograder/adapters` and are packaged inside the JAR.

## Configuration and another assessment

`config.properties` controls paths, assessment questions, extraction limits, test cases, and reports. Relative paths resolve against the properties file. `execution.java` and `execution.javac` accept `JDK`, a command on `PATH`, or an executable path. Compilation defaults to 15 seconds per question; each case defaults to 2 seconds, with stdout and stderr captured up to 65,536 bytes per stream.

`execution.case.isolation` may be `part` (default) or `case`. Part isolation reuses a Java process across one question's cases while refreshing student classes and fixtures and restoring system properties. Each case still has its own output files and timeout. A failed shared process is replaced; `execution.case.startup.timeout.ms` (default 30 seconds) limits process startup. `execution.workers` accepts 1 to 32 or `auto` (default 1); counts above the logical processor count are capped with a warning. High worker counts can make slow correct submissions reach the case timeout.

Plagiarism analysis is configured with `plagiarism.enabled`, `plagiarism.algorithms` (`ngram`, `jplag`, or both), and the algorithm-specific threshold settings. The n-gram detector compares normalized token sequences; JPlag compares Java syntax tokens. Both can exclude the configured starter directory. Findings are advisory and never affect marks.

`grading.sanity.*` settings control optional checks for assessment-wide patterns such as unusually low scores or widespread timeouts. These checks do not change marks or export decisions. `grading.feedback.enabled`, `grading.feedback.directory`, and `grading.feedback.max.diagnostic.lines` control per-student Markdown feedback.

`assessment.question.ids` defines each part's source files, tester, cases, weights, support classes, and fixtures. The supplied assessment has five question parts and 20 fixed cases. The dynamic test provider adds five seeded property checks per part, distributing each question's existing maximum across its fixed and dynamic cases. A different tester needs a matching adapter; configuration alone does not turn arbitrary Java testers into runnable cases.

## Grading behavior and process limits

Each question compiles only its selected source, required companions, supplied support classes, and adapter. Student `.class` files are ignored. A missing answer/companion or syntax error gives that part zero marks without blocking other parts. Each case has a fresh working directory and fixture copies. A pass earns configured points; a mismatch, runtime error, timeout, or compile failure earns zero. A pass–loop–pass sequence retains two marks. Missing/invalid result files and `System.exit(0)` without a result are runtime errors. Printed `Passed` messages cannot award marks.

Unresolved identities, duplicate submissions, and unusable preparations are withheld. Name corroboration warnings remain visible but do not withhold a uniquely matched student's usable work. Infrastructure errors withhold the affected student's export instead of silently presenting a partial score as final. Every submission has a record for each configured case, including NOT_RUN records when blocked.

The configured score filename defaults to `scorereport.csv`. `cases.csv` contains case status, points, duration and bounded stdout/stderr. `submissions.csv` identifies EXPORTED/WITHHELD rows, reasons and preparation warnings. Withheld and absent students retain their original template numerators; consult `submissions.csv` before treating those unchanged values as grades from this run.

Child processes and deadlines are reliability controls, not an OS security sandbox. This application supports course submissions in a controlled grading environment, not deliberately hostile code: student programs still have the operating-system user's permissions and can attempt filesystem/network/process access or tamper with the result protocol. Descendant cleanup is best effort and depends on OS permissions. Use an externally isolated account/container/VM for untrusted submissions; this application does not provision one.

## CSV compatibility

The reusable writer preserves the supplied header order and roster field values:

```text
OrgDefinedId,Username,Last Name,First Name,Email,Calculated Final Grade Numerator,Calculated Final Grade Denominator,End-of-Line Indicator
```

Only explicitly supplied scores change the numerator field; exported scores are rounded half-up to two decimal places. Per-case diagnostics retain full precision. All other rows/fields remain intact. The writer rejects unknown student IDs, invalid/out-of-range marks, and attempts to overwrite the source template. It handles quoted commas, quotes, and line breaks. Byte-for-byte CSV formatting need not be identical, but the schema and field contents are preserved.

The grading pipeline supplies scores only for eligible students after recording every expected case. Each run writes a fresh report; the original template is never the output path.

## Reports and analysis

Each run writes a score sheet, per-case results, submission decisions, and any enabled reports to its own directory under `build/grading/run-*`. Similarity analysis flags code pairs for instructor review and never changes marks. Score sanity checks look for unusual assessment-wide patterns and are advisory. The optional HTML dashboard and Markdown feedback summarize the completed run.

The dashboard is a single offline HTML file. It supports searching and filtering submissions, sorting scores, and inspecting question-level case evidence. Summary averages and medians include exported submissions only; withheld scores are diagnostics, not final marks.

## Sample result

A run with the supplied assessment produces these totals with the fixed and dynamic test cases:

| Student | Q1a | Q1b | Q2a | Q2b | Q3 | Total | Score sheet |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Chee | 3 | 3 | 5 | 0.5 | 0 | 11.5 | 11.50 |
| David | 3 | 3 | 4.5 | 0 | 0 | 10.5 | 10.50 |
| Fen | 0.375 | 0 | 5 | 4.5 | 0 | 9.875 | 9.88 |
| Jing | 0 | 0 | 0 | 0 | 0 | 0 | 0.00 |
| Ping | 3 | 3 | 5 | 5 | 4 | 20 | 20.00 |
| Xing | 0 | 0 | 0.5 | 0 | 0 | 0.5 | 0.50 |

Question maxima are 3, 3, 5, 5 and 4. Each question's marks are shared equally across its fixed and dynamic cases, so case values such as 0.375 are expected. The score sheet rounds half-up to two decimal places; case and submission reports retain full precision. Results may vary when the assessment, submissions, or configuration changes.
