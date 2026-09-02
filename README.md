# CS102 Auto Grading System

A configurable Java console application for grading batches of Java submissions. It
extracts student ZIP files, identifies their owners, normalizes common directory
mistakes, compiles each question, runs each test case in an isolated JVM process, and
exports final marks in the supplied scoresheet format.

The application uses only the Java standard library; no third-party dependencies or
build tools are required.

## Features

- Batch grading of all ZIP files in a submission directory
- Support for nested, flat, incorrectly named, and student-number root folders
- Identity resolution using ZIP names, student IDs, Java headers, and folder names
- Five-part grading for `Q1a`, `Q1b`, `Q2a`, `Q2b`, and `Q3`
- Fallback support when a submission provides only `Q2.java`
- A separate JVM and configurable timeout for every individual test case
- Continued grading after compilation errors, exceptions, or infinite loops
- Trusted instructor fixtures that cannot be replaced by student-submitted copies
- Clear progress messages and non-penalizing anomaly reports
- CSV output that preserves the original roster columns and row order
- External configuration through `config.properties`

## Instructor quick start

### 1. Install the prerequisites

The machine must have:

- Bash, such as macOS/Linux Bash, Git Bash, or Windows Subsystem for Linux
- JDK 11 or newer
- `java` and `javac` available on `PATH`

Confirm the Java installation:

```bash
java -version
javac -version
```

Both commands should complete successfully. A Java Runtime Environment without
`javac` is not sufficient; a full JDK is required.

### 2. Prepare the grading inputs

The default configuration expects this layout:

```text
Project/
├── student-submission/
│   ├── student-one.zip
│   ├── student-two.zip
│   └── ...
├── Tester-Files/
│   ├── Q1aTester.java
│   ├── Q1bTester.java
│   ├── Q2aTester.java
│   ├── Q2bTester.java
│   ├── Q3Tester.java
│   ├── personstester.txt
│   └── studentstester.txt
├── RenameToYourUsername/
│   └── trusted exam support files
└── IS442-ScoreSheet.csv
```

Place all student ZIP files directly in `Project/student-submission/`. Do not unzip or
rename them manually. The grader extracts every archive into a temporary workspace and
never modifies the original submission.

The scoresheet must retain these column names:

- `OrgDefinedId`
- `Username`
- `First Name`
- `Email`
- `Calculated Final Grade Numerator`

Other columns are preserved without modification.

### 3. Review the configuration

Open `config.properties` and verify the input and output paths. The supplied defaults
are ready to run with the repository's `Project/` directory.

Common settings include:

| Property | Purpose | Default |
|---|---|---|
| `submissions.directory` | Directory containing student ZIP files | `Project/student-submission` |
| `testers.directory` | Trusted testers and data fixtures | `Project/Tester-Files` |
| `scoresheet.input` | Roster and scoresheet template | `Project/IS442-ScoreSheet.csv` |
| `scoresheet.output` | Generated scoresheet | `Project/IS442-ScoreSheet-graded.csv` |
| `test.timeout.seconds` | Maximum runtime for one test case | `5` |
| `compile.timeout.seconds` | Maximum compilation time per question | `30` |
| `java.compiler.command` | Java compiler executable | `javac` |
| `java.runtime.command` | Java runtime executable | `java` |
| `working.cleanup` | Remove temporary run files afterward | `true` |

Question filenames, tester filenames, fixtures, supporting classes, and maximum marks
are also defined in this file instead of being hard-coded in the application.

### 4. Compile the application

From the repository root, run:

```bash
./compile.sh
```

The script compiles every Java file under `src/` and stores the application class files
under `classes/`. A successful build prints the number of compiled source files.

If the shell reports `Permission denied`, make the scripts executable once:

```bash
chmod +x compile.sh run.sh
```

### 5. Run the grader

After compilation, run:

```bash
./run.sh
```

The console displays each student, question score, compilation problem, timeout, and
detected submission anomaly. A final summary is printed when the batch finishes.

The completed scoresheet is written by default to:

```text
Project/IS442-ScoreSheet-graded.csv
```

The report is written atomically, so an existing output file is replaced only after a
complete new CSV has been prepared.

### Using another configuration file

After compiling, a different assessment configuration can be supplied with:

```bash
./run.sh --config /absolute/path/to/config.properties
```

