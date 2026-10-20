# HOW_THIS_FEAT_4_WORKS.md

**Feature 4 — Dynamic Parameterized Test Ingestion** (owner: William, status: REVIEW)

This document explains, in plain terms, how Feature 4 works end-to-end: what problem it solves, what
the code does at each stage, how it catches "memorized" submissions, and how to run it. It is meant to be
readable without first reading the source.

---

## 1. The problem it solves

The auto-grader already has **fixed** tests: the instructor's `Tester-Files/*Tester.java` define specific
(input, expected) pairs, and the MVP `InstructorTestCaseProvider` mirrors those into the grader's
result-file protocol. Those fixed cases only check the **exact** inputs the instructor wrote.

A student can pass those fixed cases by **memorizing the answers** — hard-coding `if (input == X) return Y`
for each fixed input — without actually implementing the algorithm. Feature 4 adds **dynamic** tests that
generate *novel* inputs at grading time to expose that cheating.

### The "oracle" constraint
The project rule is that **only the `Project/` directory is supplied to the team**. There is no instructor
reference solution for arbitrary inputs, so the team cannot compute a correct expected answer for a random
input. If Feature 4 compared student output against a computed "correct" answer, the team would be acting
as the answer key (an **oracle**) — which is disallowed and unrealistic for a real exam.

**Solution: property-based (metamorphic) testing.** Instead of checking an *expected value*, the runner
asserts an *invariant* that the question's own specification guarantees for **every** valid input. A
memorized solution only matches a handful of fixed inputs, so it violates the invariant on novel random
inputs and fails. The team never computes a correct answer.

---

## 2. End-to-end architecture

```
GradingPipeline
   │  builds the case list from CombinedTestCaseProvider
   │     ├─ InstructorTestCaseProvider  (fixed:  20 cases, from instructor testers)
   │     └─ DynamicTestCaseProvider     (dynamic: 25 cases, 5 per question, seeded)
   │
   ├─ StudentCompiler
   │     compiles, per submission part:
   │        • the student's own sources
   │        • DynamicRunner.java        (default-package; copied from resources)
   │        • the question's support .class files (e.g. Shape/Circle/Rectangle)
   │     → all into one <part>/classes  directory (the ONLY thing on the subprocess classpath)
   │
   └─ CaseExecutor  (runs each dynamic case as a separate child process)
         command: java -cp <part>/classes  DynamicRunner  <questionId> <seed> <resultFile>
         cwd:     <part>/cases/<caseId>/   (also holds the question's copied fixture files, e.g. personstester.txt)
         result token written to resultFile must be PASSED / FAILED / ERROR  (capped at 16 bytes)
```

### File inventory
| File | Role |
| --- | --- |
| `src/main/java/autograder/grading/DynamicTestCaseProvider.java` | Emits 5 seeded dynamic cases per question. Args: `[questionId, seed]`. Runner: `DynamicRunner`. |
| `src/main/resources/autograder/adapters/DynamicRunner.java` | The runner itself (default-package, reflection-based). Compiled into the student's classes dir. |
| `src/main/java/autograder/execution/StudentCompiler.java` | Also copies/compiles `DynamicRunner.java` into the student classes directory. |
| `src/main/java/autograder/grading/CombinedTestCaseProvider.java` | Merges fixed + dynamic cases (already used by `GradingPipeline`). |
| `src/main/java/autograder/execution/CaseExecutor.java` | Runs each case as a child process; reads the `PASSED/FAILED/ERROR` token. |

---

## 3. How a dynamic case is built (`DynamicTestCaseProvider`)

For every question in the assessment (`Q1a, Q1b, Q2a, Q2b, Q3`), the provider emits
`CASES_PER_QUESTION = 5` cases. Each case is:

```
TestCaseDefinition(
    id        = "<questionId>-dynamic-<n>",     // e.g. Q1a-dynamic-1
    questionId= "<questionId>",
    runner    = "DynamicRunner",
    arguments = [ "<questionId>", "<seed>" ],    // seed is deterministic, see below
    points    = question.getPointsPerCase()      // same weight as a fixed case
)
```

The seed is **deterministic and reproducible** (same submission ⇒ same dynamic inputs every run):

```
seed = 0x9E3779B1L  +  questionIndex * 1_000_003L  +  caseNumber * 7_919L
```

So grading is stable: a submission cannot "get lucky" with different random inputs on a re-run.

---

## 4. How it runs (`DynamicRunner`)

`DynamicRunner.main(args)` expects exactly:

```
args[0] = questionId      ("Q1a" | "Q1b" | "Q2a" | "Q2b" | "Q3")
args[1] = seed            (parsed as a long; if it isn't numeric, its hashCode is used)
args[2] = resultFilePath
```

For each case it runs **`TRIALS = 25`** independent random trials:

```java
for (int trial = 0; trial < TRIALS; trial++) {
    String failure = runTrial(questionId, random);   // returns null if the invariant holds
    if (failure != null) { /* record failure, stop */ }
}
status = allTrialsPassed ? "PASSED" : "FAILED";
// any thrown exception anywhere  →  "ERROR"  (mapped to RUNTIME_ERROR by the pipeline)
writeResult(resultFilePath, status);                // one of PASSED / FAILED / ERROR
```

Key mechanics:
- **Reflection, not compile-time dependency.** `DynamicRunner` calls the student's method with
  `Class.forName("Q1a").getMethod("getIsogramWords", ArrayList.class).invoke(null, inputs)`. It is a
  default-package class with no reference to any student type, so it compiles for every question part.
