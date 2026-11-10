# Parallel grading architecture

Status: DONE. Additional feature (not one of the six numbered bonuses; improvement
idea 6). Owner: Boey. Merged into main through PR #18 (scheduler, 2026-10-27) and
PR #20 (worker-count settings, `parallel-workers-settings`, 2026-10-27). Boey confirmed
the clean build of main with PR #20 and checked the GUI dropdown on 2026-10-27; no
teammate review is recorded on the PRs.

## Purpose and flow

Grading six or more submissions one after another is slow because each case starts
a child process. Parallel grading lets several submissions be graded at once without
changing how any single submission is graded. The default stays one worker, which is
the previous sequential behaviour.

1. The worker count comes from `execution.workers` (1 to 32, or `auto`), the
   command-line `--workers` flag, or the GUI **Workers** dropdown.
2. A count above the machine's logical processors is capped, with a warning.
3. `GradingPipeline.grade()` hands the submission indexes to `SubmissionScheduler`,
   which runs them on a fixed thread pool and returns results in submission order.
4. Withheld decisions and grades are merged in that order, so reports are written as before.

```
execution.workers ┐
--workers flag    ├→ RunSettings.withWorkers → WorkerLimits.cap → GradingPipeline.grade()
GUI dropdown      ┘                                                  └→ SubmissionScheduler
```

## Components and responsibilities

| Class | Responsibility |
| --- | --- |
| `grading.SubmissionScheduler` (`src/main/java/autograder/grading/SubmissionScheduler.java:14`) | Package-private. Runs `IndexedTask`s sequentially when `workers <= 1` or one submission (line 29), otherwise on a daemon thread pool of `min(workers, total)` threads (lines 41, 101). Collects results in index order and rethrows the lowest-index failure (lines 58-87). On exit it cancels and shuts down the pool and waits up to 10 s (lines 49-55, 89) |
| `grading.GradingPipeline` (`GradingPipeline.java:85`) | The single scheduler call in `grade()`. Per-submission state is local (`local` withheld map), and results are merged afterwards (lines 94-97). Progress counts are updated under a lock so they never go backwards (`notifyCount`, line 109) |
| `config.ExecutionConfig` (`config/ExecutionConfig.java:14`) | `workers` record component. Absent or blank gives 1, `auto` gives `WorkerLimits.autoWorkers()`, otherwise a positive whole number up to `MAX_WORKERS = 32`; larger values are rejected (lines 87-99). `withWorkers(int)` returns a copy with the same checks (line 76) |
| `config.WorkerLimits` (`config/WorkerLimits.java:9`) | Probes logical processors through an injectable `IntSupplier` (`system()`, line 19). `autoWorkers()` is `min(4, processors)` (line 28). `cap(requested)` returns a `Capped` record with the capped count and an optional warning (lines 32-43) |
| `config.RunSettings.withWorkers` (`config/RunSettings.java:32`) | Copy of the validated settings with a different worker count; nothing is written back to the properties file |
| `WorkersOption` (`WorkersOption.java:9`) | Parses the `--workers` value: `auto` or a whole number from 1 to 32; anything else throws with a usage-style message (lines 16-31) |
| `App` (`App.java:49-100`) | `--workers <N|auto> <config>` requires exactly three arguments, otherwise prints usage and returns 1 (lines 59-73). `capWorkers` prints the cap warning to stderr and applies the capped count (lines 93-100). The one-argument form is unchanged |
| `gui.WorkerChoice` (`gui/WorkerChoice.java:12`) | Options are `1..processors` (line 21). The selection follows the configured value (capped) only when that value changed since the previous load (lines 26-33) |
| `gui.RunBar`, `gui.MainWindow` | `RunBar` holds the `JComboBox` (`gui/RunBar.java:22`). `MainWindow.showWorkers` preselects it (line 129) and `onFinalCheck` applies the chosen count to the run's settings (line 155). `GuiPreferences` stores only the four paths, so the choice is never saved |

## Behaviour preserved

- Default is one worker. With `execution.workers` absent, the scheduler runs the same loop sequentially. `config.properties` has no workers key.
- Per-submission grading (`gradeSubmission`) is not modified; only the loop that calls it was replaced.
- Results are returned and merged in submission order, so CSV row order does not depend on which worker finishes first.
- A before/after comparison of the six-student sample with workers absent gave identical stdout, stderr and run files; at 1, 2 and 4 workers outputs matched sequential apart from `DurationMillis` and timings. `scorereport.csv` was byte-identical to main.
- Sample scores are unchanged: 20.00, 11.50, 10.50, 9.88 (9.875 exact), 0.00, 0.50.
- Known difference: with more than one worker, console progress lines appear in completion order, so their order varies between runs.

## Failure, cap and stop behaviour

- A failure in one task cancels the rest of the batch; the lowest failing index is rethrown, matching what the sequential loop would have reported (`SubmissionScheduler.java:58`).
- An interrupt (GUI **Stop**) is rethrown as `InterruptedException`; the pool is shut down and worker threads are daemon threads, so they cannot keep the JVM alive.
- `--workers` with a bad value prints the problem and usage and exits 1. `--workers 64` exits 1. A value in the 1 to 32 range above the processor count is capped with a warning on stderr (for example 16 became 10 on the 10-core test machine).
- Zero, negative, non-numeric or above-32 values in `execution.workers` are rejected when the configuration loads.

## Actual OOP principles used

- **Single responsibility:** scheduling (`SubmissionScheduler`), machine limits (`WorkerLimits`), value validation (`ExecutionConfig`), flag parsing (`WorkersOption`) and GUI selection rules (`WorkerChoice`) are separate classes.
- **Encapsulation:** the scheduler and `WorkersOption` are package-private; `WorkerLimits.Capped` and `ExecutionConfig` are immutable records.
- **Dependency injection for testability:** `WorkerLimits` takes an `IntSupplier`, so tests use a fixed processor count instead of the real machine.
- **Functional interface as a strategy:** `SubmissionScheduler.IndexedTask` (line 17) lets the scheduler stay independent of what a task grades.
- **Open/closed:** existing `ExecutionConfig.load` overloads and the one-argument CLI form were kept; `withWorkers` and the flag were added beside them.

## Verification and acceptance

- `mvn clean verify`: 222 tests, 0 failures, errors or skips, on macOS (JDK 25, Java 17 target) and in the Windows JDK 17 CI.
- New tests include `src/test/java/autograder/grading/SubmissionSchedulerTest.java`.
- Sample wall-clock on a 10-core machine: 43 s sequential, about 41 s at 2 workers, about 35 s at 4 workers. The run is limited by the slowest student, so the speed-up is modest on a small batch.
- Not verified: very large batches; speed-up on machines with fewer cores.

## Slide Deck Talking Points

- Grade several submissions at once; the default stays sequential, so nothing changes unless asked.
- One scheduler call replaced the loop; the grading code for a single student is untouched.
- Results merge in submission order, so reports match the sequential run.
- Worker counts follow the machine: `auto` picks up to 4, and larger requests are capped with a warning.
- The same setting works from config, the `--workers` flag and a GUI dropdown that is never saved.
- Measured on the sample: 43 s down to 35 s at 4 workers, bounded by the slowest student.
- Outputs are identical apart from timings; only the console line order varies.
