# If Feature 4's Question-Object Bonus Were Integrated

> **Purpose:** A personal design guide for William, in case the instructor asks what would be needed to connect the Question-object randomized grader to the normal grading workflow.
>
> **Status:** Proposal only. The current Question-object mode is a separate optional workflow. This document describes a possible future integration; it does not claim that integration exists, and no application code was changed to create this guide.

## Short answer

The existing Feature 4 path already adds deterministic randomized tests to the regular grading pipeline. The separate Question-object path adds a richer way to declare a question: its Java method signature, typed input generators, fixtures, answer key, and comparison rules live together in a `Question` object.

To integrate that model, the program would need to connect it to the normal assessment configuration, prepared submissions, per-question grading, score aggregation, and reports. The main redesign question is how to turn typed randomized trials and their answer-key results into the existing case and score model.

This does **not** automatically require replacing the whole application. A focused adapter inside the existing grading flow is plausible. Replacing the grading flow or moving submission intake and reporting into the randomized grader would be a much larger and riskier change.

## What exists today

### Regular grading workflow

The normal application path reads the assessment and execution configuration, prepares all ZIP submissions, resolves identities and working copies, grades configured questions, writes the score and diagnostic CSV files, then runs optional reports such as HTML and Markdown feedback. `GradingService` coordinates this run and `GradingPipeline` performs grading.

`GradingPipeline` currently combines the fixed instructor cases with Feature 4's dynamic cases. Both are represented as `TestCaseDefinition` objects and contribute to the ordinary `StudentGrade`, batch result, score sheet, and reports.

Feature 4's existing dynamic cases use a question ID and deterministic seed as runner arguments. Their runner generates trials and checks the configured invariant. This is integrated into the normal grading workflow already.

### Separate Question-object workflow

The `autograder.autotester.Question` model describes a question's method signature, parameter types, generators, artifacts, comparison settings, and `answerKey` method. `QuestionRegistry` supplies the known question definitions.

The `--randomized-grade` command grades one prepared student folder. `--randomized-grade-batch` accepts a ZIP directory and writes its own per-student text results and `summary.csv`. These commands are routed by `App`, but they do not go through `GradingService` or `GradingPipeline`; the batch command has its own archive extraction and question-source staging. It therefore does not currently populate the normal run's score sheet, `cases.csv`, or standard report hooks.

## What “integrated” should mean

For this proposal, integrated means an instructor can enable a Question-object question in the usual assessment configuration and then run the normal grading command. The ordinary submission intake and identity handling still apply, and the new question contributes marks to the same student grade, score sheet, and run reports as the existing questions.

If the requirement is only to offer an optional standalone randomized grader with its own summary file, that capability already exists; a unified pipeline change would not be necessary.

## Proposed changes by area

