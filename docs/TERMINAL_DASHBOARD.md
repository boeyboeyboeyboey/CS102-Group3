# Terminal dashboard: feature 6

## Purpose and flow

`App` creates one `TerminalDashboard` and passes it through the existing `ProgressListener` interface to submission intake and grading. Intake reports each archive before and after preparation. The grading pipeline reports each submission before and after its cases, then reports CSV writing. `App` reports completion of optional starter analysis, HTML and Markdown output. The existing final score and diagnostic paths still appear after the progress lines. Counts reflect completed work; percentages do not estimate time remaining.

`TerminalDashboard` handles presentation only. It prints a bounded, append-only status line with the stage, count, percentage, bar, and current archive. Append-only lines keep warnings visible and make redirected logs readable. It replaces terminal control characters from archive names before printing them. ANSI color is optional; `DashboardSettings` reads `dashboard.mode` from the properties file. `auto` uses color only on an interactive console without `NO_COLOR` or `TERM=dumb`. `plain` forces uncolored output, and `ansi` forces color for a compatible terminal.

`TerminalCancellation` listens for `q` followed by Enter only when Java has an interactive `Console`. It interrupts the grading thread. Intake checks cancellation between archives; grading checks between submissions, questions, and cases and before CSV export. The child-process runner already terminates the active child and attempts descendant cleanup when interrupted. Cancellation exits with code 130. Cancellation is cooperative: a current file operation or optional report may finish before the next check. A run directory can contain partial files if cancellation occurs during reporting; instructors should use only a completed report.

Both intake and grading catch runtime display errors and continue their work without further progress updates. `App` also isolates display errors around optional reports. A broken display cannot change a student's score or stop the batch. The console is not a security sandbox for submitted Java code.

## Design and rubric discussion

- **Abstraction:** the existing `ProgressListener` contract keeps pipeline work separate from presentation. The previous `ConsoleProgressListener` remains available.
- **Single responsibility:** settings parsing, progress rendering, and cancellation input live in separate classes under `autograder.ui`.
- **Usability:** counts and stage labels explain what is happening; plain output works in redirected logs; diagnostics and final result paths remain visible.
- **Robustness:** display exceptions are isolated from grading, and cancellation uses the pipeline's existing interruption cleanup.

The dashboard intentionally reports archive-level grading progress rather than guessing case duration or hiding error messages behind a screen redraw. It requires no external runtime library.

## Verification and demo

Run `mvn clean verify`, then `java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties`. Demonstrate the default output on the six supplied submissions, set `dashboard.mode=plain` for a redirected-log example, and show `q` plus Enter during an interactive run. Explain that a non-interactive terminal cannot accept the cancellation command. The focused tests check plain/ANSI selection, escaped archive labels, invalid settings, cancellation signaling, listener failure isolation, and grading/report progress events. Kai Cong's clean build and behavior confirmation remain required before DONE.

On 2026-10-25, `mvn verify` passed 137 tests with zero failures, errors, or skips and built the JAR. A packaged six-submission run exited 0, reached reporting 2/2, and wrote the score, case, decision, similarity, starter, HTML, and six Markdown outputs under `build/grading/run-10685875647523570255`. An interactive `q` run printed `grading cancelled` and returned Java exit code 130. `mvn clean verify` stopped in the clean plugin because Windows could not delete `target/test-classes/autograder/ui`; no compilation or test failure was reported in that attempt. A clean build on Kai Cong's machine is still needed for the human completion gate.
