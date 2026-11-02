# William's Autograder Guide

This guide explains two related but separate systems in this repository:

1. **Feature 4: dynamic tests in the regular CS102 grading pipeline.** These
   add scored cases to the normal 20-point assessment.
2. **The Question-object randomized autograder.** This is an optional testing
   mode with its own commands and PASS/FAIL reports. It does not change the
   normal score sheet.

The second system is the one where each question is represented by a `Question`
object. Both systems use randomized inputs, but they have different oracles,
execution paths and scoring behavior. Understanding that distinction is the
key to understanding your work.

## 1. Where each system starts

| System | Entry point | Main classes | Effect on marks |
| --- | --- | --- | --- |
| Regular grading with Feature 4 | `App`'s normal configuration command | `GradingPipeline`, `DynamicTestCaseProvider`, `CombinedTestCaseProvider`, `DynamicRunner` | Dynamic case results contribute to each question's existing maximum, totaling 20 points |
| Question-object randomized mode | `App`'s `--randomized-*` commands | `Question`, `QuestionRegistry`, `RandomizedGrader`, `RandomizedBatchGrader` | Writes independent PASS/FAIL results; does not update `scorereport.csv` |

The regular path answers: **How many marks did this submission earn on the
assessment, including extra randomized checks?** The Question-object path
answers: **Does this implementation satisfy this question's reference behavior
over a reproducible set of generated inputs?**

## 2. Feature 4: randomized cases in normal grading

### Why this design exists

The fixed instructor tests in `Project/Tester-Files` are necessary, but a
student could hard-code their known examples. Feature 4 adds generated inputs
and checks the behavior described by each question. It deliberately uses
**properties/invariants rather than a precomputed answer key**. The team does
not need to invent a second answer oracle for the supplied assessment.

Each dynamic case has a deterministic seed. This makes a generated trial
repeatable and avoids grading that changes unpredictably from run to run.

### Normal grading flow

1. `GradingPipeline` composes the fixed instructor provider and
   `DynamicTestCaseProvider` through `CombinedTestCaseProvider`.
2. The dynamic provider creates **five cases per assessment question**. Each
   case identifies `DynamicRunner` and passes the question ID and a derived
   seed.
3. `CombinedTestCaseProvider` combines the fixed and dynamic definitions and
   redistributes each question's existing maximum over all of that question's
   cases. The five question maxima remain **3, 3, 5, 5 and 4**, or **20 total**.
   Adding cases therefore changes how evidence is sampled, not the assessment
   denominator.
4. `StudentCompiler` compiles the student's source and the default-package
   `DynamicRunner.java` resource into that student's isolated class directory.
   The runner must be present there because the student subprocess classpath
   does not include the autograder's application classes.
5. `CaseExecutor` launches `DynamicRunner` in a child Java process for each
   case. The runner generates 25 trials from the case seed and checks the
   question-specific invariant. It writes a bounded result status for the
   case.
6. The normal pipeline records pass/fail against the case's allocated points
   and writes its usual grading diagnostics and reports.

The assessment therefore has 20 fixed and 25 dynamic cases overall: **45 per
submission**. The five dynamic cases for a question each represent a sample of
25 randomized trials; those 25 trials decide the single dynamic case result.

### What each dynamic invariant checks

| Question | Generated behavior being checked |
| --- | --- |
| Q1a | Return exactly the input words that are isograms, preserving duplicates and order; letter matching is case-insensitive and non-letters do not count. |
| Q1b | Sum only even `Integer` values in a mixed `ArrayList<Object>`; ignore booleans, doubles and strings. |
| Q2a | Average ages for a surname at either end of the person's name, case-insensitively. Candidate surnames are selected from the supplied fixture outside the fixed adapter's known surnames. |
| Q2b | Return the highest-GPA student for a course. Candidate courses are selected from the fixture outside the fixed adapter's known courses. |
| Q3 | Keep every shape with area at most 1000 and sort by descending area, then descending perimeter. Generated trials include an over-limit shape and equal-area shapes to exercise both rules. |

The runner stops the current question's trial set at its first invariant
failure and marks that dynamic case failed. An unexpected exception is recorded
as an error. This gives a compact case-level result while keeping the marks
compatible with the existing pipeline.

### Why the regular pipeline is wired this way