| Area and likely location | Change needed | Why it is needed |
| --- | --- | --- |
| **Assessment model** — `autograder.model.QuestionDefinition`, `AssessmentDefinition`; `autograder.config.AssessmentConfigLoader` | Add an explicit way for an assessment question to identify a registered Question-object definition, its source-file/answer layout, enabled mode, trial count or seed policy, and mark allocation. Validate the definition before processing student ZIPs. | The standard assessment currently describes fixed case counts, points-per-case, tester, support files, and fixtures. It does not identify a Question-object class or its typed generators and answer key. The model needs a clear bridge without weakening existing assessment validation. |
| **Question registration and configuration bridge** — `autograder.autotester.QuestionRegistry` and a small integration class near `autograder.grading` | Resolve the configured question ID to an allowed, already registered `Question`; expose its metadata in a form the grading pipeline can use. Keep question selection explicit and validate IDs, signatures, artifacts, and duplicate IDs. | `QuestionRegistry` currently serves the standalone randomized commands. Normal assessment loading does not use it. Connecting them avoids a second, uncontrolled question-definition mechanism. |
| **Pipeline case creation** — new `QuestionObjectCaseProvider` (or similarly named adapter), wired into `GradingPipeline` | Produce deterministic trial inputs for each configured Question-object question and seed. The adapter should retain each trial's ID, typed arguments, expected answer, and question association. It should integrate alongside the existing fixed and dynamic providers. | The current `TestCaseProvider` path creates `TestCaseDefinition` values. Those only hold string arguments, a runner class, and a maximum score. Question-object trials have typed values (for example arrays or lists) plus an expected value or expected exception. A conversion boundary is required. |
| **Typed trial protocol and student execution** — `autograder.autotester.RandomizedTrialProtocol`, `RandomizedStudentRunner`, and/or a new grading adapter | Reuse the current isolated student execution protections, but pass typed trial inputs to the student runner and return structured outcomes for each trial. Keep answer-key evaluation in the trusted instructor process; do not send the answer key into the student JVM. | `RandomizedGrader` already has a typed serialization protocol and isolated child process. The regular case model and runner protocol are built around string arguments and case status. The integrated path has to preserve typed data, timeouts, output bounds, isolation, and useful error states. |
| **Expected-versus-actual comparison** — question-object grading adapter | Use the registered `Question` to generate expected results and apply its declared comparison policy (including text comparison and expected exceptions). Return an explicit outcome for each generated trial. | A generic string comparison is not equivalent to the Question object's typed comparison rules. The result must distinguish a wrong answer from a compile error, timeout, runtime exception, or grader/infrastructure error. |
| **Score aggregation** — `TestCaseDefinition`, `TestCaseResult`, `StudentGrade`, and `GradingPipeline` | Decide and implement how Question-object trial outcomes earn the configured question marks. Recommended design: preserve each trial as an individual case result, assign deterministic case IDs, and allocate the question's maximum marks across its trials. Keep total assessment marks stable and ensure compile/runtime failures score consistently with current policy. | `TestCaseResult.getScore()` awards the case maximum only for `PASSED`; the current `RandomizedResult` mainly summarizes pass counts and samples failures. A pass count alone cannot explain each row in the normal case diagnostics or preserve detailed scoring without a defined mapping. |
| **Submission preparation** — `GradingService`, `SubmissionProcessor`, `SubmissionResult` / working-copy preparation | Invoke the Question-object grader using the already prepared, identity-checked working copy from the normal batch. Do not make the integrated path extract and stage ZIPs a second time. Confirm how the required answer and companion sources are located in the prepared copy. | The standalone batch grader performs its own archive discovery, extraction, source lookup, and staging. Repeating those steps in the same normal run could produce different identity, path-safety, correction, or failure behavior. The normal workflow should remain the owner of submission intake. |
| **Run configuration** — `AppConfig` / assessment configuration and possibly `RunSettings` | Put the integrated mode's enablement, seed policy, maximum trial count, per-trial timeout, and output limits under the normal run configuration, reusing existing execution limits where their meaning matches. Retain standalone `RandomizedConfig` for standalone commands if those commands remain supported. | The normal run and standalone randomized mode currently have separate settings. Explicit configuration prevents surprising defaults and makes the feature reproducible and reviewable. Avoid maintaining two conflicting timeout or output-limit settings for one unified run. |
| **Diagnostics and reports** — `GradingDiagnosticsWriter`, `GradingRun`, report context, HTML/Markdown feedback, GUI results | Ensure the resulting case records are written to normal `cases.csv`, scored through the normal score sheet, and visible to applicable run reports. Show question/trial identifiers and concise failure evidence while respecting existing report and withheld-submission semantics. | A grader is not fully integrated if its marks appear only in a separate summary while the instructor's normal scores and reports omit them. The ordinary report pipeline consumes the `GradingRun`, so producing compatible results is the cleanest way to reuse it. |
| **Application entry points** — `App`, optionally GUI settings | Keep standalone resource generation and standalone grading commands available if useful. Add the unified mode through normal assessment configuration so CLI and, if intended, the GUI reach the same `GradingService` path. | Routing the feature through the shared service gives it normal preparation, cancellation/progress, grading, and report behavior. A second top-level pipeline would continue to feel like a separate tool. |
| **Validation and verification** — focused tests around the above classes and end-to-end run | Cover config validation, deterministic seed replay, correct and incorrect implementations, typed inputs, expected exceptions, compile failure, timeout, runtime error, missing source/artifact, identity/preparation blockers, mark totals, normal CSVs, and optional reports. Confirm existing fixed and dynamic questions retain their scores and behavior. | The integration touches public grading outcomes and report contracts. Evidence should demonstrate that the new mode works and that existing assessments are unchanged when it is disabled. |

