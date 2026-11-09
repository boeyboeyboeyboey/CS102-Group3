# Application Sequence Diagrams

These diagrams show how a request moves through the current application. The
regular grading path is the default. The optional Question-object mode branches
from `App` and does not enter the regular score pipeline.

## 1. Full regular grading run

The CLI and Swing window use the same `GradingService`. Their setup and final
display differ; the preparation, grading and reporting work is shared.

```mermaid
sequenceDiagram
    actor User
    participant App as App CLI
    participant GUI as Swing GUI
    participant Worker as GradingRunWorker
    participant Settings as RunSettings
    participant Service as GradingService
    participant Pipeline as GradingPipeline
    participant Providers as TestCaseProviders
    participant Intake as SubmissionProcessor
    participant Zip as SafeZipExtractor
    participant Identity as IdentityResolver / Validator
    participant Copy as WorkingCopyPreparer
    participant Scheduler as SubmissionScheduler
    participant Compiler as StudentCompiler
    participant Cases as PartCaseRunner
    participant Host as CaseHostProcess
    participant Writer as GradingDiagnosticsWriter
    participant Reports as App report hooks

    alt CLI regular run
        User->>App: java -jar app.jar config.properties
        App->>App: Parse args and enter runGrading
        App->>Settings: Load and validate configuration
        Settings-->>App: RunSettings
        App->>Service: run(settings, progress listener)
    else GUI regular run
        User->>GUI: Select inputs and click Grade
        GUI->>Settings: Load settings with selected path overrides
        Settings-->>GUI: RunSettings
        GUI->>Worker: Start background task(settings)
        Worker->>Service: run(settings, progress relay)
    end

    Service->>Pipeline: Construct pipeline
    Pipeline->>Providers: Load fixed and dynamic case definitions
    Providers-->>Pipeline: Combined, weighted case list
    Service->>Pipeline: validateEnvironment()
    Pipeline->>Pipeline: Check Java tools and instructor resources
    Pipeline-->>Service: Environment ready

    Service->>Intake: processBatch()
    Intake->>Intake: Read score sheet and find matching ZIPs
    loop Each submission archive
        Intake->>Zip: Extract with path and size checks
        Zip-->>Intake: Extracted files or archive error
        Intake->>Identity: Select question root, inspect files and resolve identity
        Identity-->>Intake: Student identity, warnings and proposed corrections
        opt Identity and layout permit a working copy
            Intake->>Copy: Prepare canonical copy and apply approved repairs
            Copy-->>Intake: Working-copy path and correction records
        end
        Intake->>Intake: Record preparation status and anomalies
    end
    Intake->>Intake: Mark duplicate roster identities after intake
    Intake-->>Service: SubmissionResult list

    alt No matching archives
        Service-->>App: No submissions outcome
        App-->>User: Explain that no archives matched
    else Submissions found
        Service->>Pipeline: grade(submissions)
        Pipeline->>Scheduler: Schedule submissions using configured workers
        loop Each submission
            Scheduler->>Pipeline: Grade one submission
            alt Identity, duplicate or preparation blocker
                Pipeline->>Pipeline: Withhold score export and mark cases not run
            else Eligible submission
                loop Each assessment question
                    Pipeline->>Compiler: Compile answer, companion, support files and adapter
                    Compiler-->>Pipeline: Compile result
                    alt Compile error or deadline
                        Pipeline->>Pipeline: Mark this question's cases as compile errors
                    else Compiled
                        Pipeline->>Cases: Run fixed and dynamic cases for this part
                        opt Default PART isolation and no live host
                            Cases->>Host: Start host JVM with compiled classes
                            Host-->>Cases: Ready
                        end
                        loop Each case in the question part
                            Cases->>Cases: Create case result/output directory
                            Cases->>Host: Send runner class, args and case path
                            Host->>Host: Create fresh class loader and invoke runner main
                            alt Fixed instructor case
                                Host->>Host: Run question adapter
                                Host->>Host: Adapter invokes student method and checks fixed example
                            else Feature 4 dynamic case
                                Host->>Host: Run DynamicRunner with question ID and seed
                                loop Up to 25 generated trials
                                    Host->>Host: Generate input and check question invariant
                                end
                                Host->>Host: Write PASSED, FAILED or ERROR token
                            end
                            Host-->>Cases: Exit status, timeout/thread state and captured output
                            Cases->>Pipeline: CaseResultReader maps evidence to TestCaseResult
                            opt Host failure
                                Cases->>Host: Restart and retry the affected case once
                            end
                        end
                        Cases-->>Pipeline: Case results for this question part
                    end
                end
                Pipeline->>Pipeline: Sum passed-case marks and apply export eligibility
            end
            Pipeline-->>Scheduler: StudentGrade and withholding reason, if any
        end
        Scheduler-->>Pipeline: Ordered grades
        Pipeline->>Pipeline: Compare submissions for advisory similarity findings
        Pipeline->>Writer: Write cases, submissions, similarity and score-sheet CSVs
        Writer-->>Pipeline: Core grading artifacts in the run directory
        Pipeline-->>Service: GradingRun (grades, run path and withholding map)

        Service->>Reports: Run post-grading hooks in order
        Reports->>Reports: Starter analysis
        Reports->>Reports: Score sanity checks
        Reports->>Reports: Offline HTML dashboard, if enabled
        Reports->>Reports: Markdown feedback, if enabled
        Reports-->>Service: Report status and any report failures
        Service-->>App: RunOutcome
        alt CLI caller
            App->>App: Print outcome and elapsed run time
            App-->>User: Summary and output paths
        else GUI caller
            Service-->>Worker: RunOutcome
            Worker-->>GUI: Publish result and artifact locations
            GUI-->>User: Results, status and available report buttons
        end
    end
```

