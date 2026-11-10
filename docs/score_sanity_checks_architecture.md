# Score sanity checks architecture

Status: DONE. Additional feature (not one of the six numbered bonuses; improvement
idea 9). Owner: Boey. Branch: `score-sanity-checks`, merged into main through PR #19
on 2026-10-27 without a teammate review. Boey confirmed the clean build of main (which
includes it) on 2026-10-27.

## Purpose and flow

An instructor's tester or expected output can be wrong, and a wrong assessment makes
many students look bad at once. Sanity checks look across the whole graded batch for
patterns that point at the assessment rather than the students, and report them as
advisory findings. They never change a score or withhold a result.

1. After grading and the starter-code analysis, `SanityAnalysis.analyze` runs `SanityChecker` on the run.
2. Findings are written to `sanity-checks.csv` in the run folder and summarised on the console.
3. HTML and feedback reports are then generated as usual.

```
GradingRun → StarterAnalysis → SanityAnalysis → SanityChecker → SanityFinding ─┬→ SanityCsvWriter → sanity-checks.csv
                                  │                                            └→ console summary
                                  └→ HTML report → Markdown feedback
```

## Components and responsibilities

| Class | Responsibility |
| --- | --- |
| `analysis.sanity.SanityChecker` (`src/main/java/autograder/analysis/sanity/SanityChecker.java:15`) | Pure analysis over a `GradingRun`: drops withheld submissions, returns nothing when fewer than `minGraded` remain (lines 20-29), then checks every question |
| `analysis.sanity.SanityCheck` (`SanityCheck.java:3`) | Enum of five checks: `ALL_COMPILE_ERRORS`, `QUESTION_MOSTLY_ZERO`, `SHARED_FAILURE`, `CLUSTERED_TIMEOUTS`, `FIXED_DYNAMIC_GAP` |
| `analysis.sanity.SanityFinding` (`SanityFinding.java:5`) | Immutable finding: check, question, case, affected count, graded count, message |
| `analysis.sanity.SanityCsvWriter` (`SanityCsvWriter.java:7`) | Formats findings with the project's `CsvCodec`; header `Check,Question,Case,Affected,Graded,Message` (line 13) |
| `config.SanityConfig` (`config/SanityConfig.java:9`) | Record of the settings; validates them and loads the optional `grading.sanity.*` keys. `defaults()` and `disabled()` factories (lines 19-25) |
| `SanityAnalysis` (`SanityAnalysis.java:14`) | Package-private. Runs the checker, writes `sanity-checks.csv` with `CREATE_NEW`, prints the summary and returns a `Result` with the exit code |
| `App.generateReports` (`App.java:197-205`) | Fixes the order and combines exit codes with `Math.max` |
| `config.RunSettings` (`config/RunSettings.java:19, 59`) | Carries `SanityConfig`, loaded with the other typed configs |
| `gui.RunArtifacts` (`gui/RunArtifacts.java:17, 34`) | Adds a "Sanity checks" artifact button for the file when it exists |

## What each check does

Thresholds are the defaults from `SanityConfig.defaults()` (line 20). A finding needs the affected share to reach the percentage (`meets`, `SanityChecker.java:171`).

- `ALL_COMPILE_ERRORS`: every case of the question has a compile error for every graded submission (line 145). When it fires, the mostly-zero check is skipped for that question.
- `QUESTION_MOSTLY_ZERO`: the share of graded submissions with a question score of exactly zero reaches `zeroPercent` (90).
- `SHARED_FAILURE`: for one case, the largest group of `FAILED` or `RUNTIME_ERROR` results with identical status, standard output and standard error reaches `sameFailurePercent` (80). Silent failures are not grouped, because a hard case would otherwise always be flagged (lines 71-76).
- `CLUSTERED_TIMEOUTS`: timeouts in one case reach `timeoutPercent` (50).
- `FIXED_DYNAMIC_GAP`: pass rate of fixed cases exceeds the pass rate of dynamic cases (ids containing `-dynamic-`) by at least `fixedDynamicGapPoints` (40), ignoring compile errors (lines 97-130).

