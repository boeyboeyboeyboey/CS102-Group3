# Code review and modularisation

Reviewed on 2026-10-14 for Boey's approved review/documentation phase. Source baseline: `c769fa93fc25b28b2a8b1af6806949e72de97979` (accepted PR #3 merged into main). Review began at PR #2's merge `a99190f`; when main advanced, those new teammate changes were merged into the review branch and the new/modified files were reviewed. Working branch: `code-modularisation`. Boey subsequently directly requested the actual behaviour-preserving modularisation and a replacement PR. Documentation-only PR #4 was closed unmerged. M1-M5 are now implemented on this branch, in REVIEW pending human clean-build/behaviour acceptance; the separate bug fixes remain unapproved.

## PR #5 integration status

Kiya's feature-1 PR #1 merged into main on 2026-10-15, after the modularisation branch's 86-test verification. PR #5 now integrates that detector with M1-M5. At the start of resolution the only reported conflict was GradingPipeline.java, and the Git merge was paused without a merge commit. The modularisation evidence below remains valid for its recorded branch snapshot but does not verify this integration. The focused GradingPipelineTest/AppTest run passed 10 tests with zero failures/errors/skips. Clean verification then passed 110 tests with zero failures/errors/skips; two later test-only assertions passed in a targeted GradingPipelineTest run (4/4), with no production changes after the full run. The packaged sample succeeded with six expected totals, 120 case rows, six submission rows, six Markdown reports, dashboard.html and a header-only similarity.csv; the dashboard had no HTTP(S) asset references. All 63 Project non-metadata input files and the ignored handoff matched the protected manifest. Current physical counts are App.java 193 lines, GradingPipeline.java 209 and GradingDiagnosticsWriter.java 89. App.run is 58 nonblank/noncomment lines and remains a justified CLI-orchestration exception; no other changed production method exceeds 40. The detailed chronology and preservation result are in [PR5_MERGE_RESOLUTION.md](PR5_MERGE_RESOLUTION.md). Human clean-build/behaviour acceptance and teammate review remain pending. The full contract and audit are in [PR5_MERGE_RESOLUTION.md](PR5_MERGE_RESOLUTION.md). PR #6 is open and unmerged; none of its changes are included or used to claim B2 resolved.

## Scope, ownership and evidence

The original source review covered every then-tracked application Java file, Java resource adapter, dashboard asset, test file, `pom.xml`, `config.properties` and `.gitignore`: 48 application Java files (5,398 lines), 17 test files (2,660 lines), 5 adapter resources (243 lines), 2 browser assets (246 lines), and 3 build/configuration files. The inventory below records all 75 reviewed files. Supplied assessment/student inputs, unrelated files, generated output and teammates' unmerged branches are outside the code-edit scope.

For the source-review phase, main reviewed application/pipeline integration, APIs, shared models, their tests and build/settings; it owns all documentation and final design decisions. One bounded Luna reconnaissance agent reviewed reporting/configuration/CSV/roster/assets/tests and the later merged feedback delta; one bounded Luna/xhigh agent reviewed execution/submission/provider/adapters/tests. Neither agent edited files or spawned agents. Main inspected their evidence and the cross-component boundaries, including the newly merged feedback implementation. The execution agent's additional documentation verification was interrupted by a usage limit; main completed that targeted check from the source and its already-completed full review.

That initial source-review phase did not run Maven tests, a grading run or browser checks. The subsequent implementation evidence is recorded below. The one new runtime probe used a disposable temporary directory to establish local filesystem case aliasing; it did not execute the application. Documentation checks and preservation hashes are reported separately in the PR.

The accepted MVP/original-dashboard evidence and completed heatmap agent evidence remain historical: heatmap focused tests 14/14, clean build 67 tests, mock-DOM fixture 2/2, six expected totals and 120 case records. The DOM fixture is not a real browser/keyboard pass. Agent Java 17 runtime and Windows verification were not performed; a separate new human heatmap clean-build/display transcript remains uncaptured despite PR acceptance and merge. PR #3's upstream record reports Windows 11/Temurin 25.0.4/Maven 3.9.16, 80/81 tests passing, and a packaged run producing HTML plus six feedback files with the expected totals. That is recorded teammate evidence, not a new independent agent run or clean Windows build; B2 records its test failure.

## Implemented revision and checks

The main agent integrated the grading helpers/CSV writer and documentation, reviewed delegated changes, and kept the public contracts and shared models intact. One bounded Luna/xhigh agent implemented HTML then feedback helpers; another implemented identity then configuration helpers and direct config tests. No agents edited overlapping files, ran concurrent builds or spawned further agents. No unmerged teammate branch was included; Gabriel retains ownership of the already-merged feedback feature.

| Facade | Before / after physical lines | Collaborators now responsible for extracted work |
| --- | ---: | --- |
| `OfflineHtmlReportGenerator` | 681 / 100 | Summary, submission table/details, cases, analysis, escaping, decimal formatting and shared submission presentation; immutable `QuestionSection` |
| `AppConfig` | 611 / 240 | `AppConfigLoader`, `AssessmentConfigLoader`, `ConfigPathPolicy`, `PropertyParsers` |
| `IdentityResolver` | 578 / 116 | `IdentityEvidenceCollector`, `RosterIdentityMatcher`, `HeaderCorrectionPlanner`, `HeaderCorrectionApplier`, immutable `IdentityEvidence` transfer data |
| `GradingPipeline` | 193 / 187 | `GradingDiagnosticsWriter`; focused per-submission/per-question and compilation-failure helpers |
| `MarkdownFeedbackGenerator` | 397 / 336 | `MarkdownDiagnosticFormatter`, `FeedbackFileWriter`; section composition retained |

