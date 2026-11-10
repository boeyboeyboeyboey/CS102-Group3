# Instructor GUI architecture

Status: DONE on 2026-10-26 after Boey confirmed the clean build and GUI behaviour.
Additional feature (not one of the six numbered bonuses). Owner: Boey. Branch:
`instructor-gui`.

## Purpose and flow

Instructors asked for "extra features that improve the user experience" and minimal
work to use the autograder. The GUI is a Java Swing desktop window over the
existing grading flow. The command line keeps working exactly as before.

1. `GuiLauncher` finds a configuration file and fills the submissions folder, score
   sheet and output folder from the last run, or else from that configuration.
2. Each field shows its readiness next to it, checked in the background without
   creating or writing anything.
3. **Grade** runs a final check, then grades in the background with live progress.
4. The window shows the outcome, a per-student score table and buttons for the
   output files that were actually written.

```
GuiLauncher → MainWindow ─┬─ PathRow ×4 ──── ReadinessMonitor → ReadinessChecker → RunSettings
                          ├─ RunBar
                          └─ GradingRunWorker → GradingService → RunOutcome → ResultsPanel
App (CLI) ──────────────── RunSettings ─────→ GradingService → RunOutcome → console text
```

## Components and responsibilities

Shared, non-visual layer (used by both CLI and GUI):

| Class | Responsibility |
| --- | --- |
| `config.RunSettings` | One validated configuration: reads the properties file once, applies runtime path overrides, builds all six typed configs through their existing checks |
| `config.RuntimePathOverrides` | Optional replacements for the submissions folder, score sheet and grading output folder |
| `GradingService` | The ordered run: environment check, preparation, grading, then starter, HTML and Markdown reports. Returns a `RunOutcome`; never exits or touches Swing |
| `RunOutcome` | Status (completed, report failures, no submissions, failed, interrupted), failed stage, cause, preparation results, grading run and exit code |
| `RunListener` | Run milestones and progress callbacks |
| `ConsoleRunListener` | The CLI's existing console output, moved unchanged from `App` |
| `GradingPipeline` (new overload) | Optional `ProgressListener` for per-student grading progress; existing constructors are unchanged |

GUI layer (`autograder.gui`):

| Class | Responsibility |
| --- | --- |
| `GuiLauncher` | Entry point: headless check, look and feel, builds the window on the event thread |
| `ConfigLocator`, `StartupValues` | Config discovery order and initial field values |
| `GuiPreferences` | Remembers last-used paths with Java Preferences; failures are ignored |
| `ReadinessChecker`, `Readiness`, `FieldStatus`, `InputField`, `GuiInputs` | Side-effect-free field checks, overlap pre-checks and mapping of config errors to the right field |
| `ReadinessMonitor` | Debounced background checks; stale results are discarded with a generation counter |
| `MainWindow`, `PathRow`, `RunBar` | Layout, state switching, inline status, progress and close handling |
| `GradingRunWorker` | `SwingWorker` that runs `GradingService` and publishes progress to the event thread |
| `ResultsPanel`, `ResultsTableModel`, `RunArtifacts`, `Artifact`, `ArtifactOpener` | Completion view, score table and buttons that open existing outputs |
| `OutcomeText` | All headline and progress wording in one testable place |

## Behaviour preserved

- The CLI still takes one configuration path or `--help`. Its exit codes, console text and order, report order, scoring, withholding and CSV schemas are unchanged.
- A before/after run of the six-student sample produced identical stdout/stderr, `scorereport.csv`, `submissions.csv`, `similarity.csv` and `starter-analysis.csv`. `cases.csv` differed only in timing.
- Known difference: an invalid HTML, feedback or starter setting is now reported before environment validation rather than after it, because all configs load together.
- GUI-selected paths go through the same canonicalisation, protected-path and overlap checks as configured ones. `config.properties` is never rewritten.

## Failure, stop and close behaviour

- Setup problems appear next to the field they concern; the first bad field takes focus.
- Student-level problems (compile errors, timeouts, identity warnings) stay in the table, dashboard and feedback. They never raise a dialog.
- Run-level failures show the failed stage and cause in the headline.
- **Stop** interrupts the worker thread. Running test processes and their descendants are terminated by the existing `ProcessRunner`, and no scores are written. The partial run folder is left in place.
- Closing during a run asks "Grading is running. Stop and quit?" and exits only after the run has stopped.
- On a headless machine, the launcher prints the command-line alternative and exits with status 1.

## Actual OOP principles used

- **Single responsibility:** checking, monitoring, running, displaying and opening are separate classes. `OutcomeText` owns the wording.
- **Dependency inversion and the observer pattern:** `GradingService` reports through the `RunListener` interface. Console and Swing listeners plug in without the service knowing about either.
- **Encapsulation and immutability:** `RunSettings`, `RuntimePathOverrides`, `RunOutcome`, `GuiInputs` and `FieldStatus` are immutable records. Implementation classes are package-private.
- **Open/closed:** existing `load(Path)` methods and `GradingPipeline` constructors were kept, and new overloads were added beside them.
- **Composition:** `MainWindow` composes rows, a run bar and a results panel instead of subclassing a large frame.

## Verification and acceptance

- `mvn clean verify`: 157 tests, 0 failures, errors or skips (JDK 25, Java 17 target, Maven 3.9.9, macOS).
- New tests: `RunSettingsTest`, `GradingServiceTest`, the new `AppTest` case and `autograder.gui.*Test`.
- A scripted drive of the real window covered startup readiness, an invalid path, a full sample run with the same scores as the CLI and all eight output buttons, and a stopped run. No exceptions occurred and `Project/` input hashes were unchanged.
- Boey built it and confirmed the GUI works on 2026-10-26.
- Not verified: Windows, a Java 17 runtime, dark-mode contrast, or the output buttons opening files on another OS.

## Slide Deck Talking Points

- One grading service shared by the CLI and GUI, so the grading rules exist once.
- Inline readiness meets Nielsen's error-prevention heuristic: problems show beside the field before grading starts.
- Background grading with live progress keeps the window responsive and shows system status (brief §4.3).
- One bad submission never stops the batch; student issues stay in the results while run-level failures name their stage (brief §4.2).
- Stop and safe close reuse the existing process cleanup, giving user control without leaving test processes running.
- Remembered paths and config-based defaults mean an instructor can usually just press **Grade**.