The names above are likely change locations based on the current structure, not a promise that each file must be modified. A clean design could put most Question-object specifics in one adapter package and keep shared pipeline changes small.

## The main design decision: how do trial results become marks?

There are two reasonable choices:

1. **One normal case result per generated trial (recommended).** Allocate the configured question maximum across its generated trials. Each `TestCaseResult` then has a stable case ID and a normal status, so the score and `cases.csv` can explain exactly which generated input passed or failed. The allocation and rounding rules must be explicit.
2. **One aggregate result for the whole question.** Convert a pass ratio or another policy into one question score. This is simpler to summarize, but it needs a score model that can award partial marks; today's `TestCaseResult` awards either that case's full maximum or zero. It also hides which individual generated trials failed unless the aggregate result carries extra detail outside the existing case contract.

The first choice fits the existing per-case diagnostics better. It still requires deciding whether the number of generated trials is fixed by configuration, defined per registered `Question`, or a combination with a validated cap.

## Keep, integrate, or replace: pros and cons

| Option | Pros | Cons / costs | Fit |
| --- | --- | --- | --- |
| **Keep the Question-object grader standalone** | Lowest change risk; its specialized typed generators, answer keys, and randomized protocol stay cohesive; normal grading contracts remain untouched; useful as a clearly optional demonstration or separate command. | Instructor has to understand and run a second workflow; it has a separate summary/output directory; its marks do not automatically appear in the normal score sheet, case CSV, or standard reports; intake behavior may differ from the normal batch. | Best when the bonus is optional and there is little time before submission, or when a separate tool is acceptable. |
| **Integrate it as an optional question type in the existing pipeline** | One batch intake and identity decision; consistent working-copy and withholding behavior; one mark total and score sheet; normal diagnostics/reports can include its results; fixed, Feature 4 dynamic, and Question-object questions can coexist. | Requires shared model/config/pipeline work; typed arguments and expected values need a bridge; scoring and CSV/report details need decisions; requires regression and end-to-end verification; adds implementation and review risk close to a deadline. | Best if the instructor expects the bonus to be part of a normal instructor grading run and there is enough time for design, testing, and review. |
| **Replace the normal case system with the Question-object system** | One question-definition style might eventually simplify authoring for questions that fit its supported method signature and typed value model. | Largest change; risks losing or duplicating current fixed adapters, dynamic invariants, per-case score behavior, submission protections, and report assumptions; Question objects have their own current supported-type and Java-method constraints; not needed just to make this bonus usable. | Not recommended for this project unless there is a separate, approved redesign requirement. |

**Key distinction:** integrating the Question-object mode does not have to mean switching the entire application to it. The sensible integrated design is an additional optional question type that feeds the existing `StudentGrade` and `GradingRun` contracts. The existing fixed and Feature 4 dynamic question paths can remain in place.

## Proposed execution order if integrated

1. Instructor starts the normal CLI or GUI grading run and loads the ordinary run/assessment configuration.
2. Configuration validation resolves the enabled Question-object question through the registry and checks its sources, artifacts, mark allocation, seed/trial policy, and execution limits.
3. `GradingService` constructs the usual pipeline and calls `SubmissionProcessor` once. Normal archive safety, root selection, identity matching, correction/withholding decisions, and working-copy preparation happen as they do today.
4. The pipeline schedules each eligible student using the existing grading scheduler. For each Question-object question it builds deterministic typed trials and evaluates the instructor answer key in the trusted host process.
5. The student's answer and required companions are compiled in the normal prepared workspace. Each generated trial is sent through the isolated student runner under the configured limits.
6. The adapter compares each student outcome with the corresponding expected result and converts it to a normal case status and allocated score. Blocked or withheld submissions follow the existing policy and do not get silently treated as ordinary failures.
7. The pipeline aggregates all fixed, Feature 4 dynamic, and Question-object case results into the usual grades and writes the normal score and diagnostic CSVs.
8. `GradingService` runs the existing optional analysis/report hooks against the completed `GradingRun`; the instructor sees the Question-object marks in the same run outputs.