### Reading the regular path

1. **Configuration and case plan:** `App` loads settings, then `GradingService`
   constructs the pipeline. The pipeline combines fixed instructor cases with
   Feature 4 dynamic cases before checking that Java and the configured
   assessment resources are available.
2. **Preparation:** `SubmissionProcessor` safely extracts each archive,
   identifies its student and layout, records problems, and prepares a
   separate working copy when possible. Duplicate identities are checked
   after the full intake list is known.
3. **Compilation and execution:** `GradingPipeline` schedules submissions.
   Each eligible student's question part is compiled separately. The default
   `PART` mode reuses a host JVM for that part, but gives each case a fresh
   class loader and its own result/output directory. `CASE` mode instead
   starts a JVM per case. Feature 4 is simply another case runner in this
   loop: its generated cases invoke `DynamicRunner`.
4. **Scoring and export:** passed cases earn their configured share of the
   question's maximum. The combined provider keeps the five maxima at 3, 3, 5,
   5 and 4 points (20 total). Blocked submissions keep `NOT_RUN` evidence and
   are withheld from score export.
5. **Reports and presentation:** the pipeline writes the core CSVs first.
   Optional starter analysis, score sanity checks, HTML and Markdown reports
   run afterward in that order. Both the CLI and GUI receive a `RunOutcome`
   from the same service.

When more than one worker is selected, submissions may be graded concurrently
and returned in input order. That does not change the case definitions or
scoring rules. In the default case-host mode, a host-level failure is retried
once on a fresh host; an individual timed-out or abnormal case causes the host
to be replaced before the next case.

## 2. The regular case execution detail

This is the Feature 4 insertion point shown in the first diagram. There are
five dynamic case definitions per question; each definition generates up to
25 internal inputs using its fixed seed. The 25 internal trials determine one
case verdict and one case score.

```mermaid
sequenceDiagram
    participant Pipeline as GradingPipeline
    participant Combined as CombinedTestCaseProvider
    participant Fixed as InstructorTestCaseProvider
    participant Dynamic as DynamicTestCaseProvider
    participant Compiler as StudentCompiler
    participant Cases as PartCaseRunner
    participant Host as CaseHost / fresh case class loader
    participant Adapter as Fixed adapter or DynamicRunner
    participant Student as Student question class
    participant Reader as CaseResultReader

    Pipeline->>Combined: Load assessment cases
    Combined->>Fixed: Load fixed definitions
    Fixed-->>Combined: 20 fixed definitions
    Combined->>Dynamic: Load dynamic definitions
    Dynamic-->>Combined: 25 seeded definitions, five per question
    Combined->>Combined: Divide each question maximum across its cases
    Combined-->>Pipeline: Weighted case list, 20-point total preserved

    Pipeline->>Compiler: Compile one student's question part
    Compiler->>Compiler: Copy answer, companions and support classes
    Compiler->>Compiler: Add fixed adapter and DynamicRunner sources
    Compiler-->>Pipeline: Compiled classes or compile diagnostics
    Pipeline->>Cases: Run that part's case list
    loop Each case
        Cases->>Host: Create a clean case directory and ensure host is ready
        Cases->>Host: Send runner name, arguments and result path
        Host->>Adapter: Invoke runner main in fresh class loader
        alt Fixed case
            Adapter->>Student: Call required method for fixed test input
            Student-->>Adapter: Return value or exception
            Adapter->>Adapter: Apply the fixed assertion
        else Dynamic case (Feature 4)
            Adapter->>Student: Reflectively call method for generated input
            Student-->>Adapter: Return value or exception
            Adapter->>Adapter: Check specification invariant (no answer key)
            Adapter->>Adapter: Repeat with seeded inputs, stop at first failure
        end
        Adapter->>Host: Write case token/result and captured output
        Host-->>Cases: Host completion, exit state and captured streams
        Cases->>Reader: Read result token and process evidence
        Reader-->>Pipeline: Pass, fail, timeout or runtime error result
    end
    Cases-->>Pipeline: One result for every fixed and dynamic definition
```

