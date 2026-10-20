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

## Per-question invariants

| Question | Student method (all `public static`) | Property asserted (no expected answer) |
| --- | --- | --- |
| Q1a | `getIsogramWords(ArrayList<String>)` | Result is exactly the isogram (no repeated letter) words of the input |
| Q1b | `getSumOfEvenIntegers(ArrayList<Object>)` | Result equals the sum of the `Integer` values that are even |
| Q2a | `getAverageAge(String file, String surname)` | Mean age of persons whose surname matches; probes only non-fixed surnames (`TAY, LEE, WONG, ONG`) so memorized constants are exposed |
| Q2b | `getTopStudent(String file, String course)` | Highest-GPA student for the course; probes only non-fixed courses (`SMU101, CS101, CS102, CS103`) |
| Q3 | `sortShapes(List<Shape>)` | Result is a permutation of the inputs **and** sorted by descending area (parsed from each shape's `id=>AREA:PERIMETER` `toString()`) |

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

- `GradingPipelineTest` — both methods **PASS**. The full pipeline compiles and runs all 45 cases
  (20 fixed + 25 dynamic) end-to-end, including Q3 with the reference support classes, without breaking
  existing behavior.
- Standalone harness (`scratch/verify.sh`) — for **every** question and seeds 7 / 42 / 99:
  - Correct reference implementations → `PASSED`
  - Hard-coded "memorized" implementations → `FAILED`

## Build / run

Maven (preferred):
```
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

Fallback (Maven not installed): compile main + test sources with JDK 17+ and run
`autograder.grading.ManualTestLauncher`, or use `scratch/verify.sh` to exercise `DynamicRunner`
directly against correct / hard-coded implementations.

## Next step

Status is **REVIEW**, not DONE, pending a human clean-build confirmation. After confirmation, create
`dynamic_test_architecture.md` per the completion gate (actual OOP principles + slide talking points).