The branch has 69 application Java files (5,990 physical lines) and 18 test files (2,778 lines). The increase is 21 package-private collaborators/value containers plus their imports and boundaries; it is not a new feature. CSS/JavaScript, public APIs, assessment inputs, adapters, dependencies and operational settings remain unchanged.

Actual commands, from the repository root:

```sh
/tmp/cs102-modularisation-tools/apache-maven-3.9.16/bin/mvn -Dmaven.repo.local=/tmp/cs102-modularisation-maven-repository clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

- Clean verification passed 86 tests across 18 suites, with zero failures/errors/skips. Five new `AppConfigTest` scenarios cover config-relative paths, configured order/weights, immutable collections and representative duplicate-list, regex, overlap and numeric failures. The first sandboxed attempt stopped at dependency DNS resolution before tests; the exact authorised command succeeded with network access and a temporary Maven repository
- The packaged sample exited successfully: Chee 12, David 10, Fen 10, Jing 0, Ping 20, Xing 1; six submissions, 120 case rows, `dashboard.html` and six Markdown files in `build/grading/run-13212618080201842227`
- All 63 non-metadata Project input files and the four protected handoff/build/settings files matched the 71-path pre-edit snapshot; its four Project `.DS_Store` entries are excluded from that claim. Unrelated root `.DS_Store` changes remain preserved and uncommitted
- Verification used Temurin JDK 25 with Java 17 compiler target. Actual Java 17 runtime, Windows and a new real-browser pass were not performed. The existing Windows-invalid hostile-ID fixture (B2) remains unchanged; this macOS pass does not resolve it

The saved pre-edit JAR and verified target JAR produced 10 byte-identical deterministic artifacts totalling 95,036 bytes: three HTML reports, three Markdown files, three blocked-batch CSVs and a config/assessment snapshot. Synthetic fixtures exercised empty/all-withheld batches, all six statuses, decimal scores, duplicate identities, hostile text, corrections, supplied analysis, unmatched question IDs, JVM-notice filtering, dynamic fences, bounded diagnostics, compiler-path redaction, missing-source explanation and repeated safe filename allocation. The CSV fixtures used the actual configuration/instructor cases with two duplicate blockers and one unresolved identity (60 NOT_RUN rows); they did not compile student code. This comparison establishes exact output preservation for those fixtures, not all possible inputs or actual browser behaviour. Harness/output: `/tmp/cs102-modularisation-differential/`; fixture compiled with `--release 17` and executed on Temurin 25.0.4.

Static checks compared 49 public class surfaces (48 original top-level application classes plus `IdentityResolver.Resolution`) with `javap -public`: zero signature/declaration mismatches. All 21 new helpers/value containers are package-private and all 27 changed/new Java files have one top-level owner. A declaration scan of 240 methods/constructors in those files found only the documented 51-line `AppConfig` constructor above the method trigger. No new wildcard imports or AI-attribution comments were found. Markdown checks passed 107 local links/anchors, 25 tables and balanced code fences; `git diff --check` passed.

Temporary local logs are `/tmp/cs102-modularisation-verify.log` and `/tmp/cs102-modularisation-sample.log`; these are execution evidence, not repository deliverables. Human clean-build/behaviour acceptance and teammate review remain pending. Existing accepted MVP/dashboard status is retained.

### Justified size exceptions

| File/method | Size and convention | Responsibility and reason to keep it together | Checks and limits |
| --- | --- | --- | --- |
| `MarkdownFeedbackGenerator.java` | 336 total physical lines | Composes the existing Markdown sections in one readable sequence after diagnostic formatting and filesystem work have been extracted; further arbitrary section classes would add indirection | Existing feedback/App tests passed; human acceptance pending |
| `AppConfig` constructor, lines 49-99 | 51 nonblank/noncomment lines, declaration through closing brace | Initialises the immutable aggregate and makes defensive copies; retain the aggregate/getter contract rather than introducing unrelated grouped configuration objects | Five direct config tests and existing ingestion/config/pipeline checks passed |
| `QuestionOutcome` constructor, lines 49-97 | 47 nonblank/noncomment lines | Existing coherent calculation of one immutable question outcome, counts and band; untouched in this revision | Outcome and renderer tests passed; existing browser/platform limits remain |
| `App.run`, lines 35-88 | 52 nonblank/noncomment lines | Existing linear CLI startup/preparation/grading/report flow and error boundary; untouched in this revision | CLI/pipeline tests and sample run passed |

## Source-review findings

| ID | Priority | Finding | Proposed treatment |
| --- | --- | --- | --- |
| M1 | First batch | `OfflineHtmlReportGenerator` combines document/file orchestration, summaries, rows, diagnostics, analysis and presentation helpers in 681 lines | Extract cohesive report components; retain the public generator and current markup/behaviour |
| M2 | Second batch | `AppConfig` combines immutable settings, properties parsing, assessment loading and path policy in 611 lines | Delegate loading and validation while retaining `load(Path)`, getters and the existing canonicalisation bridge |
| M3 | Third batch | `IdentityResolver` collects evidence, matches a roster, plans repairs and writes corrected files in 578 lines | Separate evidence, matching, repair planning and mutation; retain the resolution facade/result |
| M4 | Later bounded batch | `GradingPipeline` is only 193 lines, but `grade` combines nested per-submission/per-question execution and `writeReports` owns three CSV projections | Extract diagnostic persistence and clarify execution helpers while keeping eligibility and ordering authoritative |
| M5 | Coordinate with Gabriel | Newly merged `MarkdownFeedbackGenerator` combines file naming/writing, Markdown sections and diagnostic formatting in 397 lines | Discuss focused diagnostic/output-naming extraction with its owner after the shared review; preserve existing feedback semantics |
| S1 | Scoped cleanup | Some existing Java files use wildcard imports or tightly grouped getters; new style calls for explicit imports and consistent spacing | Correct only deliberately edited areas; no broad formatting-only rewrite |
| B1 | Separate correctness approval | Case variants of reserved CSV filenames can overwrite diagnostics on a case-insensitive filesystem | Add explicit validation only after a separate bug-fix approval; do not mix it into pure extraction |
| B2 | Separate correctness approval | The hostile question-ID test builds a Windows-invalid tester path; upstream reports an 80/81 test run | Decouple the synthetic tester filename from hostile display text, retaining escaping coverage; no fix in this phase |

No other material correctness defect was established in the reviewed evidence. This is not a guarantee that no defects exist. The known controlled-course execution limits remain documented; changing them is outside this modularisation proposal. Feedback currently derives its withholding notice from NOT_RUN cases rather than receiving the run's authoritative map; M5 records that contract limitation without claiming a proven export discrepancy in the current pipeline.

## Reviewed extraction boundaries

The following records the boundary decisions used for M1-M5 and the original source line references at `c769fa9`. The collaborators are now package-private, use composition and retain the Java 17/Maven layout. Follow-up substantial changes still require their own approved plan; these historical recommendations do not authorise additional work.

### M1: report rendering

Keep `OfflineHtmlReportGenerator` as the public boundary: its constructor validates the filename, and `generate` validates the supplied context/output directory, creates the directory and writes the report. Keep document/resource composition in the generator. Proposed collaborators in `autograder.report.html`:

| Proposed component | Existing logic to move | Preservation constraints |
| --- | --- | --- |
| `HtmlSummaryRenderer` | `appendSummary`, `metric`, `median` and exported-score summary calculations (generator lines 100-143, 580-589) | Same authoritative withheld map; exported-only mean/median; existing rounding/captions |
| `HtmlSubmissionTableRenderer` | Controls/table assembly, `appendStudentRow` and row identity/diagnostic display (145-263) | Same search attributes, row IDs, control IDs, sort values, received/archive rows and withheld labels |
| `HtmlSubmissionDetailsRenderer` | `appendDetailRow`, corrections and detail metadata (264-340, 541-545) | Same roster/raw identity, warnings, corrections, paths and native details |
| `HtmlCaseRenderer` | Case groups/tables/output, score cells/status counts, question grouping and unmatched cases (341-496) | Same configured order, ordinal/index anchors, neutral states, mixed status counts and all recorded evidence |
| `HtmlAnalysisRenderer` | `appendAnalysisSection` (497-540) | Display supplied findings only; retain no-data explanation |
| `HtmlEscaper` | `escape` (650-667) | One escaping implementation for every untrusted text/attribute value |
| `HtmlScoreFormatter` | Decimal formatting, unavailable-value handling and recorded-score/max conversion/access helpers (567-579, 590-610) | Same decimal values and display formatting; no new scoring policy |
| `QuestionSection` | Existing private record (678-680) | Extract package-locally with a defensive-copy constructor for recorded cases, preserving the current immutable-list construction; anchor ordinals allocated by one owner |

Display-name/path helpers belong with the submission renderers; share only genuinely reused display logic in a focused submission-presentation helper if review of the moved code requires it. `QuestionOutcome` remains unchanged as the existing immutable evidence/credit-band helper. Retain resource loading inside the generator unless a concrete reuse need appears. Keep CSS/JS inline and preserve their selectors; no visual redesign or script behaviour change is proposed.

Existing renderer, outcome, context and pipeline tests remain black-box regression coverage. Add focused tests only for meaningful extraction seams or missing invariants; avoid tests that merely assert collaborator calls. A targeted interaction/manual pass may be needed if markup changes cannot be avoided, within the established browser restrictions.

### M2: configuration loading

Keep `AppConfig` as the immutable aggregate and compatibility facade. `load(Path)` delegates without changing property names, defaults, getter signatures or error semantics. Proposed collaborators in `autograder.config`:

| Proposed component | Existing logic to move | Preservation constraints |
| --- | --- | --- |
| `AppConfigLoader` | File/properties loading, top-level validation sequence and aggregate construction (112-191), score-column mapping (193-200) | Same configuration-file base directory, required keys and immutable aggregate |
| `AssessmentConfigLoader` | `loadAssessment`, assessment canonicalisation (201-266) | Same configured question order, source/companion names, counts, weights and protected resources |
| `ConfigPathPolicy` | `canonicalize`, `validateOutputPaths`, `requireDisjoint`, `pathList`, `resolvePath` (268-365, 396-403) | Same real-path/symlink handling, protected-input disjointness and path resolution |
| `PropertyParsers` | Required values, name lists, prefixes, regex capture requirements and numeric parsers (366-395, 404-479) | Preserve validation details and exception behaviour; share with other config loaders only after reviewing compatibility |

`ExecutionConfig` calls package-private `AppConfig.canonicalize`; retain that delegating bridge initially. Keep the small `ExecutionConfig` and `HtmlReportConfig` records together. Avoid introducing grouped settings objects that force changes across all consumers in this first pass.

Add focused `AppConfigTest` coverage for valid/invalid loading and path/regex/numeric policies extracted from ingestion integration fixtures. Preserve existing execution/HTML config tests and ingestion tests rather than relocating them merely to match implementation classes.

### M3: identity and corrections

Keep `IdentityResolver.resolve(...)`, `applyCorrections(...)` and public nested `Resolution` initially. The two public methods delegate to focused collaborators in `autograder.submission` so current callers continue to work:

| Proposed component | Existing logic to move | Preservation constraints |
| --- | --- | --- |
| `IdentityEvidenceCollector` | Archive/wrapper evidence, required-source checks and bounded header parsing (142-298) | Same strong-evidence classification, exact-case file checks, header limit and anomaly order |
| `RosterIdentityMatcher` | Unique roster matching, evidence matching and roster-prefix handling (300-358) | Same ambiguous/unresolved outcomes; no guessed ownership |
| `HeaderCorrectionPlanner` | Header reconciliation, field correction proposals, unresolved anomalies and canonical roster names (360-431, 459-480) | Repair only missing values for a unique owner; preserve nonempty conflicting values and warnings |
| `HeaderCorrectionApplier` | `applyCorrections`, field patterns and labels (81-140, 433-457) | Mutate only working copies; preserve containment, bounded matching, inserted header order and returned applied corrections |

Move small evidence/header/source data types to their owning stage; use immutable package-local transfer data where multiple stages consume it. Preserve existing list order and `Resolution` outputs. `WorkingCopyPreparer` may retain its current call through the facade initially; remove that coupling only if an approved implementation plan explicitly wires the applier directly. Unused stored `ParsedSource.sourcePath` is optional cleanup, not a bug.

Use focused collection/matching/planning/applier tests for the new boundaries, with existing ingestion and pipeline tests preserving end-to-end identity, warnings, correction logs and withholding decisions. Source input hashes remain protected.

### M4: pipeline responsibilities

Propose a package-private `GradingDiagnosticsWriter` to own the `cases.csv` and `submissions.csv` projections and score-writer invocation from `writeReports` (159-192). It consumes the completed `GradingRun` and configuration; it does not decide eligibility. Existing `CsvScoreReportWriter` remains responsible for schema/score validation and atomic score-sheet output.

Keep batch sequencing, blockers and infrastructure withholding in `GradingPipeline`. If its long `grade` method remains hard to follow after report extraction, introduce focused per-submission/per-question private helpers in a bounded follow-up. Preserve all expected case records, independent compilation, per-case timeouts, continuation after infrastructure errors and report order. Do not introduce a new provider/injection architecture or implement William's feature 4 in this work.

`App` remains the CLI coordinator. Its linear `run` method is a reasonable size-review exception at this stage: it keeps the startup/preparation/grading/report flow visible, with error handling at the CLI boundary. Extract console formatting later only if it materially improves readability. Existing `AppTest` and `GradingPipelineTest` remain the observable contract checks. Exception record at the extended baseline: `App.java:35-88`, 54 physical lines, 52 nonblank/noncomment lines; responsibility is end-to-end CLI coordination, including both optional report hooks; retaining this sequence helps readers see stage order and the exit/error boundary. Existing CLI/pipeline tests were inspected, not rerun; live runtime/platform limits remain as recorded above.

### M5: newly merged Markdown feedback

Gabriel owns feature 5, recorded REVIEW; its merge does not imply a separately captured human completion gate. Boey directly authorised the bounded helper extraction from the already-merged implementation; no feature claim or public feedback-contract change was made. Coordinate future shared changes with its owner. Keep the public `FeedbackGenerator.generate(StudentGrade, Path)` contract and `MarkdownFeedbackGenerator` facade initially. Two cohesive candidate collaborators in `autograder.report.feedback` are `MarkdownDiagnosticFormatter` (bounded output, launcher-notice filtering, dynamic fences and workspace-path redaction) and `FeedbackFileWriter` (sanitised basename, suffix allocation and CREATE_NEW persistence). Keep section composition readable; extract additional sections only where their responsibilities justify it.

Preserve recorded statuses/marks, line-limit notices, missing-file explanations, roster formatting, Markdown escaping, question/case order and continued generation after one student's write failure. `FeedbackConfig` is a small cohesive record/loader and should remain intact. The changed `App` invokes HTML then feedback and returns nonzero if either optional report fails; retain that sequence and the independent report attempts.

The existing feedback contract contains a grade but no authoritative withholding map. The implementation currently derives its notice from NOT_RUN records. A pure refactor retains that behaviour; changing eligibility presentation or adding withholding context requires a separate coordinated API/design plan. The current fixed pipeline creates NOT_RUN records for its withholding paths, so no concrete exported-score discrepancy was established here.

Existing feedback tests cover rendering, recorded outcomes, missing source/compilation, withheld notices, hostile text/fences, bounded diagnostics, safe unique filenames, corrections, roster/JVM-noise formatting and invalid limits. App wrapper tests cover disabled output and continuation after a student's failure. The delta adds no full App.run configuration-to-feedback integration test; that remains a targeted integration consideration for later approved changes.

## Separate correctness and hardening findings

### B1: case-variant report names can replace diagnostic CSVs

Tracked in [BUGS.md — B1](BUGS.md#b1-csv-filename-case-can-overwrite-diagnostics): case variants of reserved score-report filenames can replace diagnostic CSVs on case-insensitive filesystems. The register preserves the source/filesystem evidence and its reproduction limits, current code locations, suggested validation checks and Boey's shared-core coordination role. No fix is approved or included in this refactor.

### H1: conditional multiline anomaly records

`AnomalyLog.java:17` removes tabs from an archive basename but retains CR/LF. A custom archive regex allowing line breaks, or another direct logger caller, could split a record. The supplied archive regex uses Java's default dot behaviour and rejects line-break basenames, so this was not established as a defect in the supplied intake flow. Treat filename normalisation as optional hardening requiring separate approval, not part of pure modularisation.

### B2: hostile display text creates a Windows-invalid test path

Tracked in [BUGS.md — B2](BUGS.md#b2-dashboard-test-uses-a-windows-invalid-filename): the escaping-test fixture uses a hostile display ID in a Windows-invalid tester filename. The register preserves the reported Windows 80/81 result, source evidence, platform limits and proposed acceptance checks. Boey owns the affected feature-2 tests; no production dashboard defect or new Windows pass is claimed.

### Existing limits and evidence gaps

- Current-user JVM permissions, protocol tampering and best-effort descendant cleanup remain documented course-grading limits; stronger adversarial isolation is external
- Fixed adapters support reviewed supplied testers, not arbitrary edited Java testers. Per-case fresh JVM state can differ from an original single-process stateful tester
- Adapter equivalence tests compare aggregate outcomes on a known-good submission and limited wrong-answer/expected-exception cases; they do not cover every varied submission per case
- Execution tests cover a looping child and interruption, but do not establish cleanup of an arbitrary process tree
- Dashboard CSS has a 390px minimum body width; arbitrarily narrow layout, network-disabled demonstrations and real browser keyboard behaviour remain unverified, as already documented
- Some model constructors rely on validated upstream data and allow nullable preparation paths. Document those contracts before considering stricter validation; changing invalid-input behaviour is separate from extraction

## Verification rationale and separate follow-ups

| Batch | Targeted existing checks | New checks justified by extraction |
| --- | --- | --- |
| M1 | `OfflineHtmlReportGeneratorTest,QuestionOutcomeTest,GradingReportContextTest,AppTest,GradingPipelineTest` | Same fixture output/anchors/eligibility and hostile text; targeted navigation if markup changes |
| M2 | `ExecutionConfigTest,HtmlReportConfigTest,SubmissionProcessorTest,GradingPipelineTest` | Direct AppConfig loading/path/regex/numeric regression scenarios at the new parsing boundaries |
| M3 | `SubmissionProcessorTest,GradingPipelineTest` | Focused evidence/matching/planning/application tests where independent seams merit them |
| M4 | `GradingPipelineTest,AppTest,CsvScoreReportWriterTest` | Same case/submission CSV fields, export eligibility, diagnostic persistence and failure behaviour |
| M5 (owner coordination) | `MarkdownFeedbackGeneratorTest,FeedbackConfigTest,AppTest` | Same Markdown/diagnostic/name behaviour and optional-report failure sequencing |
| B1 (separate approval) | `ExecutionConfigTest` | Case variants of reserved names rejected; valid custom name remains accepted |
| B2 (separate approval) | `OfflineHtmlReportGeneratorTest` | Portable synthetic paths with unchanged hostile-text coverage; actual Windows confirmation separately recorded |

The implementation used one successful clean verification and one packaged sample run at final integration, as recorded above. Expected supplied totals remain Chee 12, David 10, Fen 10, Jing 0, Ping 20 and Xing 1, with six submissions and 120 case records. Compare semantic decisions/statuses/fields across independent runs; timings and run paths can vary. Existing fixtures assert the three completed CSVs remain byte-identical during HTML rendering. Check supplied-input hashes and record actual platform/runtime/browser limits.

Keep refactoring and bug fixes in focused branches/PRs and coordinate shared-file ownership. Publish reviewed approved files to `main` through a PR, give Boey its URL and ask him to contact the group. Human clean-build/behaviour acceptance remains required for substantial implemented revisions. No automatic merge, teammate messages, optional enhancements, bonus claim changes or human disclosure writing are authorised by this review.

## Brief/rubric alignment

The actual `CS102-Project-Auto Grading System.pdf` was read, and relevant pages were rechecked before planning. Page 6 calls for separated major responsibilities, focused methods, clear packages, consistent conventions, external configuration and understandable class relationships; M1-M5 address those concerns through composition and explicit boundaries. Page 7's edge-case handling/usability criteria motivate preserving identity, export, failure and navigation behaviour through observable regression checks. Page 1's demonstration expectations favour a stable rehearsable pipeline and explainable design. Page 3 permits Maven and only free/open-source libraries; no new runtime or style-tool dependency is proposed. Human attribution/AI Use disclosure remains a release duty under the repository's explicit agent exception.

See [CODING_STYLE.md](CODING_STYLE.md) for future coding rules. The inventory below includes deliberate keep decisions; class count and line count alone do not measure design quality.

## Complete source-baseline reviewed-file inventory

This historical inventory is not the post-refactor file list. Line counts are physical lines at the extended source baseline `c769fa9`; paths resolve from the repository root. Each listed file was read by main or its bounded reviewer and integrated into this report. Test size is assessed by scenario readability rather than a production-file cap.

### Application Java

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [src/main/java/autograder/App.java](../src/main/java/autograder/App.java) | 168 | CLI orchestration, exit status and report coordination | Keep coordinator; method-size exception recorded in M4 |
| [src/main/java/autograder/api/BoilerplateDetector.java](../src/main/java/autograder/api/BoilerplateDetector.java) | 12 | Starter-analysis extension contract | Keep; Sera owns implementation |
| [src/main/java/autograder/api/FeedbackGenerator.java](../src/main/java/autograder/api/FeedbackGenerator.java) | 9 | Student-feedback extension contract | Keep; coordinate feature integration |
| [src/main/java/autograder/api/HtmlReportGenerator.java](../src/main/java/autograder/api/HtmlReportGenerator.java) | 9 | Completed-context HTML output contract | Keep public boundary |
| [src/main/java/autograder/api/PlagiarismDetector.java](../src/main/java/autograder/api/PlagiarismDetector.java) | 12 | Similarity-analysis extension contract | Keep; Kiya owns implementation |
| [src/main/java/autograder/api/ProgressListener.java](../src/main/java/autograder/api/ProgressListener.java) | 5 | Progress event contract | Keep; coordinate richer events |
| [src/main/java/autograder/api/TestCaseProvider.java](../src/main/java/autograder/api/TestCaseProvider.java) | 10 | Runnable-case provider contract | Keep; coordinate William’s feature |
| [src/main/java/autograder/config/AppConfig.java](../src/main/java/autograder/config/AppConfig.java) | 611 | Immutable settings plus properties/assessment/path loading | Extract loading/policy (M2); retain facade |
| [src/main/java/autograder/config/ExecutionConfig.java](../src/main/java/autograder/config/ExecutionConfig.java) | 86 | Execution settings and grading-output validation | Keep cohesive record; B1 requires separate fix |
| [src/main/java/autograder/config/FeedbackConfig.java](../src/main/java/autograder/config/FeedbackConfig.java) | 47 | Feedback enablement, safe subdirectory and line cap | Keep cohesive record/loader |
| [src/main/java/autograder/config/HtmlReportConfig.java](../src/main/java/autograder/config/HtmlReportConfig.java) | 29 | Optional report enablement and safe basename | Keep cohesive record |
| [src/main/java/autograder/csv/CsvCodec.java](../src/main/java/autograder/csv/CsvCodec.java) | 128 | CSV quoting, records, parsing and serialization | Keep focused codec |
| [src/main/java/autograder/execution/CaseExecutor.java](../src/main/java/autograder/execution/CaseExecutor.java) | 92 | Fixture copying, one case and result protocol | Keep execution boundary |
| [src/main/java/autograder/execution/ProcessResult.java](../src/main/java/autograder/execution/ProcessResult.java) | 5 | Immutable child-process outcome | Keep small value record |
| [src/main/java/autograder/execution/ProcessRunner.java](../src/main/java/autograder/execution/ProcessRunner.java) | 225 | Bounded streams, deadlines and cleanup | Keep coupled lifecycle service |
| [src/main/java/autograder/execution/StudentCompiler.java](../src/main/java/autograder/execution/StudentCompiler.java) | 92 | Part workspace and selected compiler inputs | Keep compilation boundary |
| [src/main/java/autograder/grading/GradingPipeline.java](../src/main/java/autograder/grading/GradingPipeline.java) | 193 | Batch/part execution, eligibility and CSV projections | Extract diagnostics; clarify private helpers (M4) |
| [src/main/java/autograder/grading/GradingRun.java](../src/main/java/autograder/grading/GradingRun.java) | 13 | Completed batch, run path and copied decisions | Keep immutable result boundary |
| [src/main/java/autograder/grading/InstructorTestCaseProvider.java](../src/main/java/autograder/grading/InstructorTestCaseProvider.java) | 91 | Fixed tester validation and case definitions | Keep supported-assessment boundary |
| [src/main/java/autograder/model/AnalysisFinding.java](../src/main/java/autograder/model/AnalysisFinding.java) | 26 | Recorded source-analysis evidence | Keep small immutable model |
| [src/main/java/autograder/model/AssessmentDefinition.java](../src/main/java/autograder/model/AssessmentDefinition.java) | 46 | Assessment metadata and decimal maximum | Keep coherent aggregate |
| [src/main/java/autograder/model/BatchResult.java](../src/main/java/autograder/model/BatchResult.java) | 32 | Completed grades and supplied analysis lists | Keep copied result aggregate |
| [src/main/java/autograder/model/GradingReportContext.java](../src/main/java/autograder/model/GradingReportContext.java) | 22 | Batch and validated copied withholding decisions | Keep authoritative reporting boundary |
| [src/main/java/autograder/model/QuestionDefinition.java](../src/main/java/autograder/model/QuestionDefinition.java) | 78 | Question sources, resources, counts and weights | Keep coherent metadata model |
| [src/main/java/autograder/model/ScoreSheet.java](../src/main/java/autograder/model/ScoreSheet.java) | 56 | Raw roster schema, rows and student mapping | Keep protected nested collections |
| [src/main/java/autograder/model/SimilarityFinding.java](../src/main/java/autograder/model/SimilarityFinding.java) | 36 | Validated pairwise similarity/evidence | Keep focused result model |
| [src/main/java/autograder/model/StudentGrade.java](../src/main/java/autograder/model/StudentGrade.java) | 49 | Recorded-case score aggregation and duplicate check | Keep scoring responsibility |
| [src/main/java/autograder/model/StudentIdentity.java](../src/main/java/autograder/model/StudentIdentity.java) | 85 | Observed headers, matched record and evidence | Keep identity result model |
| [src/main/java/autograder/model/StudentRecord.java](../src/main/java/autograder/model/StudentRecord.java) | 37 | Roster identity fields | Keep small model |
| [src/main/java/autograder/model/SubmissionCorrection.java](../src/main/java/autograder/model/SubmissionCorrection.java) | 53 | Recorded proposed/applied correction data | Keep shared audit model |
| [src/main/java/autograder/model/SubmissionResult.java](../src/main/java/autograder/model/SubmissionResult.java) | 89 | Preparation paths/status/anomalies/corrections | Keep preparation result aggregate |
| [src/main/java/autograder/model/SubmissionStatus.java](../src/main/java/autograder/model/SubmissionStatus.java) | 7 | Preparation-state enum | Keep distinct from test states |
| [src/main/java/autograder/model/TestCaseDefinition.java](../src/main/java/autograder/model/TestCaseDefinition.java) | 43 | Validated runnable case and maximum points | Keep execution contract |
| [src/main/java/autograder/model/TestCaseResult.java](../src/main/java/autograder/model/TestCaseResult.java) | 46 | Status, captured output, timing and earned points | Keep scoring/result boundary |
| [src/main/java/autograder/model/TestStatus.java](../src/main/java/autograder/model/TestStatus.java) | 10 | Recorded execution-status enum | Keep all six states |
| [src/main/java/autograder/report/CsvScoreReportWriter.java](../src/main/java/autograder/report/CsvScoreReportWriter.java) | 143 | Schema-preserving score export and atomic write | Keep persistence/safety boundary |
| [src/main/java/autograder/report/feedback/MarkdownFeedbackGenerator.java](../src/main/java/autograder/report/feedback/MarkdownFeedbackGenerator.java) | 397 | Markdown sections, diagnostics and unique file output | Coordinate diagnostic/output extraction with Gabriel (M5) |
| [src/main/java/autograder/report/html/OfflineHtmlReportGenerator.java](../src/main/java/autograder/report/html/OfflineHtmlReportGenerator.java) | 681 | Document, sections, summaries and HTML persistence | Extract render collaborators (M1) |
| [src/main/java/autograder/report/html/QuestionOutcome.java](../src/main/java/autograder/report/html/QuestionOutcome.java) | 175 | Recorded question evidence and credit bands | Keep existing report-local helper |
| [src/main/java/autograder/roster/ScoreSheetRepository.java](../src/main/java/autograder/roster/ScoreSheetRepository.java) | 144 | CSV roster import and semantic validation | Keep import boundary |
| [src/main/java/autograder/submission/CorrectionLog.java](../src/main/java/autograder/submission/CorrectionLog.java) | 43 | Correction audit persistence | Keep focused logger |
| [src/main/java/autograder/submission/IdentityResolver.java](../src/main/java/autograder/submission/IdentityResolver.java) | 578 | Evidence, matching, repair planning and mutation | Extract staged collaborators (M3); retain facade |
| [src/main/java/autograder/submission/SafeZipExtractor.java](../src/main/java/autograder/submission/SafeZipExtractor.java) | 144 | Bounded ZIP extraction, CRC and containment | Keep safety boundary |
| [src/main/java/autograder/submission/SubmissionProcessor.java](../src/main/java/autograder/submission/SubmissionProcessor.java) | 268 | Intake coordination, duplicates and progress | Keep coordinator; reconsider only if growth warrants |
| [src/main/java/autograder/submission/SubmissionValidator.java](../src/main/java/autograder/submission/SubmissionValidator.java) | 113 | Question-root selection and folder validation | Keep narrow package-local service |
| [src/main/java/autograder/submission/WorkingCopyPreparer.java](../src/main/java/autograder/submission/WorkingCopyPreparer.java) | 118 | Canonical working copy and layout corrections | Keep copy concern; delegate repairs through facade initially |
| [src/main/java/autograder/ui/ConsoleProgressListener.java](../src/main/java/autograder/ui/ConsoleProgressListener.java) | 10 | Plain console progress rendering | Keep ProgressListener adapter |
| [src/main/java/autograder/util/AnomalyLog.java](../src/main/java/autograder/util/AnomalyLog.java) | 32 | Anomaly audit persistence and stderr | Keep; conditional hardening H1 |

### Tests

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [src/test/java/autograder/AppTest.java](../src/test/java/autograder/AppTest.java) | 129 | CLI/HTML handling plus disabled/failed feedback hooks | Keep observable CLI/report wrapper checks |
| [src/test/java/autograder/config/ExecutionConfigTest.java](../src/test/java/autograder/config/ExecutionConfigTest.java) | 60 | Tool/path resolution, overlaps and invalid settings | Keep; add B1 cases only after fix approval |
| [src/test/java/autograder/config/FeedbackConfigTest.java](../src/test/java/autograder/config/FeedbackConfigTest.java) | 55 | Defaults, valid settings and rejected feedback values | Keep public config validation scenarios |
| [src/test/java/autograder/config/HtmlReportConfigTest.java](../src/test/java/autograder/config/HtmlReportConfigTest.java) | 39 | Report defaults, enablement and safe name | Keep public config tests |
| [src/test/java/autograder/execution/ExecutionServicesTest.java](../src/test/java/autograder/execution/ExecutionServicesTest.java) | 181 | Timeout/output/interruption/compile/result protocol | Keep observable execution scenarios |
| [src/test/java/autograder/grading/AdapterEquivalenceTest.java](../src/test/java/autograder/grading/AdapterEquivalenceTest.java) | 224 | Original/adapted scoring and exception comparisons | Keep; limitations recorded separately |
| [src/test/java/autograder/grading/GradingPipelineTest.java](../src/test/java/autograder/grading/GradingPipelineTest.java) | 268 | Fair timeouts, independent parts, withholding and CSV/HTML | Keep end-to-end assertions |
| [src/test/java/autograder/grading/InstructorTestCaseProviderTest.java](../src/test/java/autograder/grading/InstructorTestCaseProviderTest.java) | 109 | Case metadata, hashes and CRLF tester handling | Keep; includes already-merged CRLF correction |
| [src/test/java/autograder/model/GradingReportContextTest.java](../src/test/java/autograder/model/GradingReportContextTest.java) | 40 | Copied authoritative/nonblank batch decisions | Keep immutable-context invariants |
| [src/test/java/autograder/model/StudentGradeTest.java](../src/test/java/autograder/model/StudentGradeTest.java) | 53 | Independent marks, duplicate IDs and decimals | Keep score invariants |
| [src/test/java/autograder/model/TestCaseResultTest.java](../src/test/java/autograder/model/TestCaseResultTest.java) | 34 | Only passes earn points; invalid maximum/duration | Keep status/score invariants |
| [src/test/java/autograder/report/CsvScoreReportWriterTest.java](../src/test/java/autograder/report/CsvScoreReportWriterTest.java) | 110 | Schema/BOM preservation, source protection and bounds | Keep persistence invariants |
| [src/test/java/autograder/report/feedback/MarkdownFeedbackGeneratorTest.java](../src/test/java/autograder/report/feedback/MarkdownFeedbackGeneratorTest.java) | 223 | Outcomes, escaping/fences, diagnostics and safe names | Keep observable feedback assertions |
| [src/test/java/autograder/report/html/OfflineHtmlReportGeneratorTest.java](../src/test/java/autograder/report/html/OfflineHtmlReportGeneratorTest.java) | 371 | Inline assets, escaping, eligibility, links and edge cases | Keep cohesive black-box report assertions; test files have no automatic cap |
| [src/test/java/autograder/report/html/QuestionOutcomeTest.java](../src/test/java/autograder/report/html/QuestionOutcomeTest.java) | 147 | Exact bands, mixed statuses and neutral states | Keep calculation/evidence invariants |
| [src/test/java/autograder/roster/ScoreSheetRepositoryTest.java](../src/test/java/autograder/roster/ScoreSheetRepositoryTest.java) | 136 | Roster/schema/quoting and invalid-row validation | Keep import scenarios |
| [src/test/java/autograder/submission/SubmissionProcessorTest.java](../src/test/java/autograder/submission/SubmissionProcessorTest.java) | 481 | Extraction, identity, repairs, duplicates and progress | Keep ingestion integration scenarios; review organisation when edited |

### Runtime resources

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [src/main/resources/autograder/adapters/Q1aAdapter.java](../src/main/resources/autograder/adapters/Q1aAdapter.java) | 59 | Fixed isogram cases/oracles and result protocol | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q1bAdapter.java](../src/main/resources/autograder/adapters/Q1bAdapter.java) | 42 | Fixed even-sum cases/oracles and result protocol | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q2aAdapter.java](../src/main/resources/autograder/adapters/Q2aAdapter.java) | 40 | Fixture-backed person cases and missing-file oracle | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q2bAdapter.java](../src/main/resources/autograder/adapters/Q2bAdapter.java) | 47 | Student-file cases and expected DataException oracle | Keep standalone compiler resource |
| [src/main/resources/autograder/adapters/Q3Adapter.java](../src/main/resources/autograder/adapters/Q3Adapter.java) | 55 | Shape sorting cases and expected output | Keep standalone compiler resource |
| [src/main/resources/autograder/html/dashboard.css](../src/main/resources/autograder/html/dashboard.css) | 146 | Offline layout, statuses, credit/focus classes and breakpoints | Keep selectors/layout; documented 390px limit |
| [src/main/resources/autograder/html/dashboard.js](../src/main/resources/autograder/html/dashboard.js) | 100 | Search/filter/sort pairing and detail/question navigation | Keep interactions; preserve emitted IDs/data |

### Build and settings

| Reviewed file | Lines | Responsibility | Disposition |
| --- | ---: | --- | --- |
| [pom.xml](../pom.xml) | 48 | Java 17/JUnit/Maven executable packaging | Keep build/dependencies; no new tooling proposed |
| [config.properties](../config.properties) | 96 | Operational settings and supplied assessment mapping | Keep unchanged; defaults do not trigger B1 |
| [.gitignore](../.gitignore) | 8 | Generated output and exact local-handoff ignore rules | Keep; tracked metadata remains unrelated |