`TestCaseProvider` is the shared boundary for supplying case definitions.
`DynamicTestCaseProvider` can add definitions without changing the fixed
provider. `CombinedTestCaseProvider` composes the two and owns the reweighting
step. `GradingPipeline` then treats fixed and dynamic definitions uniformly.
That separation keeps the generation policy, scoring policy and execution
policy in different places.

`DynamicRunner` is a standalone source resource rather than an ordinary
application class. It is copied and compiled with the submission. It uses
reflection to call the question's public method and checks generated behavior
inside the student process. This placement solves a practical classpath issue:
the child process sees the student's compiled classes and support files, not
the host application's private implementation classes.

## 3. The Question-object randomized mode

### What a `Question` object contains

`autograder.autotester.Question` is an abstract definition of one programming
question. A subclass declares:

- The class, method, return type and parameter names/types expected from a
  submission.
- A human-readable description and whether the student method is static.
- One or more named test cases, with one input generator per parameter.
- Optional fixtures, support classes/sources, starter sources and companion
  source names needed to compile or run the answer.
- `answerKey(arguments)`, the trusted reference implementation used to compute
  expected outputs for generated trials.

For example, a small question definition conceptually reads like this:

```java
import java.util.List;

public final class Q1 extends Question {
    public Q1() {
        signature("Q1", "isEven", boolean.class);
        describe("Return true when the integer is even.");
        param("n", int.class);
        addTestCase("small", new RandomInt(-100, 100));
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        return ((Integer) arguments.get(0)) % 2 == 0;
    }
}
```

`signature` and `param` describe the Java API to compile and invoke. `addTestCase`
connects a test case ID to input generators. The `answerKey` implementation
produces the expected result for each generated argument list. The snippet is
illustrative; the live Q1 definition has several cases.

The base class validates identifiers, supported Java types, matching numbers
of generators and parameters, duplicate names/IDs, artifact path boundaries,
and the presence of a signature and at least one test case. It exposes copied
lists for its metadata instead of allowing callers to replace its internal
collections.

### Question registration and current lab definitions

`QuestionRegistry` constructs, validates and registers the definitions under
their class IDs. The current registry includes the small API examples Q1
(is-even) and Q2 (array sum), plus lab questions Q1a, Q1b, Q2a, Q2b and Q3.
Adding a question involves defining a subclass, registering it, and ensuring
its referenced resource artifacts exist.

| ID | Expected student method | Reference behavior |
| --- | --- | --- |
| Q1 | `boolean isEven(int)` | True when the integer is even. |
| Q2 | `int sum(int[])` | Sum every array value. |
| Q1a | `ArrayList<String> getIsogramWords(ArrayList<String>)` | Keep isogram words in original order, retaining duplicates; ignore case and non-letters when deciding whether a word repeats a letter. |
| Q1b | `int getSumOfEvenIntegers(ArrayList<Object>)` | Sum even `Integer` elements only. |
| Q2a | `double getAverageAge(String, String)` | Average matching first or last name tokens; return 0 if there are no matches and -1 for an unavailable file. |
| Q2b | `String getTopStudent(String, String)` | Return `Name-GPA` for the highest-GPA student in a course; throw `DataException` when no matching course record exists. |
| Q3 | `List<Shape> sortShapes(List<Shape>)` | Keep shapes with area at most 1000; sort by descending area, then descending perimeter. |

The lab definitions can declare fixture files, support sources and companion
sources. For instance, Q2a/Q2b use their text fixtures; Q2b also includes
`DataException.java`; Q3 includes the shape support types and the required
`ShapeComparator.java` companion.

### How one randomized grading run works

1. The CLI resolves the question ID through `QuestionRegistry` and loads
   `RandomizedConfig` from the provided properties file. Relative paths are
   based on that properties file.
2. `RandomizedGrader` validates the definition and submission folder, checks
   the named student source and required companions, and rejects overlapping
   source/results directories.
3. It creates one `Random(seed)` and walks test cases in declaration order.
   For every configured run of each case, it generates and validates arguments,
   copies the arguments for the answer key, calculates and validates the
   expected result, and stores a trial. A copied argument list prevents a
   mutating answer key from changing the student's input/reference data.
4. The grader creates a temporary workspace, copies the student's source,
   companions, declared fixtures and support code, and compiles with
   `javac --release 17`. Compilation runs as an external process.