- **Seeded randomness.** A single `Random(seed)` drives all 25 trials, so the inputs are reproducible.
- **No expected answer.** Every trial checks an *invariant* (Section 5), never a computed correct value.
- **Result token.** It writes exactly `PASSED`, `FAILED`, or `ERROR` (≤ 16 bytes). `CaseExecutor`
  maps `PASSED→PASSED`, `FAILED→FAILED`, `ERROR→RUNTIME_ERROR`.

> Q2a / Q2b note: `DynamicRunner` reads the tester data file (`personstester.txt` /
> `studentstester.txt`) **by its fixture filename** from the subprocess working directory. `CaseExecutor`
> copies each question's configured fixtures into that directory, so the file is always present.

---

## 5. The invariants (what "correct" means, per question)

| Q | Student method (all `public static`) | Invariant asserted (no expected answer computed) |
| --- | --- | --- |
| **Q1a** | `getIsogramWords(ArrayList<String>) → ArrayList<String>` | Result contains **exactly** the input words that are isograms (letters only, case-insensitive). Every result word must come from the input; each input word appears in the result iff it is an isogram (count preserved). |
| **Q1b** | `getSumOfEvenIntegers(ArrayList<Object>) → int` | Result equals the sum of the `Integer` elements that are even. `Boolean`, `Double`, and `String` elements are ignored. |
| **Q2a** | `getAverageAge(String file, String surname) → double` | Returned average equals the mean age of persons whose **surname** (last whitespace token of the name, case-insensitive) matches. It only queries surnames the fixed adapter does **not** cover (`TAY, LEE, WONG, ONG` are skipped), so a memorized solution is forced to give a wrong answer. |
| **Q2b** | `getTopStudent(String file, String course) → String` | Returned `"Name-GPA"` must name a real student enrolled in the course with that exact GPA, and no other enrolled student may have a higher GPA. It only queries courses the fixed adapter does **not** cover (`SMU101, CS101, CS102, CS103` are skipped). |
| **Q3** | `sortShapes(List<Shape>) → List<Shape>` | Result is a **permutation** of the inputs (same ids, same multiplicities) **and** sorted in **descending** order of area. Area is parsed from each shape's `toString()` in the form `id=>AREA:PERIMETER`. |

For Q3, `DynamicRunner` builds `Circle`/`Rectangle` objects via reflection
(`Class.forName("Circle").getConstructor(String, double).newInstance(id, radius)`) using the compiled
support classes already in the student classes directory, then feeds them to the student's `sortShapes`.

---

## 6. Why this catches hard-coding (concrete example)

Suppose a student hard-codes Q1a to only recognize the instructor's fixed words:

```java
public static ArrayList<String> getIsogramWords(ArrayList<String> inputs) {
    if (inputs.equals(new ArrayList<>(Arrays.asList("cat","dog","tiger"))))
        return new ArrayList<>(Arrays.asList("cat","dog","tiger"));
    return new ArrayList<>();   // gives up on anything else
}
```

On the instructor's fixed cases this may look fine. But `DynamicRunner` generates **25 random word
lists** (seeded) that are *not* the fixed ones — e.g. `["ben","Elle","aa","deer"]`. The student's code
hits the `return new ArrayList<>()` branch and returns an empty list. The invariant check then finds that
`"ben"`, `"Elle"`, `"aa"` (all isograms) are missing from the result → **FAILED**. The correct
implementation returns the right subset for every random list → **PASSED**.

The same logic applies to all five questions: the fixed cases can be memorized, but the randomized
invariant checks cannot.

---

## 7. How to run it

### A. Grade the sample submissions (production path)
Dynamic cases run automatically — you never invoke `DynamicRunner` by hand.

```bash
mvn clean verify                                                          # compile + run the test suite
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties   # grades the 6 sample archives
```

Each submission now receives **45 cases = 20 fixed + 25 dynamic** (5 per question).

### B. Prove it catches hard-coding (fastest, JDK only, no build)
`scratch/verify.sh` compiles a *correct* and a *hard-coded* reference solution per question and runs
`DynamicRunner` directly:

```bash
bash scratch/verify.sh
```

Expected output: every `correct … -> PASSED` and every `hard … -> FAILED` for seeds 7 / 42 / 99.

### C. Run the pipeline unit test without Maven
This sandbox has no Maven installed, so the suite was verified by compiling with JDK 17+ and running
`autograder.grading.ManualTestLauncher`, which invokes the two `GradingPipelineTest` methods directly.
On a normal machine this is simply `mvn test`.

---

## 8. Verification status

- `GradingPipelineTest` (both methods) **PASS** — the full pipeline compiles and runs all 45 cases
  end-to-end, including Q3 with the reference support classes, without breaking existing behavior.
- `scratch/verify.sh` confirms, for every question and seeds 7/42/99:
  correct → `PASSED`, hard-coded → `FAILED`.
- No supplied `Project/` inputs were modified; the oracle-free design is compatible with
  "only `Project/` is provided".

## 9. Status & next step

Feature 4 is in **REVIEW**, not DONE, pending a human clean-build confirmation (`mvn clean verify`).
After confirmation, create `dynamic_test_architecture.md` (actual OOP principles + slide talking points)
per the completion gate.

### Known limitations / notes
- Q2a/Q2b only probe *non-fixed* surnames/courses. If a real `personstester.txt`/`studentstester.txt`
  contains **no** surname/course outside the fixed sets, that trial reports "no non-fixed X available"
  and the case would be `ERROR` rather than a clean pass — by design, to avoid rewarding a memorized
  constant. Extend `FIXED_Q2A_SURNAMES` / `FIXED_Q2B_COURSES` if the dataset grows.
- `DynamicRunner` lives in `src/main/resources/autograder/adapters/` (not under `src/main/java`) because
  `StudentCompiler` loads it as a resource and re-compiles it into the student's classes directory.