Relative paths in that configuration are resolved from the directory containing the
configuration file.

## Supported student directory layouts

The expected layout is:

```text
username.zip
└── username/
    ├── Q1/
    │   ├── Q1a.java
    │   └── Q1b.java
    ├── Q2/
    │   ├── Q2a.java
    │   └── Q2b.java
    └── Q3/
        ├── Q3.java
        └── ShapeComparator.java
```

The grader also detects and logically corrects:

- ZIP files containing `Q1/`, `Q2/`, and `Q3/` directly
- Placeholder roots such as `RenameToYourUsername`
- Roots named with a student organization ID
- Additional nested wrapper directories
- Incorrect filename capitalization
- A single `Q2.java` containing the Q2 implementation

Corrections occur only in a temporary grading workspace. Structural anomalies are
reported but do not deduct marks.

## Grading and timeout behavior

Instructor tester files are converted at runtime into temporary one-case runners. Each
runner emits an `AUTOGRADER_SCORE=` marker and executes in a fresh JVM. Student console
output therefore cannot be mistaken for the final score.

If a test case exceeds the configured timeout, its JVM is terminated, that case earns
zero, and the next case still runs. A compilation failure gives zero for the affected
question but does not stop other questions or students from being graded.

Submitted `.class` files and submitted tester-data copies are ignored. The grader uses
the instructor-owned fixtures in `Project/Tester-Files/` and trusted support classes
listed in `config.properties`.

`Project/groundtruth.txt` is a reference artifact only. It is not read by the grader
and cannot override results produced by compilation and test execution.

## Project structure

```text
.
├── src/autograder/
│   ├── config/              Configuration loading and validation
│   ├── model/               Grading and diagnostic data models
│   ├── report/              CSV roster reading and report generation
│   ├── service/
│   │   ├── api/             Extension interfaces
│   │   ├── grading/         Question and batch orchestration
│   │   ├── process/         javac/java process execution
│   │   ├── submission/      Extraction, identity, and normalization
│   │   └── testing/         Tester adaptation and JVM isolation
│   ├── ui/                  Console entry point and progress output
│   └── util/                Focused file-tree operations
├── classes/                 Compiled application output
├── Project/                 Default grading inputs
├── config.properties
├── compile.sh
└── run.sh
```

## Extension points

The core workflow depends on interfaces in `autograder.service.api`:

- Implement `TestSuiteProvider` to support another tester format or dynamic tests.
- Implement `ReportGenerator` to add HTML or another report format.
- Implement `AnomalyDetector` to add submission-level checks.
- Implement `BatchAnalyzer` for cross-submission checks such as plagiarism detection.
- Implement `TestExecutionStrategy` to provide another isolation mechanism.

These components can be added without changing the core grading engine.

## Troubleshooting

### `Application is not compiled`

Run `./compile.sh` before `./run.sh`.

### `java` or `javac` cannot be found

Install a full JDK and add its `bin` directory to `PATH`, or set the executable paths in
`config.properties`.

### A submission receives zero for one question

Read the console message for that question. Missing source files, compiler errors,
tester compilation failures, exceptions, invalid score output, and timeouts are
reported separately.

### A student is not matched to the roster

Check that the ZIP filename, student-number folder, Java header email, or fallback
folder name matches a row in `IS442-ScoreSheet.csv`. Identity evidence is considered in
that order.

### Temporary files are needed for diagnosis

Set `working.cleanup=false` before running. The generated sources, compiler output, and
question workspaces will remain under `.autograder-work/` for inspection. Restore the
setting to `true` for normal use.

## Security notice

ZIP extraction is protected against path traversal and configurable archive-size
limits, and test JVMs are stopped after their timeout. However, executing arbitrary
student Java code is not a complete operating-system sandbox. Student code runs with
the same filesystem and network permissions as the instructor account.

For real assessments, run the grader in a disposable virtual machine, container, or
restricted operating-system account that contains no personal files, saved credentials,
or sensitive network access.

## Publishing this repository

Before making a school-project repository public, remove or anonymize student
submissions, names, email addresses, scoresheets, and any documents marked restricted.
Confirm that you have permission to redistribute the exam materials and tester files.
Public examples should use synthetic submissions and fictional identities.

Add an appropriate `LICENSE` file before inviting third parties to copy, modify, or
redistribute the project.
