# PR #5 merge resolution record

## Purpose and status

This record documents Boey's approved merge-conflict resolution and replacement PR for the behaviour-preserving modularisation on `code-modularisation`. PR #5 is the existing live draft for that branch. Kiya's feature-1 PR #1 was merged into `main` on 2026-10-15, advancing the base after the modularisation revision. This merge integrates that upstream feature while preserving the modularisation and its public contracts.

The Git agent fetched main at 268f4315e3f3de26d0deaaf82ecba4d5b3a52b9b and began a normal no-commit, no-fast-forward merge. The sole content conflict was src/main/java/autograder/grading/GradingPipeline.java. The conflict resolution and integration are complete; publication remains with the Git agent after main review.

## Scope and ownership

The approved scope is to integrate the merged plagiarism detector with the M1–M5 modularisation and prepare one replacement PR #5. The Git agent owns fetch/merge/index/commit/push and PR publication. The Java agent owns `App.java`, `GradingPipeline.java`, `GradingDiagnosticsWriter.java`, and focused tests/builds. Documentation ownership covers this record and the current project summaries. No optional feature, separate bug fix, dependency or supplied assessment change is included.

The detector contract to preserve is the two-argument `GradingPipeline` constructor, which delegates to a no-op detector, and the three-argument `GradingPipeline` overload, which accepts an injected `PlagiarismDetector`. The integration performs one comparison after grading against the original full submission list and assessment. `IOException` and `RuntimeException` from the optional comparison are reported as `similarity check skipped: ...`, then grading continues with empty findings. The similarity CSV is written unconditionally after submissions and before score export, including a header-only file when there are no findings. Its exact header is `FirstArchive,FirstUsername,SecondArchive,SecondUsername,Similarity,Evidence`; similarity uses `Locale.ROOT` with three decimals. Existing filename, path-matched username and newline-separated evidence fields are retained. The diagnostic-writer boundary remains responsible for completed CSV projections, while the pipeline retains authoritative export eligibility and withholding decisions.

The `App` integration must retain one configured grading/report flow. Upstream currently has a duplicated grading-local declaration and an early return that leaves HTML and Markdown generation unreachable; the integration restores a compiling, reachable flow that runs the optional reports on the completed batch without changing grading or CSV authority.

## Preservation criteria

Review the merged result against the existing modularisation evidence and contracts:

- Keep public signatures and the package-private composition boundaries described in [CODE_REVIEW.md](CODE_REVIEW.md).
- Preserve scoring, identity decisions, submission ordering, withholding, and the byte-level CSV contracts. Similarity output must not influence score export.
- Keep all generated artifacts under the configured grading run and preserve the supplied `Project` inputs.
- Do not include Sera's open, unmerged PR #6 or treat it as approval/evidence for B2.
- Keep the historical 86-test modularisation run, six sample totals, 120 cases, HTML/Markdown output, 10 synthetic byte-identical artifacts, and 49 public API comparison as historical evidence. They are not post-merge verification.

## Verification and release gates

Focused integration verification passed 10 tests with zero failures/errors/skips using:
/private/tmp/cs102-modularisation-tools/apache-maven-3.9.16/bin/mvn -Dmaven.repo.local=/tmp/cs102-modularisation-maven-repository -Dtest=GradingPipelineTest,AppTest test

This run covered detector invocation/positive output, header-only output when absent, IOException/runtime failure continuation, preserved score withholding, and HTML/report flow. Its first attempt caught an incorrect new test expectation for HTML rounding; the expectation was corrected to the existing 87.654% display behavior, then the run passed.

Clean verification then passed 110 tests with zero failures/errors/skips on JDK 25 targeting Java 17. Two test assertions were added afterward: one for header-only no-op output and one for byte preservation of similarity.csv after HTML rendering. The final GradingPipelineTest run passed 4/4; no production code changed after the 110-test build, so the full suite was not rerun.

The packaged sample used that same production code and exited successfully with plagiarism enabled and zero pairs. It retained scores 12,10,10,0,20,1, six submission rows and 120 case-data rows. It produced dashboard.html, six Markdown reports and a header-only similarity.csv in build/grading/run-8584886716168395256. The dashboard contained zero HTTP(S) external-asset references. At the verification snapshot, the protected-input manifest matched all 63 Project non-metadata files plus the ignored handoff. The handoff may be intentionally refreshed after publication; that planned update is separate from the verified snapshot.

The current touched production files are App.java (193 physical lines), GradingPipeline.java (209) and GradingDiagnosticsWriter.java (89). No other changed production method exceeds 40 nonblank, noncomment lines. App.run has 58 nonblank, noncomment lines; retain it as a documented exception because it is the CLI orchestration path coordinating one configured grading/report flow. For the three production files changed in this merge, no other changed method exceeds 40 nonblank, noncomment lines. Existing M1–M5 exceptions in other files remain documented separately in the historical modularisation review.

Builds used JDK 25 targeting Java 17. No actual Java 17 runtime, Windows run or new real-browser check was performed. The concurrent .DS_Store change is preserved as found and excluded from exact preservation claims.

Exact full-build command:
`/tmp/cs102-modularisation-tools/apache-maven-3.9.16/bin/mvn -Dmaven.repo.local=/tmp/cs102-modularisation-maven-repository clean verify`

Packaged run:
`java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties`

The targeted 4-test run used:
`/private/tmp/cs102-modularisation-tools/apache-maven-3.9.16/bin/mvn -Dmaven.repo.local=/tmp/cs102-modularisation-maven-repository -Dtest=GradingPipelineTest test`


PR #5 remains a draft while Boey's clean-build/behaviour acceptance and another teammate's review are pending. Publishing or updating that PR does not authorize merging it into `main`. Sera's PR #6 remains open and unmerged; coordinate any overlap with Sera and do not incorporate its changes. Human method-level attribution and the presentation AI Use appendix remain release checklist items owned by the team.

## Publication evidence

The replacement change targets the existing PR #5, [code-modularisation into main](https://github.com/boeyboeyboeyboey/CS102-Group3/pull/5). Its verified pre-merge branch head was 08269bcd56286c067c97058ac6ffcd47100de36b; the fetched main base was 268f4315e3f3de26d0deaaf82ecba4d5b3a52b9b. Git used a normal no-commit/no-fast-forward merge, with GradingPipeline.java as the sole content conflict. The resulting commit and branch head are verifiable in PR #5's commit list after the Git agent publishes the approved files. No merge into main is authorized. The PR remains draft pending Boey's clean-build/behaviour acceptance and another teammate's review.

The verification evidence comes from the implementing agent's command results, Maven's target/surefire-reports, and the packaged sample artifacts. No separate Maven console-log files were saved. The ignored local handoff may be refreshed after publication to record the latest continuation facts; that planned update is outside the protected-input snapshot documented above.
