# Feature 4 Runner and Score Export Fixes

**Owner:** William  
**Work completed:** 2026-10-20  
**Change commit:** `517ad84` (`Fix dynamic runner checks and round exported scores`)  
**Current repository state:** merged to `main` in PR #11 (`4d25864`)

## Summary

I fixed two dynamic-test runner checks that rejected valid student solutions, and made exported score-sheet marks easier to enter in a gradebook. Dynamic cases continue to contribute to marks: each question's existing points are shared across its fixed and dynamic cases, preserving the assessment maximum of 20. Only the exported score-sheet numerator is rounded, using half-up rounding to two decimal places; case-level and submission-level diagnostic values retain their full precision.

## Problems and changes

### Q2a surname handling

The dynamic check assumed the uppercase surname had to be the final name token. The assessment allows the surname at either end, so valid implementations could fail when the surname appeared first. The runner now uses non-fixed uppercase surnames and checks the average for matching surnames at either the first or last token.

### Q3 shape filtering and ordering

The earlier check expected every generated shape in the result and derived comparison values from string output. That rejected correct solutions which exclude shapes with area over 1000. The runner now checks shape area and perimeter directly, requires the result to contain exactly the input shapes with area at most 1000, and verifies descending area followed by descending perimeter. Its generated inputs include an over-limit circle and equal-area rectangles arranged to exercise the perimeter tie-break.

### Score-sheet precision

Dividing question marks among fixed and dynamic cases can produce fractional totals. The score CSV writer now exports marks rounded half-up to two decimal places (for example, `9.875` becomes `9.88`). The case and submission reports preserve the underlying full-precision values for auditing.

## Verification recorded

- Q2a and Q3 runner checks passed for seeds 7, 42, and 99; the generated Q3 cases exercise the area cutoff and both sort keys.
- Focused score-writer and grading-pipeline regression checks passed. The pipeline check confirms the question maxima still total 20.
- Production sources compiled with `javac --release 17`.
- The packaged six-submission run exited successfully at `build/grading/run-15847751552065290169`. It wrote 270 case records (45 per submission); Ping passed all ten Q2a/Q3 dynamic cases.
- Exported marks were Ping 20.00, Chee 11.50, David 10.50, Fen 9.88, Jing 0.00, and Xing 0.50. Six reports were produced: the score, case, submission, similarity, and starter-analysis CSVs plus the HTML dashboard; six Markdown feedback files were also generated.
- SHA-256 checks confirmed the supplied files under `Project/` were unchanged.

The focused JUnit checks were compiled and invoked using the available manual harness. Maven was unavailable in the implementation environment, so this work does **not** claim a successful `mvn clean verify` run. The full Maven verification and William's human clean-build confirmation remain outstanding. Feature 4 remains **REVIEW**, not DONE, until the documented completion gate is met.

## Files involved

The runner changes are in `src/main/resources/autograder/adapters/DynamicRunner.java`; score rounding is in `src/main/java/autograder/report/CsvScoreReportWriter.java`. Regression coverage is in `src/test/java/autograder/grading/DynamicRunnerTest.java`, `src/test/java/autograder/grading/GradingPipelineTest.java`, and `src/test/java/autograder/report/CsvScoreReportWriterTest.java`. The implementation and verification notes were also updated in the feature summary, README, architecture and walkthrough documentation.

## Build and run for review

From the repository root:

```text
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

Review the generated `scorereport.csv` for two-decimal exported marks and `cases.csv` / `submissions.csv` for full-precision diagnostics. David and Xing may still have expected name-header review warnings in the sample run; those are separate from these runner fixes.
