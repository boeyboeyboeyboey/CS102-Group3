# Terminal dashboard: feature 6

## Purpose and flow

`App` creates one `TerminalDashboard` and supplies it through `DashboardRunListener` to `GradingService`. `DashboardRunListener` composes the existing `ConsoleRunListener` with the dashboard, preserving ordinary console summaries while forwarding intake progress from `onProgress` and grading progress from `gradingProgress`. `RunListener` extends the existing `ProgressListener` contract. `GradingService` owns the run sequence, passes the listener to `SubmissionProcessor`, adapts grading callbacks from `GradingPipeline`, and reports optional report progress and failures through the listener. The terminal dashboard presents those events; it does not own grading or report generation. Counts reflect completed work; percentages do not estimate time remaining.

`TerminalDashboard` handles presentation only. It prints a bounded, append-only status line with the stage, count, percentage, bar, and current archive. Append-only lines keep warnings visible and make redirected logs readable. It replaces terminal control characters from archive names before printing them. ANSI color is optional; `DashboardSettings` reads `dashboard.mode` from the properties file. `auto` uses color only on an interactive console without `NO_COLOR` or `TERM=dumb`. `plain` forces uncolored output, and `ansi` forces color for a compatible terminal.

`TerminalCancellation` listens for `q` followed by Enter only when Java has an interactive `Console`. It interrupts the grading thread. `GradingService` recognizes interruption during preparation or grading and returns an interrupted `RunOutcome`; intake and grading observe interruption at their existing checkpoints, and the child-process runner already terminates the active child and attempts descendant cleanup when interrupted. The terminal entry point maps cancellation to exit code 130. Cancellation is cooperative: a current file operation or optional report may finish before the next check. A run directory can contain partial files if cancellation occurs during reporting; instructors should use only a completed report.

`GradingService` wraps the supplied listener and isolates runtime display errors across intake, grading, and report callbacks. A broken display cannot change a student's score or stop the batch. The console is not a security sandbox for submitted Java code.

## Design and rubric discussion

- **Abstraction:** `RunListener` extends the existing `ProgressListener` contract, keeping `GradingService` and pipeline work separate from terminal presentation. `DashboardRunListener` composes terminal progress with the existing console summaries.
- **Single responsibility:** settings parsing, progress rendering, and cancellation input live in separate classes under `autograder.ui`.
- **Usability:** counts and stage labels explain what is happening; plain output works in redirected logs; diagnostics and final result paths remain visible.
- **Robustness:** display exceptions are isolated from grading, and cancellation uses the pipeline's existing interruption cleanup.

The dashboard intentionally reports archive-level grading progress rather than guessing case duration or hiding error messages behind a screen redraw. It requires no external runtime library.

## Verification and demo

Run `./mvnw clean verify`, then `java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties`. Demonstrate the default output on the six supplied submissions, set `dashboard.mode=plain` for a redirected-log example, and show `q` plus Enter during an interactive run. Explain that a non-interactive terminal cannot accept the cancellation command. The focused tests check plain/ANSI selection, escaped archive labels, invalid settings, cancellation signaling, listener failure isolation, and grading/report progress events. Kai Cong's clean build and behavior confirmation remain required before DONE.

On 2026-10-25, `mvn verify` passed 137 tests with zero failures, errors, or skips and built the JAR. A packaged six-submission run exited 0, reached reporting 2/2, and wrote the score, case, decision, similarity, starter, HTML, and six Markdown outputs under `build/grading/run-10685875647523570255`. An interactive `q` run printed `grading cancelled` and returned Java exit code 130. `mvn clean verify` stopped in the clean plugin because Windows could not delete `target/test-classes/autograder/ui`; no compilation or test failure was reported in that attempt. The current `GradingService`/`RunListener` integration and its terminal adapter still need focused and clean verification; a clean build on Kai Cong's machine is required for the human completion gate. These earlier results do not verify the integration with current `main`.

Current-main integration verification is recorded in [PR #15 merge resolution](PR15_MERGE_RESOLUTION.md); the older Windows evidence above remains historical.