5. It serializes the generated inputs to a protocol file and launches the
   packaged `RandomizedStudentRunner` in a separate Java process. The child
   resolves the public method by reflection, creates an object for instance
   methods, invokes it for each trial, and writes results to a second protocol
   file. Q3 shape descriptors are reconstructed as `Shape` objects in the
   child and returned shape results are normalized to text for comparison.
6. The host grader compares normal results using deep equality and recognizes
   declared expected exceptions. It creates a `RandomizedResult` with pass
   counts, seed, and a capped list of failure details, writes the report, and
   removes the temporary workspace.

Keeping the reference calculation in the host process and invocation in a
separate JVM has two useful properties: the answer key is not packaged with the
student code, and ordinary student exceptions or `System.exit` do not directly
terminate the main grader process.

### Single-submission, resource and batch commands

After building the application, the three command forms are:

```text
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-resources <question-id> config.properties
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade <question-id> <submission-folder> config.properties [seed]
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade-batch <question-id> <zip-folder> config.properties [seed]
```

The resource command writes a student stub, a tester containing seeded fixed
examples, and any declared support artifacts to
`randomized.resources.directory` (default `students/resources`). The tester's
examples are convenient practice checks. Grading uses additional generated
inputs rather than relying only on those displayed examples.

The single-submission command accepts a folder containing the question's
`.java` file and any required companion source. If no seed is supplied, the
application chooses one; the result records it so the run can be reproduced.
The result file is written under `randomized.results.directory` (default
`grading/results`) and begins with PASS or FAIL, followed by pass count, seed,
and up to the configured number of failure examples.

The ZIP batch command selects top-level archives matching the project's
`archive.file.regex`, processes them in filename order, extracts each with the
existing bounded ZIP extractor, requires exactly one source with the selected
question's filename, stages companions, and grades each archive with the same
seed. Each archive receives a text report and the run writes `summary.csv`.
An archive-specific problem becomes a failed row and does not prevent later
archives from being attempted. Run the command once per question when grading
the complete lab.

### Configuration and guardrails

The optional `randomized.*` properties are:

| Property | Default | Purpose |
| --- | --- | --- |
| `randomized.resources.directory` | `students/resources` | Generated stub/tester destination |
| `randomized.results.directory` | `grading/results` | Reports and temporary workspace parent |
| `randomized.java.command` | `java` | Child JVM command |
| `randomized.javac.command` | `javac` | Student compiler command |
| `randomized.runs.per.case` | 50 | Generated trials for each declared case |
| `randomized.timeout.seconds` | 10 | Deadline for the complete child run |
| `randomized.max.failures.shown` | 10 | Failure examples retained in report |
| `randomized.examples.per.case` | 2 | Distinct examples in generated tester |
| `randomized.max.output.bytes` | 1 MiB | Captured process output limit |

Values are validated. A run is limited to 10,000 trials and an estimated 16
MiB of input. Compilation has a separate deadline of at least 60 seconds.
Protocol/result deserialization has size, depth, reference-count and allowed
type filters. Student input arrays/lists are copied for the trusted answer key.

These are robustness controls, **not a security sandbox**. The child Java
process has the grading user's file, network and process permissions. A
malicious submission can access those permissions or start other processes.
Use trusted course submissions or add OS/container restrictions outside this
application.

## 4. How the two random systems differ

| Detail | Feature 4 dynamic case | Question-object randomized mode |
| --- | --- | --- |
| Expected-result strategy | Check a question invariant; no independent answer key | Call the trusted `Question.answerKey` in the host process |
| Unit of result | One pass/fail result for a dynamic case after up to 25 internal trials | One result per generated trial, summarized for the full run |
| Score behavior | Case contributes part of the existing question maximum | Independent PASS/FAIL report only |
| Main runner | `DynamicRunner.java` compiled into the student's case directory | `RandomizedStudentRunner.java` launched from a temporary workspace |
| Intended use | Additional scored evidence in the regular assessment | Practice resource generation and seeded per-question or ZIP-batch testing |

Feature 4 avoids writing a second oracle because it checks invariants from the
question specification. The Question-object mode is more general: each
question can supply its own reference implementation and typed generators,
but that means its answer key must be correct and maintained by the instructor.
These are complementary approaches, not interchangeable implementations.

## 5. Object-oriented design you can explain

- **Abstraction:** `Question` describes the common question contract; callers
  use the metadata and `answerKey` behavior without needing a specific Q1/Q2
  subclass.