## Configuration

Optional keys, all with defaults when absent (`SanityConfig.java:36-45`); `config.properties` is unchanged.

| Key | Default | Range |
| --- | --- | --- |
| `grading.sanity.enabled` | true | true or false |
| `grading.sanity.min.graded` | 5 | at least 1 |
| `grading.sanity.zero.percent` | 90 | 1 to 100 |
| `grading.sanity.same.failure.percent` | 80 | 1 to 100 |
| `grading.sanity.timeout.percent` | 50 | 1 to 100 |
| `grading.sanity.fixed.dynamic.gap.points` | 40 | 1 to 100 |

`sanity-checks.csv` is also a reserved report name, so `grading.report.file` cannot use it (`config/ExecutionConfig.java:18-20, 103`).

## Behaviour preserved

- Advisory only: scores, withholding and the other CSVs are not affected. Withheld submissions are excluded from the analysis.
- Fewer than 5 graded submissions produces no findings.
- Sample run: exit 0, unchanged scores (20.00, 11.50, 10.50, 9.88 with 9.875 exact, 0.00, 0.50), console line "Sanity checks: none" and a header-only CSV. `Project/` input hashes unchanged.
- The older `generateReports` overload passes `SanityConfig.disabled()` (`App.java:193`), so earlier callers get no sanity step.

## Failure behaviour

- If the checker throws, an error is printed and the result is exit 1 with no CSV and no "none" line, so a crash never reads as a clean pass (`SanityAnalysis.java:30-34`).
- If the CSV cannot be written, the findings are still printed with "not saved" and the exit code is 1 (lines 36-37, 45-47).
- A sanity failure makes the run return nonzero, but the HTML and feedback reports still run (`App.java:201-204`).
- Control characters in student output are removed from console excerpts (`SanityChecker.java:184-186`).

## Actual OOP principles used

- **Single responsibility:** detection (`SanityChecker`), data (`SanityFinding`), serialisation (`SanityCsvWriter`), settings (`SanityConfig`) and orchestration (`SanityAnalysis`) are separate.
- **Encapsulation and immutability:** `SanityFinding` has private final fields and no setters; `SanityConfig` is a record that validates in its compact constructor (line 11); `SanityAnalysis` is package-private with a private constructor.
- **Enum for a closed set of checks:** `SanityCheck` names every finding type, so the CSV and console share one vocabulary.
- **Open/closed:** `RunSettings` kept its earlier constructor (line 25) and `generateReports` kept its earlier overload.
- **Fail-safe defaults:** missing keys use defaults and `disabled()` offers an explicit off switch.

## Verification and acceptance

- `mvn clean verify` after the final fix on the feature branch: 171 tests, 0 failures, errors or skips (macOS, JDK 25, Java 17 target); the repository later reached 222 tests green on macOS JDK 25 and Windows JDK 17 CI.
- `SanityCheckerTest` covers the checks; the sample run verified the no-findings path.
- Not verified: findings on real student data. Every finding type is covered by unit tests only.

## Known limits

- No dashboard panel and no GUI headline count; the GUI only offers the artifact button.
- For `FIXED_DYNAMIC_GAP`, the `Affected` and `Graded` columns count cases (dynamic results), not students (lines 124-125).
- `QUESTION_MOSTLY_ZERO` counts only a score of exactly zero, so a question with a case every student passes cannot trigger it.
- Both points above are open review points from PR #19, addressed separately on branch
  `sanity-review-fixes`.

## Slide Deck Talking Points

- Checks the assessment, not the students: patterns across the batch point at a bad tester or expected value.
- Five checks, each with a configurable threshold and sensible defaults.
- Advisory only: scores and withholding never change.
- Results land in `sanity-checks.csv`, on the console and behind a GUI button.
- A crashed check never looks like a clean pass: it returns a nonzero exit code.
- Reports still finish when a check fails, so one problem does not hide the rest.
- Honest limits: unit-tested on synthetic data, not yet seen on real findings.
