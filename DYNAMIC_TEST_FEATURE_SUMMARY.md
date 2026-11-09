# Feature #4 — Dynamic Parameterized Test Ingestion

**Owner:** William (locked) · **Status:** REVIEW (was TODO; earlier commit was broken and has been fixed)

## Why this design (the instructor-oracle problem)

The project constraint is that **only the `Project/` directory is supplied to the team**. That means the
team cannot write answer-key adapters / oracles for the instructor's questions — doing so would force the
instructor to also hand-code those, which violates the "only `Project/` is provided" rule.

The original committed approach compared student output against a pre-computed expected answer
(`DynamicTestRunner` + per-question expected values). That approach is both disallowed (it makes the team
the oracle) and was broken in practice (see *What was broken* below).

**This implementation therefore uses property-based / metamorphic testing.** Instead of asserting an
expected answer, `DynamicRunner` generates random inputs and asserts an *invariant* that the question's
spec guarantees for **every** valid input. A hard-coded ("memorized") solution only matches a few specific
inputs, so it fails the invariant on the randomized trials. No expected answer is ever computed → the team
is never the oracle.

## How it works

1. `DynamicTestCaseProvider` emits **5 dynamic cases per question** (Q1a, Q1b, Q2a, Q2b, Q3). Each case
   carries two args: `[questionId, seed]` and names `DynamicRunner` as the runner. The seed makes every
   run reproducible (no flaky grading).
2. `StudentCompiler` compiles the student's sources **plus** `DynamicRunner.java` (a default-package
   class copied from `src/main/resources/autograder/adapters/`) **plus** the question's support `.class`
   files into one `classesDirectory`. That directory is the *only* thing on the subprocess classpath.
3. `CaseExecutor` runs `java -cp <classesDir> DynamicRunner <questionId> <seed> <resultFile>`.
   `DynamicRunner` reflects the student's `public static` method, generates `TRIALS = 25` random inputs,
   checks the property for each, and writes `PASSED`, `FAILED`, or `ERROR` (capped at `MAX_RESULT_BYTES`).

`CombinedTestCaseProvider` distributes each question's existing maximum across all its fixed and
dynamic cases. The five question maxima remain 3, 3, 5, 5 and 4 points (20 overall); passed dynamic
cases contribute to those marks without allowing a score above the roster denominator.

## Per-question invariants

| Question | Student method (all `public static`) | Property asserted (no expected answer) |
| --- | --- | --- |
| Q1a | `getIsogramWords(ArrayList<String>)` | Result is exactly the isogram (no repeated letter) words of the input |
| Q1b | `getSumOfEvenIntegers(ArrayList<Object>)` | Result equals the sum of the `Integer` values that are even |
| Q2a | `getAverageAge(String file, String surname)` | Mean age of persons whose uppercase surname is the first or last name token; probes only non-fixed surnames so memorized constants are exposed |
| Q2b | `getTopStudent(String file, String course)` | Highest-GPA student for the course; probes only non-fixed courses (`SMU101, CS101, CS102, CS103`) |
| Q3 | `sortShapes(List<Shape>)` | Result contains exactly the input shapes with area at most 1000, sorted by descending area and then descending perimeter |

## What was broken (and is now fixed)

- The old `DynamicTestRunner` required 4 CLI args and called `System.exit(1)` on 3, but the pipeline only
  passed 3 (`input`, `expected`, `resultFile`) → **every dynamic case errored**.
- Q2a / Q2b / Q3 had no runner cases and placeholder expected values (`0.0` / `""`).
- `DynamicTestRunner` lived in `autograder.grading`, which is **never** on the student subprocess
  classpath, so it could not have run even with correct arg counts.

**Changes:** new `DynamicRunner.java` (resources); rewritten `DynamicTestCaseProvider.java`; removed
`DynamicTestRunner.java`; `StudentCompiler.java` now also compiles `DynamicRunner.java` into the student
classes dir; `GradingPipelineTest.java` updated to assert dynamic cases now execute.

## Verification

- `GradingPipelineTest` checks all 45 cases (20 fixed + 25 dynamic) end-to-end and verifies that the
  original question maxima and 20-point assessment maximum are preserved.
- Development harness (`scratch/verify.sh`, now removed from the submission in the repository cleanup; retrievable from Git history at commit `517ad84`) — for **every** question and seeds 7 / 42 / 99:
  - Correct reference implementations → `PASSED`
  - Hard-coded "memorized" implementations → `FAILED`

### Scoring correction verification (2026-10-20)

`CombinedTestCaseProvider` now distributes each question's original maximum across its fixed and
dynamic cases, retaining the five maxima 3, 3, 5, 5 and 4 points. The focused pipeline integration
test passed and asserts a 20-point maximum. All 82 production Java sources and 25 test Java sources
compiled with `javac --release 17`. A packaged six-submission run exited 0, wrote 270 case records
(45 per submission), and each submission's case maxima summed to exactly 20. It produced score,
case, submission, similarity, starter-analysis, HTML and six Markdown feedback reports. The observed
sample scores were Chee 9, David 8, Fen 7.375, Jing 0, Ping 15.277777777777777 and Xing 0.5. All
supplied `Project/` hashes matched the pre-run snapshot. A clean Maven verification was unavailable
because Maven is not installed in this environment; the human clean-build confirmation remains
pending.

### Dynamic invariant and score display correction (2026-10-20)

Q2a dynamic candidates now follow the exam statement: uppercase surnames may be at either end of a
name. Q3 checks the actual `Shape` area/perimeter methods, requires every shape over area 1000 to be
excluded, and verifies both descending sort keys. Its generated inputs always include one excluded
circle and an equal-area pair with reversed perimeter order, so both rules are exercised. The score
sheet numerator is rounded half-up to two decimal places; `cases.csv` and `submissions.csv` retain
the full computed precision.

`DynamicRunnerTest` passed for Q2a and Q3 across seeds 7, 42 and 99. The score-writer and pipeline
regressions passed, and all production sources compiled with `javac --release 17`. The six-submission
run exited 0 at `build/grading/run-15847751552065290169`; Ping's five Q2a and five Q3 dynamic cases
all passed and his exported mark is 20.00. The other exported marks are Chee 11.50, David 10.50,
Fen 9.88, Jing 0.00 and Xing 0.50. All six reports were produced, case/submission diagnostics kept
full precision and all supplied `Project/` files retained their SHA-256 hashes. Maven is unavailable
in this environment, so `mvn clean verify` and William's human clean-build confirmation remain
pending; Feature 4 stays REVIEW.

## Build / run

Maven (preferred):
```
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

The former Maven-free fallbacks (`autograder.grading.ManualTestLauncher` and `scratch/verify.sh`) were
removed from the submission in the repository cleanup; retrievable from Git history at commit `517ad84`. Install Maven and use `mvn clean verify`.

## Next step

Status is **REVIEW**, not DONE, pending a human clean-build confirmation. After confirmation, create
`dynamic_test_architecture.md` per the completion gate (actual OOP principles + slide talking points).