The default host preserves separation between cases while saving JVM startup
time: `CaseHost` opens a new class loader for each case, captures that case's
standard output/error, and restores process-wide settings afterward. The
parent enforces the case deadline and replaces an abnormal host. This is
process management for grading reliability, not a security sandbox; student
code still runs with the grading account's operating-system permissions.

## 3. Optional Question-object commands

The `--randomized-*` commands are handled directly by `App.runRandomized`.
They do not call `GradingService` or the normal score-sheet pipeline.

```mermaid
sequenceDiagram
    actor User
    participant App as App randomized route
    participant Registry as QuestionRegistry
    participant Config as RandomizedConfig
    participant AppConfig as AppConfig
    participant Resource as ResourceGenerator
    participant Batch as RandomizedBatchGrader
    participant Zip as SafeZipExtractor
    participant Grader as RandomizedGrader
    participant Result as Result reports

    User->>App: --randomized-* question-id ... config [seed]
    App->>Registry: Find and validate question definition
    Registry-->>App: Question object
    App->>Config: Load randomized settings
    Config-->>App: Paths, tools, trial count and limits
    alt Generate practice resources
        App->>Resource: Generate stub, example tester and declared artifacts
        Resource-->>App: Resource file paths
        App-->>User: Print stub and tester locations
    else Grade one extracted submission
        App->>Grader: Grade question in submission folder
        Grader-->>App: RandomizedResult
        App->>Result: Write PASS/FAIL text report
        Result-->>App: Report path
        App-->>User: Print verdict, seed and report path
    else Grade selected question from a ZIP batch
        App->>AppConfig: Load archive matching settings
        AppConfig-->>App: Archive filename pattern and extraction limits
        App->>Batch: Grade matching archives with shared seed
        loop Each archive in filename order
            Batch->>Zip: Safely extract archive
            Zip-->>Batch: Extracted contents
            Batch->>Batch: Require one matching source and stage companions
            Batch->>Grader: Grade staged answer with same question and seed
            Grader-->>Batch: RandomizedResult
            Batch->>Result: Write per-archive text report
        end
        Batch->>Result: Write summary.csv
        Batch-->>App: Batch result and report paths
        App-->>User: Print per-archive verdicts and summary path
    end
```

## 4. Inside one Question-object grade

Single-submission grading and each archive in a batch use the same
`RandomizedGrader` path.

```mermaid
sequenceDiagram
    participant Grader as RandomizedGrader
    participant Question as Question definitions
    participant Javac as javac process
    participant Protocol as RandomizedTrialProtocol
    participant Runner as RandomizedStudentRunner
    participant Answer as Student answer method
    participant Report as Text result report

    Grader->>Question: Generate arguments with Random(seed)
    Question-->>Grader: Typed input arguments
    Grader->>Question: Compute expected results on copied arguments
    Question-->>Grader: Expected values or expected exception
    Grader->>Grader: Copy answer, companions and fixtures into temp workspace
    Grader->>Javac: Compile answer, support sources and child runner
    Javac-->>Grader: Compile result
    alt Compilation succeeds
        Grader->>Protocol: Serialize trial inputs
        Protocol-->>Runner: Child JVM reads trial inputs
        Runner->>Answer: Reflectively invoke public method for each trial
        Answer-->>Runner: Return value or exception
        Runner->>Protocol: Serialize student outcomes
        Protocol-->>Grader: Read outcomes
        Grader->>Grader: Compare with expected results and cap failure details
    else Compile failure or timeout
        Grader->>Grader: Record FAIL reason
    end
    Grader->>Report: Write PASS/FAIL, pass count, seed and failures
    Report-->>Grader: Report path
    Grader->>Grader: Delete temporary grading workspace
```

In this mode, `Question.answerKey` is the trusted oracle and the student
method runs in another JVM. That differs from Feature 4, where `DynamicRunner`
checks rules from the assessment without calculating expected answers. A
completed Question-object FAIL is still a completed command; it remains a
separate report and never changes the regular score sheet.

## Source map

- Entry points and command routing: `src/main/java/autograder/App.java`.
- Shared CLI/GUI orchestration: `src/main/java/autograder/GradingService.java`
  and `src/main/java/autograder/gui/GradingRunWorker.java`.
- Submission preparation: `src/main/java/autograder/submission/SubmissionProcessor.java`.
- Case construction, scheduling, scoring and export:
  `src/main/java/autograder/grading/`.
- Compilation and case hosting: `src/main/java/autograder/execution/`.
- Feature 4 runner: `src/main/resources/autograder/adapters/DynamicRunner.java`.
- Question-object system: `src/main/java/autograder/autotester/` and
  `src/main/resources/autograder/autotester/RandomizedStudentRunner.java`.