- **Polymorphism:** `QuestionRegistry` holds different question subclasses
  through the `Question` type. The `Generator<T>` functional interface also
  allows a built-in generator or a question-specific lambda/method reference to
  provide values through the same `next(Random)` operation.
- **Encapsulation:** `Question` owns the signature, parameters, cases and
  artifacts; it validates changes through protected declaration methods and
  returns copied collections. `RandomizedConfig` validates its values when
  constructed.
- **Composition / single-purpose services:** `RandomizedGrader` uses a
  `Question`, config, process runner and trial protocol. Resource generation,
  ZIP batching and process serialization live in separate classes instead of
  being folded into one question class.
- **Inheritance:** each concrete question reuses the common metadata and
  validation behavior from `Question`, while implementing its own reference
  behavior. The design uses inheritance because the question definitions share
  a real contract; generators themselves use interface composition rather than
  extra class inheritance.

For Feature 4, the parallel design story is the `TestCaseProvider` contract,
two provider implementations, a composing/reweighting provider, and an
execution runner separated from the pipeline. `GradingPipeline` orchestrates
grading; it does not contain every generated-input algorithm.

## 6. Verification evidence and current status

The repository records Feature 4 as **REVIEW**, with William as its locked
owner. A clean Maven build and William's human clean-build confirmation remain
pending in the latest Feature 4 handoff. Do not describe it as DONE until that
completion gate is met.

Recorded Feature 4 checks include focused runner/pipeline/score-writer checks,
Java 17-target compilation, and a six-submission sample run. The latest
recorded run produced 45 case records per submission, reports and marks of
Chee 11.50, David 10.50, Fen 9.88, Jing 0.00, Ping 20.00 and Xing 0.50. The
score sheet rounds to two decimals; case and submission diagnostics retain
full precision. The handoff records that all supplied `Project/` hashes
matched that run's pre-run snapshot. Maven was unavailable for that run, so it
does not replace the pending clean Maven verification/human confirmation.

The randomized-mode tests in the repository cover deterministic replay with a
fixed seed, a memorized/wrong answer, student mutation of array input,
compilation errors, non-terminating code, thrown exceptions, missing methods,
failure caps and result contents. `DynamicRunnerTest` covers Q2a surname
position and Q3 filtering/tie-breaking over seeds 7, 42 and 99. These tests are
source evidence; this guide-writing task did not run the test suite.

Useful commands when preparing the next verification:

```text
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

## 7. Brief and submission context

The project brief's page 5 explains why new test inputs matter: fixed examples
can encourage hard-coded answers and non-terminating programs can block manual
grading. Page 6 asks for separate responsibilities such as validation,
compilation, execution, grading and reporting. The highest code band on page 6
rewards clear modularization, focused methods, and code aligned with known
design principles. Pages 6-7 also emphasize robust handling of complex cases,
clear statuses and a usable workflow. The two systems described here address
those goals through generated evidence, process deadlines, case isolation,
separate services and reproducible results; they do not guarantee a grade.

The brief also requires each team member to understand and explain the team's
deliverables during the presentation/interview (pages 2-3). Review the actual
source and run the commands yourself before presenting. The project uses Maven
and its own Java sources; the brief permits Maven's standard directory layout
and allows only free/open-source external libraries. The team's release
checklist still assigns required AI-use documentation to the human team.

## Source map

- Feature 4: `src/main/java/autograder/grading/DynamicTestCaseProvider.java`,
  `CombinedTestCaseProvider.java`, `GradingPipeline.java`,
  `src/main/resources/autograder/adapters/DynamicRunner.java`, and
  `src/main/java/autograder/execution/StudentCompiler.java`.
- Question definitions and runner: `src/main/java/autograder/autotester/`.
- Child process entry point: `src/main/resources/autograder/autotester/RandomizedStudentRunner.java`.
- CLI and user guide: `src/main/java/autograder/App.java`,
  `RANDOMIZED_AUTOGRADER.md`, and `docs/INSTRUCTOR_USER_GUIDE.md`.
- Feature 4 status/evidence: `DYNAMIC_TEST_FEATURE_SUMMARY.md` and
  `AGENTS.md`'s current context section.
- Project expectations: `CS102-Project-Auto Grading System.pdf`, especially
  pages 2-3 and 5-7.