The sequence diagram below is a **proposed future flow**, not a description of the current standalone Question-object commands.

```mermaid
sequenceDiagram
    actor Instructor
    participant App as App CLI or GUI
    participant Service as GradingService
    participant Config as Assessment Config and Registry
    participant Intake as SubmissionProcessor
    participant Prep as Prepared Working Copies
    participant Pipeline as GradingPipeline
    participant Provider as Question Object Adapter
    participant Key as Question Answer Key
    participant Compiler as StudentCompiler
    participant Runner as Isolated Student Runner
    participant Student as Student JVM
    participant Writer as GradingDiagnosticsWriter
    participant Reports as Optional Report Hooks

    Instructor->>App: Start normal grading run
    App->>Config: Load and validate assessment
    Config-->>App: Assessment with enabled question objects
    App->>Service: Run settings and progress listener
    Service->>Pipeline: Construct regular grading pipeline
    Service->>Pipeline: Validate Java tools and instructor resources
    Service->>Intake: Process submission batch once
    Intake->>Prep: Extract safely and prepare working copies
    Prep-->>Intake: Prepared submissions and intake decisions
    Intake-->>Service: Submission results
    Service->>Pipeline: Grade prepared submissions

    loop Each eligible student
        Pipeline->>Provider: Build seeded trials for configured object question
        Provider->>Key: Generate expected answer for typed input
        Key-->>Provider: Expected value or expected exception
        Pipeline->>Compiler: Compile answer and required companion sources
        Compiler-->>Pipeline: Compiled student question or compile failure
        loop Each generated trial
            Pipeline->>Runner: Execute typed input with limits
            Runner->>Student: Invoke student's target method
            Student-->>Runner: Actual value, exception, timeout, or process failure
            Runner-->>Pipeline: Structured trial outcome
            Pipeline->>Provider: Compare expected and actual; create case result
        end
        Pipeline->>Pipeline: Aggregate trial marks into student grade
    end

    Pipeline->>Writer: Write normal score and diagnostic files
    Writer-->>Pipeline: Core grading outputs
    Pipeline-->>Service: Completed GradingRun
    Service->>Reports: Generate enabled reports from same run
    Reports-->>Service: Report status and artifacts
    Service-->>App: Run outcome and output locations
    App-->>Instructor: Show completion and results
```

## A practical answer if the instructor asks

You can describe it this way:

> “Feature 4's dynamic test cases are already included in the regular grading pipeline. I also built a Question-object randomized grader where each registered question contains typed generators and an answer key. That mode currently runs as a separate optional command, so its results do not yet feed the normal score sheet and reports. To fully integrate it, we would add a configuration and adapter layer that turns its typed trials and expected answers into normal per-case results, run it on the working copies prepared by the main workflow, and pass those results through the existing scoring and reporting path. That is possible without replacing the whole grader, but it adds shared model, scoring, and verification work. Given the deadline, I would keep it as a clearly labeled standalone bonus unless unified reporting is a required acceptance criterion.”

This explains both the value of the bonus and its current boundary without implying that the main workflow already uses the Question-object model.

## Scope and recommendation

The lowest-risk near-term choice is to keep the current Question-object mode optional and standalone, document its command and output, and describe integration as future work. If the instructor specifically requires the feature to grade through the normal batch and produce normal marks/reports, implement the adapter integration as a separate approved follow-up, with the scoring contract agreed first.

The likely integration is additive rather than a system-wide replacement. Its hardest parts are the data bridge from typed trials to normal case results, a fair mark-allocation rule, and preserving the normal submission/report contracts. Submission scanning, identity resolution, the scheduler, and optional report orchestration can largely remain the existing system's responsibility.
