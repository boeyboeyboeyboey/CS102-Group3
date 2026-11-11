# ByteGrade

ByteGrade is a batch grader for Java assessment submissions (CS102 Auto Grading System).

- Input: a folder of student ZIP archives and the class score sheet (CSV).
- It checks each ZIP's structure and student identity, then compiles each question on its own.
- It runs the instructor tests with time limits and exports marks in the supplied score-sheet
  format.
- It runs from the command line or from a Swing window.
- Reports: CSV results, an offline HTML dashboard and per-student Markdown feedback.
- Advisory analyses that never change marks: code similarity, starter-code comparison and
  score sanity checks.
- Dynamic tests add generated cases to the supplied ones; each question's maximum stays the same.
- The slide deck is `G1-T3.pptx`, in the root of the submission folder.

## Quick start

Open a terminal in the folder that contains `compile.sh`, `config.properties` and `Project/`.
The first build needs internet access (see "Requirements").

- macOS / Linux: `sh compile.sh`, then `sh run.sh config.properties`.
- Windows (Command Prompt): `compile.bat`, then `run.bat config.properties`. In PowerShell, put
  `.\` before each script name.
- The sample scores, in the order Ping, Chee, David, Fen, Jing, Xing, are 20.00, 11.50, 10.50,
  9.88, 0.00 and 0.50. See "Expected sample result".
- Results are written to the `Run folder:` the run prints. See "Outputs".

## Requirements

- A full JDK 17 or newer, including `javac`. `javac -version` should report 17 or later.
  - A JRE alone is not enough.
  - If `JAVA_HOME` is set, it must point to that JDK.
  - `JAVA_HOME` is not required. On a Windows 11 PC with it unset, `compile.bat`, `mvnw.cmd`
    and `run.bat` worked because `java` and `javac` were on `PATH`.
  - On Windows, add the JDK's `bin` folder to `PATH` in Environment Variables, then reopen the
    console.
- Internet access for the first build.
  - The Maven Wrapper (`mvnw`, `mvnw.cmd`) downloads Maven 3.9.9, its plugins and the libraries.
  - A separate Maven installation is not needed.
  - The first build can take a few minutes.
- The project targets Java 17, and newer JDKs work. Compilation, the automated tests and the
  sample run were checked on Windows and macOS with Temurin 17, and the project was also run on
  JDK 25. The GUI works on macOS and on Windows 11 (JDK 25).

## Workflow

Unzip `G1-T3-ByteGrade.zip` and open a terminal in the folder that contains `compile.sh`,
`config.properties` and `Project/`. The scripts change to their own folder before running, so
they also work from elsewhere or by double-click on Windows. Relative paths in arguments
are then resolved from the project folder; use absolute paths for files elsewhere.

### 1. Build

macOS / Linux:

```sh
sh compile.sh
```

Windows (Command Prompt):

```bat
compile.bat
```

In PowerShell, put `.\` before each script name, for example `.\compile.bat`, `.\run.bat`,
`.\demo.bat` and `.\mvnw.cmd`. Without it, PowerShell reports the script as not recognized.
Every Windows command in this README is written for Command Prompt.

- Success ends with `Built target/auto-grading-system-1.0-SNAPSHOT.jar`.
- The build may print `[WARNING]` lines about overlapping resources. They are harmless.
- On JDK 25, Maven itself also prints restricted-method and `sun.misc.Unsafe` warnings. The
  build still succeeded on Windows 11 with JDK 25.
- The compile scripts skip the automated tests.
- `run` and `demo` stop with a message if that JAR is missing, so build first.

### 2. Grade the sample class from the command line

macOS / Linux:

```sh
sh run.sh config.properties
```

Windows (Command Prompt):

```bat
run.bat config.properties
```

- The sample input is the six ZIPs in `Project/student-submission`.
- The terminal shows progress, then one short line per student. A `[REVIEW]` flag marks a
  submission that needs a look; the detail is in the CSVs. The last lines are `Run time:` and
  `Run folder:` with the new `build/grading/run-<number>`. In an interactive terminal, typing
  `q` then Enter cancels the run (exit 130).
- The console prints each student's unrounded total (for example `9.875`). `scorereport.csv`
  rounds it half-up to two decimals (`9.88`).
- Exit code 0 means the batch and its reports completed, even if some submissions need review.
  Read the status counts and `submissions.csv` rather than treating 0 as "all clean".
- A bad or missing configuration, or an empty input folder, returns a nonzero exit code.
- `sh run.sh --help` (or `run.bat --help`) prints the command-line usage.

### Choosing the number of workers

```sh
sh run.sh --workers 1 config.properties
sh run.sh --workers 4 config.properties
```

On Windows, use `run.bat` with the same arguments.

- `--workers <N|auto>` grades N submissions at once for that run only.
  - It overrides `execution.workers` in the configuration file. With no flag,
    `execution.workers` applies, and its default is 1.
  - N is 1 to 32. `auto` is the number of logical processors, at most 4.
  - A value above the machine's logical processor count is capped, with a warning.
  - A bad value prints the usage and exits with status 1.
- On one Windows laptop we tested earlier, Java start-up time counted against the 2 second case
  limit with 2 or more workers, and some passing cases were reported as timed out. On a
  Windows 11 PC with 20 logical processors, 2 and 4 workers gave the same scores and case
  statuses as 1 worker, with no new timeouts. If passing cases show as timed out on any
  machine, rerun with 1 worker.

### 3. Use the instructor window

macOS / Linux:

```sh
sh run.sh
```

Windows (Command Prompt):

```bat
run.bat
```

- Run with no arguments, the script opens the Swing window. Keep the console open; closing it
  closes the window. A headless machine prints the command-line alternative and exits with 1.
- Steps:
  - The Submissions, Score sheet and Output folder rows start from the configured values.
    Change any of them with its Choose... button. Each row shows its readiness inline.
  - To use another configuration, open Advanced and change Config file there.
    Advanced > Edit config... edits the file in a text editor that keeps comments and layout
    and refuses a save that would not load. Nothing else in the GUI changes the file.
  - The Workers dropdown next to Grade offers 1 up to the processor count. It starts at the
    configured `execution.workers` (capped, with a warning on the status line) and applies to
    that run only.
  - Click Grade. It runs a final check, then grades in the background with progress and a
    run clock.
  - Stop interrupts grading: test processes are terminated and no scores are written.
    Closing the window during a run asks for confirmation first.
  - When the run ends, the scores are shown with one button per output that was written.
    An empty Similarity or Sanity checks report says "No pairs reached the similarity
    threshold." or "No sanity checks were flagged."
- Results buttons: Dashboard, Scores, Case results, Submission decisions, Similarity,
  Starter code, Sanity checks, Feedback, Run folder and Export all...
  - The CSV reports and student feedback open inside the window.
  - Dashboard opens in the browser; Run folder opens in the file manager.
  - Export all... saves each CSV report to a folder you choose, plus one Excel workbook with
    one sheet per report.
  - Upload `scorereport.csv` itself to eLearn.
- The GUI uses the same grading service, validation, withholding and report order as the
  command line. Last-used paths are remembered with Java Preferences.
- The remembered config file, submissions, score sheet and output folder are kept per user
  account, not per copy of the project. After unzipping a fresh copy, choose these again (the
  Config file is under Advanced). Otherwise Grade can read and write the folders of an
  earlier copy.
- Without an argument the GUI starts from the last configuration it ran, then
  `config.properties` in the current directory, then the one beside the `target` folder.

### 4. Read the outputs

- See "Outputs" below. Start with the dashboard, then `submissions.csv`.
- `scorereport.csv` holds totals only. Read per-question marks in a student's file under
  `feedback/` (a table at the top) or in the dashboard (expand a submission).

### 5. Optional: run the automated tests

macOS / Linux:

```sh
sh ./mvnw verify
```

Windows (Command Prompt):

```bat
mvnw.cmd verify
```

- This takes about a minute once the dependencies are downloaded. On a Windows 11 PC it took
  3 to 4 minutes, so allow time.
- On JDK 25, the output also warns that the surefire parameters `timeout` and `timeoutUnit` are
  unknown. The tests still passed.
- The tests use temporary inputs. They include expected failures such as malformed archives
  and timed-out student programs.

## Expected sample result

The default configuration grades the six archives in `Project/student-submission`. Scores out
of 20, as written to the score sheet (rounded half-up to two decimals):

- Ping 20.00
- Chee 11.50
- David 10.50
- Fen 9.88
- Jing 0.00
- Xing 0.50

Marks per question (Q1a, Q1b, Q2a, Q2b, Q3), before rounding:

- Ping: 3, 3, 5, 5, 4 = 20
- Chee: 3, 3, 5, 0.5, 0 = 11.5
- David: 3, 3, 4.5, 0, 0 = 10.5
- Fen: 0.375, 0, 5, 4.5, 0 = 9.875, exported as 9.88
- Jing: 0, 0, 0, 0, 0 = 0
- Xing: 0, 0, 0.5, 0, 0 = 0.5

Notes on these numbers:

- Question maxima are 3, 3, 5, 5 and 4. Each question's marks are shared equally across its
  fixed and dynamic cases, so case values such as 0.375 are expected.
- `submissions.csv` (ComputedScore) and `cases.csv` keep the exact values.
- David and Xing keep name-header review warnings. A completed run with those warnings is
  expected; check the reports rather than treating each warning as a failure.
- Results may vary if the assessment, the submissions or the configuration change.

### Why this differs from the supplied graded sheet

- `Project/IS442-ScoreSheet-graded.csv` gives Ping 20, Chee 12, David 10, Fen 10, Jing 0 and
  Xing 1. It counts only the 20 supplied cases.
- The default run also grades the dynamic cases, so Chee, David, Fen and Xing differ.
- To reproduce the supplied sheet, leave `config.properties` alone and run a copy with dynamic
  tests off. Keep the copy in the same folder, because relative paths resolve against the
  properties file. On Windows, use `copy` instead of `cp`.

```sh
cp config.properties fixed-only.properties
```

- Add this line to the copy, then run `sh run.sh fixed-only.properties` (Windows Command Prompt:
  `run.bat fixed-only.properties`; PowerShell: `.\run.bat fixed-only.properties`):

```properties
grading.dynamic.enabled=false
```

- With that setting the fixed cases carry each question's full maximum.

## Demo cohort: 100 students

The six supplied submissions are too few to show parallel grading. The demo grades the six real
archives plus 94 synthetic students.

- Each synthetic student takes its Q1, Q2 and Q3 folders from randomly chosen real submissions,
  with the header name and email rewritten. Every program is real student code, and each
  synthetic question score equals its source student's.
- A fixed seed makes every generation identical.
- Nothing under `Project/` and neither properties file changes.
- The cohort is written to `build/demo-cohort/`: `submissions/` (the ZIPs), a generated
  `IS442-ScoreSheet.csv` and `donors.csv`.
- It is graded with `demo.properties`, which differs from `config.properties` only in its input,
  score sheet and output paths. Results go to `build/demo-run/grading/`.

macOS / Linux:

```sh
sh demo.sh
sh demo.sh 8
sh demo.sh generate
sh demo.sh generate 24
sh demo.sh generate 24 7
```

Windows (Command Prompt):

```bat
demo.bat
demo.bat 8
demo.bat generate
demo.bat generate 24
demo.bat generate 24 7
```

- `demo` grades the cohort, generating it first if `build/demo-cohort/submissions` is missing.
- `demo 8` does the same with 8 workers. If that is more than the machine's logical processors,
  it is capped to that count with a warning.
- `demo generate [count] [seed]` rebuilds the cohort without grading it.
  - `generate` must be the first argument. `sh demo.sh 8 generate` prints the usage line.
  - It replaces the existing cohort: `build/demo-cohort/` is deleted and rebuilt.
  - count is the number of synthetic students, 1 to 400, default 94.
  - seed is a whole number, default 102.
  - More than two arguments, or a count outside 1 to 400, prints a usage line and exits 1.
- The generator reads `config.properties` from the project folder, so use the scripts.
- To use the window instead, run `sh run.sh` (or `run.bat`) and set Config file to
  `demo.properties` under Advanced. The window remembers earlier folders, so also set
  Submissions to `build/demo-cohort/submissions`, Score sheet to
  `build/demo-cohort/IS442-ScoreSheet.csv` and Output folder to `build/demo-run/grading`.

Timings and expected behaviour:

- Measured on a 10-core Mac (4 performance cores), 100 students:
  - 1 worker: 7 min 07 s
  - 4 workers: 2 min 44 s
  - 8 workers: 2 min 30 s
- On a Windows 11 PC with 20 logical processors and JDK 25, `demo.bat 1` took 17 min 10 s, so
  allow time.
- Case verdicts were identical at every worker count, nobody was withheld and the six real
  scores were unchanged.
- A 30-student cohort (`generate 24`) took 74 s with 8 workers.
- About 117 cases time out by design: a synthetic student that reuses a hanging Q2 submission
  inherits its infinite loop.
- Recombined code produces many similarity findings. That shows the plagiarism report working,
  and it makes `dashboard.html` about 7 MB.
- On Windows, the worker count that works depends on the machine (see "Choosing the number of
  workers").

## Outputs

The last line of a command-line run, `Run folder:`, gives the new `build/grading/run-*` folder.
Each run creates a fresh folder; supplied inputs are never changed.

- `scorereport.csv`: scores in the required roster format. This is the file to upload.
- `cases.csv`: one row per case with status, marks, duration and captured stdout/stderr.
- `submissions.csv`: EXPORTED or WITHHELD for each submission, the reason, preparation warnings
  and the exact computed score.
- `similarity.csv`: student pairs whose code for a question reaches a similarity threshold.
  - One row per algorithm (n-gram and/or JPlag) with the matching line ranges.
  - Advisory only.
- `starter-analysis.csv`: how each answer file differs from the starter code (advisory).
  - `UNCHANGED_STARTER`: the answer's code matches the starter once comments and formatting are
    ignored. Open the file and decide whether real work was done. Marks are not changed.
- `starter-comparison/index.html`: method-level starter comparison, when starter analysis runs.
- `sanity-checks.csv`: score sanity checks (advisory). Header-only means nothing was flagged.
- `dashboard.html`: self-contained offline dashboard, when `grading.html.enabled=true`.
- `feedback/`: one Markdown file per student, when `grading.feedback.enabled=true`.
- `submission-1` to `submission-N`: working folders for each graded submission (compiled
  classes, test sources and case files, one subfolder per question). N is the number of
  archives, and `submission-k` is the k-th archive in file-name order, the `Grading k/N` line
  on the console. A withheld submission has no folder, so a number can be missing.

An analysis file appears only when that analysis is enabled and completes.

Other folders under `build/`:

- `build/extracted`: raw extraction workspaces
- `build/working`: corrected working copies, one per student
- `build/submission-anomalies.log`: problems found during preparation (logs append)
- `build/submission-corrections.log`: corrections made to working copies
- `build/demo-cohort`, `build/demo-run`: the demo cohort and its results
- `target`: compiled classes and the executable JAR

### Reading the dashboard

Open `dashboard.html` directly in a browser. It needs no server or internet connection.

- It contains student names and usernames, absolute file paths on the grading machine, and
  captured program output. Handle and share it with care.
- `cases.csv` and `starter-analysis.csv` also hold absolute paths, and `cases.csv` holds
  captured output. Treat them the same way.
- Search by name, username or archive; filter by export decision and warnings; sort by name or
  score; expand a submission for question marks, preparation corrections and per-case output.
- Question-score cells form a heatmap. Darker blue is a larger share of the configured marks.
  The legend explains the six credit bands.
- Click a question score to open that submission's details at the matching question group.
  Search, filters and sorting stay in place.
- Summary averages and medians include only exported submissions and name that count. The mean
  is rounded to four decimals.
- WITHHELD scores are diagnostic totals, not exported marks, and are labelled as such.
- A REVIEW_REQUIRED name warning can coexist with an exported score.
- Cells with missing or mismatched case records, or only NOT_RUN cases, are shown in neutral
  styling with an explanation. All recorded cases remain in the details.
- The dashboard covers received submissions only. Roster rows without a submission are absent.
- Case output is the recorded stdout/stderr, up to the capture limit. Expected and actual
  assertion values are not recorded.
- Settings: `grading.html.enabled` and `grading.html.file`.
  - The file name defaults to `dashboard.html` and must be a simple `.html` name of letters,
    digits, dots, underscores or hyphens.
  - If generation fails, the command returns nonzero and prints the completed CSV locations;
    those CSVs remain usable.

### Score sheet format

- The writer keeps the supplied header and every roster field except the numerator.

  Columns, in order:
  - OrgDefinedId
  - Username
  - Last Name
  - First Name
  - Email
  - Calculated Final Grade Numerator
  - Calculated Final Grade Denominator
  - End-of-Line Indicator

- Only the numerator of an exported student changes. It is rounded half-up to two decimals.
  Per-case diagnostics keep full precision.
- Withheld students and roster rows without a submission keep their original numerator.
  Check `submissions.csv` before treating an unchanged value as a grade from this run.
- The writer rejects unknown student IDs, invalid or out-of-range marks, and an output path equal
  to the source sheet. It handles quoted commas, quotes and line breaks.
- Byte-for-byte formatting may differ from the source; schema and field contents are preserved.

## Configuration

`config.properties` controls paths, the assessment, extraction limits, test cases and reports.
Relative paths resolve against the properties file. To grade other submissions, copy the file,
change the paths in the copy and pass it as the argument (`sh run.sh my.properties`).
`demo.properties` is an example of such a copy.

Keys an instructor is likely to change:

- Paths
  - `input.directory`: folder of student ZIPs
  - `score.sheet.path`: class score sheet CSV
  - `grading.directory`: where run folders are created
  - `grading.report.file`: score file name, default `scorereport.csv`
  - `extraction.directory`, `working.directory`, `anomaly.log`, `correction.log`
- Execution
  - `execution.java`, `execution.javac`: `JDK`, a command on `PATH`, or an executable path
  - `execution.compile.timeout.ms`: default 15000 per question
  - `execution.case.timeout.ms`: default 2000 per case
  - `execution.output.max.bytes`: captured stdout and stderr cap per stream, default 65536
  - `execution.workers`: 1 to 32 or `auto`, default 1 (commented out in the supplied file)
  - `execution.case.isolation`: `part` (default) or `case`
    - `part` reuses one Java process across a question's cases, refreshing student classes and
      fixtures. Each case keeps its own output files and timeout. A failed process is replaced.
    - `case` starts a new Java process for every case.
  - `execution.case.startup.timeout.ms`: process start-up limit, default 30000
- Marks
  - `grading.dynamic.enabled`: default `true`; `false` grades the supplied cases only
- Reports
  - `grading.html.enabled`, `grading.html.file`
  - `grading.feedback.enabled`, `grading.feedback.directory`,
    `grading.feedback.max.diagnostic.lines`
  - `dashboard.mode`: terminal progress display, `auto`, `ansi` or `plain`
- Advisory analyses (none of them changes marks or export decisions)
  - `plagiarism.enabled`, `plagiarism.algorithms` (`ngram`, `jplag` or both), `plagiarism.threshold`
    and `plagiarism.ngram.size`, `plagiarism.min.ngrams` (n-gram), `plagiarism.jplag.threshold`
    and `plagiarism.jplag.min.tokens` (JPlag), `plagiarism.starter.directory`
  - `grading.boilerplate.enabled`, `grading.boilerplate.starter.directory` and
    `grading.boilerplate.similarity.threshold` (starter-code comparison)
  - `grading.sanity.enabled` and the `grading.sanity.*` thresholds, commented out in the
    supplied file, with their defaults shown there
- Randomized mode: the `randomized.*` keys at the end of the file (see below)
- Assessment: `assessment.question.ids` and the `assessment.question.<id>.*` keys define each
  part's folder, source file, tester, case count, weights, support classes and fixtures.
  See "Limits" before changing them.

Invalid settings fail before grading starts.

## Behaviour rules

The command-line run does the following:

1. Load and validate the supplied properties file.
2. Read and validate the score sheet as the class roster.
3. Extract each ZIP into a fresh raw workspace, rejecting unsafe paths, corrupted data and
   excessive expansion.
4. Identify the student from the archive name, wrapper folders and source headers against the
   roster.
5. Check the configured question folders, source file names and required headers.
6. Prepare a separate working copy under the matched username, filling missing identity headers
   only when ownership is unambiguous.
7. Report problems and corrections, including duplicate submissions.
8. Compile each independently marked question in its own class folder.
9. Run the instructor and dynamic cases with the configured isolation, time limits and bounded
   captured output.
10. Export scores, per-case diagnostics and the reasons for any withheld scores into a new run
    folder.

Original ZIPs, instructor testers, starter files and the source score sheet are never modified.
Conflicting ownership is reported, not guessed. A missing source file is not replaced by starter
code. The batch continues after an individual failure.

Preparation status:

- READY: preparation is complete. It does not mean the student passed.
- REVIEW_REQUIRED: an instructor should inspect a problem. Name warnings and recoverable
  question problems do not discard valid independent questions.
- FAILED: preparation failed for that archive.

Marking:

- Each question compiles only its selected source, required companions, the supplied support
  classes and the adapter. Student `.class` files are ignored.
- A missing answer or companion file, or a syntax error, gives that question zero without
  blocking the others.
- Each case has a fresh working folder and fixture copies.
- A pass earns the configured points. A mismatch, runtime error, timeout or compile failure
  earns zero. A pass-loop-pass sequence keeps two marks.
- A missing or invalid result file, or `System.exit(0)` without a result, is a runtime error.
  Printed "Passed" messages cannot award marks.

Names and folders:

- A required question folder or source file with the wrong letter case is corrected when it is
  unique.
  - The working copy uses the expected name on every operating system, so the part is graded
    and exported everywhere, including Linux.
  - The submission is flagged REVIEW_REQUIRED and the correction is logged.
  - If several variants could match (ambiguous), nothing is repaired.
  - A submission in which no required question folder is found, exactly or as a unique
    wrong-case variant, has no usable working copy and is withheld.
- A mistyped wrapper folder name (username or student ID) is only a warning when other evidence,
  such as the archive name or source headers, names exactly one student.
  - A wrapper that names a different real student is a conflict, and the submission is withheld.
  - A mistyped wrapper with no other matching evidence is withheld.

Withholding:

- A submission is withheld, with its reason in `submissions.csv`, when:
  - its identity cannot be resolved;
  - another archive resolves to the same student: all of that student's archives are
    withheld, including the original;
  - its preparation is unusable;
  - a grading infrastructure error occurs.
- Name warnings alone do not withhold a uniquely matched student.
- Withheld students keep their original numerator in the score sheet.
- Every submission has a record for every configured case; blocked cases are NOT_RUN.
- With the default `part` isolation, a grader host (the shared Java process for a question):
  - that cannot start on both attempts is a grader fault: it is an infrastructure error and
    the student is withheld;
  - that dies during a case is retried once on a fresh host, then the case is recorded as
    RUNTIME_ERROR worth zero, and the student is still exported.

Security:

- Child processes and deadlines are reliability controls, not an operating-system sandbox.
- Student programs run with the current user's permissions and can attempt file, network or
  process access, or tamper with the result protocol. Descendant cleanup is best effort.
- Use an externally isolated account, container or VM for untrusted submissions. ByteGrade does
  not provision one.

## Randomized question mode

The ZIP batch workflow above is the default. A separate mode generates student resources and
grades one extracted submission, or one question across a folder of ZIPs, with seeded random
inputs. `<...>` marks a value you supply.

```sh
sh run.sh --randomized-resources Q1a config.properties
sh run.sh --randomized-grade Q1a <extracted-submission-folder> config.properties 482910374651
sh run.sh --randomized-grade-batch Q1a <folder-of-zips> config.properties 482910374651
```

- On Windows, use `run.bat` with the same arguments.
- Registered question IDs: Q1, Q2, Q1a, Q1b, Q2a, Q2b and Q3. `--help` lists them.
- The final seed argument is optional and must be a whole number. Without it the app picks one
  and records it in the result so a run can be replayed.
- The `randomized.*` keys in `config.properties` set the resources and results folders, the
  java and javac commands, runs per case, timeout and output limits.
- Run these commands from the project folder (see "Limits").

## Limits

- Reusing the grader for a different assessment needs new adapters or registered Java classes.
  Configuration alone does not turn arbitrary testers into runnable cases, because three things
  are compiled in:
  - the dispatch from question to adapter
  - the SHA-256 fingerprints of the five supplied tester files (an edited tester is rejected)
  - the case counts
- The randomized graders for Q2a and Q2b resolve their fixtures as
  `Project/Tester-Files/...` relative to the working directory. Run them from the project root.
- The Q2 fixture files are small (`personstester.txt` has 11 lines, `studentstester.txt` 8), so
  Q2 coverage depends mostly on the dynamic checks.
- No operating-system sandbox; see "Security" above.
- On Windows, the worker count that works depends on the machine (see "Choosing the number of
  workers").
- High worker counts on any machine can push slow but correct submissions past the case timeout.

## Folder directory by feature

Paths in this section are under `src/main/java/autograder/` unless they begin with `src/`,
`Project/`, `.mvn/` or are root files. A trailing `/` is a folder.

- Grading core and pipeline
  - `App.java`: command-line entry point, argument parsing and exit codes
  - `GradingService.java`: one grading session; shared by the command line and the GUI
  - `RunOutcome.java`, `RunListener.java`, `ConsoleRunListener.java`: run result and progress hooks
  - `grading/GradingPipeline.java`: compile, run cases, aggregate scores, hand off to reports
  - `grading/InstructorTestCaseProvider.java`: maps the five supplied testers to their adapters
  - `grading/CombinedTestCaseProvider.java`: merges supplied and dynamic cases and splits marks
  - `grading/GradingDiagnosticsWriter.java`: writes `cases.csv` and `submissions.csv`
  - `grading/GradingRun.java`: the finished run
  - `report/CsvScoreReportWriter.java`: fills scores into a new copy of the score sheet
  - `report/ReportFiles.java`: fixed report file names
  - `roster/ScoreSheetRepository.java`: reads the class list and keeps the original fields
  - `csv/CsvCodec.java`: CSV quoting and line breaks
  - `model/`: data carried between components (students, submissions, cases, grades, findings)
  - `api/`: interfaces that reports, analyses and case providers implement
  - `src/main/resources/autograder/adapters/Q1aAdapter.java` and the Q1b, Q2a, Q2b and Q3
    adapters: one per supplied tester, compiled with the student's code
- Submission preparation and identity
  - `submission/SubmissionProcessor.java`: runs preparation for the whole batch
  - `submission/SafeZipExtractor.java`: ZIP extraction with path, size and checksum checks
  - `submission/SubmissionValidator.java`: finds question folders and rejects ambiguous layouts
  - `submission/IdentityEvidenceCollector.java`: reads names and headers from the sources
  - `submission/RosterIdentityMatcher.java`: matches evidence to one roster student
  - `submission/IdentityResolver.java`: combines the evidence and plans header repairs
  - `submission/CaseVariants.java`, `submission/ExactEntries.java`: unique wrong-case repair
  - `submission/WorkingCopyPreparer.java`: builds the corrected working copy
  - `submission/HeaderCorrectionPlanner.java`, `submission/HeaderCorrectionApplier.java`
  - `submission/CorrectionLog.java`, `util/AnomalyLog.java`: the two logs under `build/`
- Execution and isolation
  - `execution/StudentCompiler.java`: compiles one question's sources with the adapter
  - `execution/ProcessRunner.java`: deadlines and bounded captured output
  - `execution/CaseExecutor.java`: one Java process per case (`case` isolation)
  - `execution/PartCaseRunner.java`, `execution/CaseHost.java`, `execution/CaseHostProcess.java`:
    one shared Java process per question (`part` isolation), with retry and start-up checks
  - `execution/CaseResultReader.java`: reads the explicit per-case result file
  - `execution/FixtureDirectory.java`, `execution/DirectoryCleaner.java`
  - `config/ExecutionConfig.java`, `config/CaseIsolation.java`
- Parallel grading
  - `grading/SubmissionScheduler.java`: runs submissions on a bounded worker pool
  - `config/WorkerLimits.java`: logical processor count, `auto`, and the cap with a warning
  - `WorkersOption.java`: parses `--workers`
  - `gui/WorkerChoice.java`: the GUI dropdown values
- Swing GUI (`gui/`)
  - `gui/GuiLauncher.java`: entry point; `gui/MainWindow.java`: the window
  - `gui/RunBar.java`, `gui/GradingRunWorker.java`: Grade, Stop, Workers and the background run
  - `gui/ResultsPanel.java`, `gui/RunArtifacts.java`, `gui/OutputViewer.java`: result buttons
  - `gui/ConfigEditor.java`, `gui/ConfigFileStore.java`: Edit config...
  - `gui/ReadinessChecker.java`, `gui/ReadinessMonitor.java`: inline field checks
  - `gui/ResultsExport.java`, `report/XlsxWorkbookWriter.java`: Export all...
- Terminal dashboard
  - `ui/TerminalDashboard.java`: progress lines, with ANSI colour when supported
  - `ui/DashboardSettings.java`: reads `dashboard.mode`
  - `ui/TerminalCancellation.java`: `q` then Enter cancels
  - `ui/ConsoleProgressListener.java`, `DashboardRunListener.java`
- HTML dashboard
  - `report/html/OfflineHtmlReportGenerator.java`: writes the single self-contained file
  - `report/html/`: the other classes there render the table, details, summary and analyses
  - `src/main/resources/autograder/html/dashboard.css`, `dashboard.js`: embedded styles and script
  - `config/HtmlReportConfig.java`
- Markdown feedback
  - `report/feedback/MarkdownFeedbackGenerator.java`: one file per student
  - `report/feedback/FeedbackFileWriter.java`, `report/feedback/MarkdownDiagnosticFormatter.java`
  - `config/FeedbackConfig.java`
- Plagiarism detection
  - `plagiarism/PlagiarismDetectorFactory.java`, `plagiarism/CombinedPlagiarismDetector.java`:
    build the configured algorithms; one failing algorithm does not discard the others
  - `plagiarism/NGramPlagiarismDetector.java`: normalized token n-grams and Jaccard similarity
  - `plagiarism/JPlagPlagiarismDetector.java`: runs the JPlag library over Java syntax tokens
  - `plagiarism/JavaTokenizer.java`, `plagiarism/NGramBuilder.java`,
    `plagiarism/SimilarityCalculator.java`, `plagiarism/QuestionFiles.java`
  - `config/PlagiarismConfig.java`
- Starter-code comparison
  - `analysis/boilerplate/StarterCodeDetector.java`: flags answers that match the starter
  - `analysis/boilerplate/MethodComparison.java`: method-level differences
  - `analysis/boilerplate/StarterComparisonReport.java`: `starter-comparison/index.html`
  - `StarterAnalysis.java`: runs the analysis and writes `starter-analysis.csv`
  - `config/BoilerplateConfig.java`
  - `src/main/resources/autograder/boilerplate/`: report page template and script
- Dynamic and randomized grading
  - `grading/DynamicTestCaseProvider.java`: five seeded property checks per question
  - `src/main/resources/autograder/adapters/DynamicRunner.java`: runs them without stored answers
  - `RandomizedCommands.java`: the `--randomized-*` commands
  - `autotester/question/`: randomized question definitions and `QuestionRegistry.java`
  - `autotester/generator/`: random input generators
  - `autotester/execution/`: resource generation and single or batch randomized grading
  - `src/main/resources/autograder/autotester/`: student runner and lab-test support classes
- Score sanity checks
  - `SanityAnalysis.java`: runs the checks and writes `sanity-checks.csv`
  - `analysis/sanity/SanityChecker.java`: the rules; `analysis/sanity/SanityCsvWriter.java`
  - `config/SanityConfig.java`
- Demo cohort generator
  - `demo/DemoCohortGenerator.java`: builds `build/demo-cohort`
  - `demo/DonorArchive.java`, `demo/HeaderRewriter.java`, `demo/SyntheticArchiveWriter.java`,
    `demo/SyntheticRoster.java`
- Configuration
  - `config.properties`, `demo.properties`: root-level settings files
  - `config/AppConfig.java`, `config/AppConfigLoader.java`, `config/AppConfigGroups.java`
  - `config/AssessmentConfigLoader.java`: the `assessment.*` keys
  - `config/RunSettings.java`: all settings for one run
  - `config/PropertyParsers.java`, `config/ConfigPathPolicy.java`,
    `config/RuntimePathOverrides.java`
- Tests
  - `src/test/java/autograder/`: JUnit 5 tests mirroring the main packages, using temporary
    synthetic inputs
- Scripts and build
  - `compile.sh`, `compile.bat`: build the JAR without tests
  - `run.sh`, `run.bat`: window with no arguments, command line otherwise
  - `demo.sh`, `demo.bat`: generate and grade the demo cohort
  - `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`: build settings and the Maven Wrapper
- Supplied inputs (`Project/`, never modified)
  - `Project/student-submission/`: the six student ZIPs
  - `Project/Tester-Files/`: instructor testers and text fixtures
  - `Project/RenameToYourUsername/`: starter sources and supplied support classes
  - `Project/IS442-ScoreSheet.csv`: class list and required report format
  - `Project/IS442-ScoreSheet-graded.csv`: supplied reference marks
  - `Project/groundtruth.txt`: per-question reference marks; its Xing components sum to 15 but it
    states a total of 1
  - `Project/IS442-ExamSample.pdf`: the sample assessment

## Third-party dependencies and licences

Maven downloads these and bundles the runtime ones into the JAR, so nothing is installed by
hand and the JAR runs on its own. The GUI uses Swing from the JDK.

Runtime, bundled in the JAR:

- `de.jplag:jplag`, `de.jplag:java`, `de.jplag:language-api` 4.3.0 (GPL-3.0)
  - 4.3.0 is the newest JPlag release built for Java 17.
- `org.slf4j:slf4j-nop`, `org.slf4j:slf4j-api` 2.0.7 (MIT)
- `com.fasterxml.jackson.core:jackson-databind`, `jackson-core`, `jackson-annotations` 2.14.2
  (Apache-2.0)
- `org.apache.commons:commons-math3` 3.6.1 (Apache-2.0)
- `org.kohsuke.metainf-services:metainf-services` 1.9 (MIT)

Test only, not in the JAR:

- JUnit 5.11.4 (EPL-2.0)

Build tool:

- Maven Wrapper (Apache-2.0)

## Troubleshooting

- `java` or `javac` is not found
  - Install a JDK 17 or newer, check `JAVA_HOME` and `PATH`, and reopen the console.
  - A JRE alone does not supply `javac`.
- `Build failed. Check that javac -version and JAVA_HOME point to a JDK 17 or newer.`
  - The Maven Wrapper must run on a full JDK 17 or newer.
- The build cannot download Maven, a plugin or a dependency
  - Check internet or proxy access, then run the compile script again.
- `target/auto-grading-system-1.0-SNAPSHOT.jar not found. Run sh compile.sh first.`
  - Build successfully before using `run` or `demo` (`compile.bat` on Windows).
- The shell or console cannot find `compile.sh`, `compile.bat` or `mvnw`
  - Run the command from inside the unzipped folder that contains `compile.sh`.
- Missing configuration or input files
  - Keep the supplied `Project/` folder and check the configured paths.
  - Paths inside a properties file resolve relative to that file.
- Passing cases reported as timed out when using several workers
  - Rerun with `--workers 1` (see "Choosing the number of workers").
- `could not complete grading: file not found: <path>`
  - The configuration file or score sheet path is wrong. Check the path in the properties file.
- `submission preparation failed: not a valid ZIP file: <name>` in a submission's issues
  - That archive is corrupt or not a ZIP, so it fails preparation and is withheld. The other
    archives are still graded. Ask the student to resubmit.
- The window does not open
  - It needs a desktop session. On a headless machine use the command line instead.

## AI use declaration

The team wrote the code by hand. We used AI for high-level guidance on bonus feature ideas, project structure, and how to divide responsibilities between classes. We also used it to help with complex debugging, but only after attempting to diagnose and fix issues ourselves.
