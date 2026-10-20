# Requirements and evidence

This checklist is based on the seven-page project brief. It separates the accepted preparation foundation from the remaining MVP, whose implementation is verified and accepted through Boey’s successful clean build/run on 2026-10-10. No feature becomes DONE until a team member confirms the documented clean build.

## Marking priorities

The brief gives tentative marks of code 7, application 16, presentation 7. It specifies possible penalties up to 10% for usability/input-validation issues and up to 50% for application Java compilation/run failures. The highest rubric band emphasizes understandable object responsibilities, consistent style, external configuration, well-handled edge cases, intuitive operation, a clear presentation, and accurate team-wide answers.

Bonus credit depends on usefulness, correctness, and design quality; there is no stated per-feature entitlement or bonus ceiling. Keep ordinary loops and readable conditionals. Complexity of implementation is not a goal.

## Core acceptance checklist

| Requirement | Implementation / planned work | Evidence required | State |
| --- | --- | --- | --- |
| Batch intake | SubmissionProcessor | All six supplied archives in one run; corrupt archive does not block the next | Foundation verified; user confirmed |
| Safe extraction | SafeZipExtractor | Traversal, entry/byte limits, checksum corruption, cleanup, repeat runs | Foundation verified; user confirmed |
| Required folders and source names | Assessment configuration and submission validator | Missing Q folder, missing required source, wrong filename case | Foundation verified; user confirmed |
| Correct common layout errors | Working-copy preparation | Direct Q folders and wrong wrappers become canonical copies; originals unchanged | Foundation verified; user confirmed |
| Identify students / missing headers | Roster reconciliation and per-file checks | Missing fields corrected only for a unique owner; conflicting evidence withheld | Foundation verified; user confirmed |
| Duplicate submissions | Batch identity checks | Both duplicates require review; no silent replacement | Foundation verified; user confirmed |
| Compile against instructor testers | StudentCompiler with explicit instructor adapters | Required support classes/fixtures, compile error in one question leaves others usable | DONE; verified and user confirmed |
| Stop non-terminating tests fairly | CaseExecutor and independent child processes | Pass-loop-pass yields two passing cases; timeout kills work and later cases run | DONE; verified and user confirmed |
| Capture diagnostic output | ProcessRunner | stderr and stdout captured with limits; output flood cannot block a run | DONE; verified and user confirmed |
| Compute scores | GradingPipeline and shared result models | Five components have maxima 3,3,5,5,4; independently checked total 20 | DONE; verified and user confirmed |
| Exact supplied CSV format | CSV codec, roster repository and score writer | Preserve column order, raw roster fields and end marker; only supplied scores change | DONE; component/pipeline verified and user confirmed |
| Instructor progress/errors/results | CLI and ProgressListener | Clear stage progress, review status, log location, input error messages | Foundation verified; user confirmed |
| Multiple assessments | External assessment definitions | A second synthetic assessment changes required filenames and scoring without Java changes | Foundation verified; fixed instructor adapters only; arbitrary testers remain out of MVP scope |
| Complete grading run | GradingPipeline and CLI integration | Six supplied submissions plus synthetic failures, independently expected scores | DONE; verified and user confirmed |

## Bonus acceptance targets

| Feature | Owner | Demonstration required before completion |
| --- | --- | --- |
| 1: token similarity | Kiya | Known similar and unrelated pairs; starter-code exclusion; evidence; configurable warning threshold; no automatic accusation |
| 2: offline HTML report | Boey | Opens without a server/network; trace a result to the failed case or anomaly; escape student-controlled content |
| 3: starter-code comparison | Sera | Unchanged starter submission detected; genuine changes distinguished; explain comparison limitations |
| 4: parameterized cases | William | New inputs catch a hard-coded solution; invalid case data gives useful errors; retain each question's maximum and the 20-point assessment total |
| 5: Markdown feedback | Gabriel | Explain compilation failure, timeout, mismatch, and missing files using recorded evidence; no invented diagnosis |
| 6: terminal dashboard | Unclaimed | Progress and cancellation; readable non-ANSI fallback; one display failure cannot stop grading |

The contracts exist before implementations. Bonus code must consume common results rather than re-run grading independently. No bonus implementation begins before the core MVP is tested.

## Feature 2 verification (DONE)

Boey approved the original offline HTML plan on 2026-10-10. The implementation consumes the completed batch and authoritative withholding map, externalizes enablement/filename, uses focused Java rendering methods and inline local browser assets, and preserves recorded diagnostics. These address the brief's page 6 reporting bonus and highest code-band responsibilities/packages/configuration criteria; explicit status handling and edge-case tests support the application criteria on page 7.

Focused tests passed 15/15; clean Maven verification passed 59 tests with zero failures/errors/skips. Coverage includes all case statuses, exported review warnings, withholding/partial scores, decimals, empty/all-withheld batches, duplicate and missing identities, roster/header differences, recorded analysis evidence, hostile text escaping, disabled generation and report-write failure with completed CSV preservation. Existing pipeline fixtures assert all three CSVs remain byte-identical after rendering.

The packaged sample run produced all expected six scores, 120 case records, six submission records and `build/grading/run-15042814915555924988/dashboard.html`. All 64 snapshotted supplied files were unchanged. The HTML embeds its styles/scripts without external assets. Automated browser verification was unavailable: the agent browser tool rejected file:// navigation under security policy and isolated Chrome failed to launch. Boey's subsequent build/run and displayed-dashboard acceptance is recorded below. The page 1/page 7 demo/presentation evidence and human release declarations remain team tasks; no grade is promised.

Boey supplied BUILD SUCCESS with 59 tests and zero failures/errors/skips plus all six expected grading scores on 2026-10-10, satisfying the human clean-build/run gate. His dashboard is `build/grading/run-1332949390012507172/dashboard.html`. He then viewed the local report and accepted it with “ok looks good”. Feature 2 is DONE; its implementation/OOP/presentation document is [offline_html_dashboard_architecture.md](../offline_html_dashboard_architecture.md). Automated browser checking remained blocked; specific network-disabled/narrow-viewport assertions were not separately reported and are not claimed. Do not repeat the accepted build request.

## Question heatmap revision (merged; historical verification)

Boey approved the detailed revision on 2026-10-12 with “ok lets do it approved”. The original feature 2 remains accepted DONE. This revision adds credit shading and recorded status counts to existing question cells, links them to grouped diagnostics, and preserves search/filter/sort state. It must preserve all CSV bytes and supplied inputs, use safe numeric anchors and escaped text, retain unexpected-question evidence, and explicitly handle missing/mismatched records, all-NOT_RUN questions, decimals, withheld partial scores, empty batches and duplicate/missing identities.

This uses the page 6 report/design criteria and page 1/page 7 usable-operation and demonstration criteria. Focused tests passed 14/14; the final clean integration build passed 67 tests with zero failures/errors/skips. Existing integration fixtures preserve the three CSVs byte-for-byte after rendering. A built-in-Node DOM API fixture executing the actual script passed two scenarios covering filters/sorting/row pairing and ordinal question navigation, keyboard-like activation, modified clicks and existing Open details. This is not real-browser verification. Scoped numeric checks give heatmap text at least 5.45:1 contrast and cell focus at least 3.84:1 across the six bands.

The packaged run retained totals 12,10,10,0,20,1, six submissions and 120 case records, producing `build/grading/run-3551798997316566219/dashboard.html`. Dependency-free HTML checks verified 30 unique question-link destinations, preserved case evidence, Ping's full-credit cells and Xing's Q2a 1/5 with one pass/four timeouts. All 67 Project input hashes matched the new snapshot. At that verification point, human clean-build and local display/interaction acceptance for the heatmap were pending; the original 59-test acceptance remains historical. Boey later reported PR #2 accepted, and GitHub confirmed it merged on 2026-10-14; a separate human heatmap build/display transcript remains uncaptured. Actual browser, Java 17 runtime and Windows checks were not run. Attention overview and score distribution are unapproved. Boey authorized feature-branch publication and a PR into `main` for group review on 2026-10-14; no merge into `main` is authorized.

## Feature 1 and PR #5 integration (REVIEW)

Kiya's token-based plagiarism detector merged into main in PR #1 on 2026-10-15 and remains REVIEW pending its human completion gate. PR #5 integrates it with the behaviour-preserving modularisation on code-modularisation. The merge is in progress at the recorded single GradingPipeline.java conflict; no merge commit or post-merge verification is yet recorded. The detector compares once after grading against the original submissions and assessment. Similarity output does not alter scores or eligibility; comparison I/O/runtime failures are reported and grading continues with empty findings. The exact CSV contract and ordering are documented in [PR5_MERGE_RESOLUTION.md](PR5_MERGE_RESOLUTION.md).

The focused GradingPipelineTest/AppTest run passed 10 tests with zero failures/errors/skips, covering comparison invocation/success, absent/header-only output, I/O/runtime failure continuation, score withholding and report flow. Its first attempt caught an incorrect new HTML rounding expectation; after matching the existing 87.654% display behavior, the focused run passed. Clean verification passed 110 tests with zero failures/errors/skips. Two later test-only assertions received a targeted GradingPipelineTest run (4/4); production code was unchanged after the full run. The packaged sample exited successfully with plagiarism enabled and zero pairs, retained scores 12,10,10,0,20,1, and wrote the dashboard, six feedback files and similarity.csv. Protected-input hashes still match for 63 Project non-metadata files plus the ignored handoff. The sample produced six score rows, 120 case rows, six submission rows, a header-only similarity.csv, six Markdown reports and a dashboard with no HTTP(S) asset references. Exact commands, artifact path and the App.run size exception are in the merge record. Do not treat the historical 86-test modularisation check as post-merge evidence. The feature remains REVIEW until Boey's clean-build/behaviour confirmation and teammate review. See [CODE_REVIEW.md](CODE_REVIEW.md) for modularisation evidence and [PR5_MERGE_RESOLUTION.md](PR5_MERGE_RESOLUTION.md) for the merge audit.

## Behaviour-preserving modularisation revision (REVIEW)

Boey directly requested actual Java modularisation on 2026-10-14 and replacement of documentation-only PR #4, which was closed unmerged. The implementation separates HTML rendering, configuration loading, identity/correction stages, CSV diagnostic persistence and Markdown diagnostic/file handling while retaining public contracts, settings and scoring/withholding behaviour. These boundaries address the brief's page 6 responsibilities/packages/focused-method criteria; the page 7 robustness criteria are checked through existing integration coverage and five direct config tests.

Before the later PR #1 integration, clean verification passed 86 tests with zero failures/errors/skips. The packaged sample retained six totals 12,10,10,0,20,1 and 120 cases, generating HTML and six Markdown files. Ten synthetic artifacts were byte-identical and 49 public API surfaces matched. These are historical branch evidence, not post-merge results. Human clean-build/behaviour acceptance and teammate review are pending. [CODE_REVIEW.md](CODE_REVIEW.md) records exact commands, size exceptions, evidence and platform limits. B1/B2 remain separately listed and unfixed.

## Team integration gate

All members create and work on their features in their own branches. After approved implementation and verification, publish only the reviewed feature files and create/update a PR targeting `main`; use a draft for pending checks or human acceptance. Inform the driver of the PR URL and ask them to contact the group for review. Require another teammate's review, the documented checks and human build/behavior acceptance before merging, with explicit merge authorization. AI-assisted review does not replace human acceptance or the release declarations. Follow [docs/TEAM_WORKFLOW.md](TEAM_WORKFLOW.md#feature-branches-and-pull-requests); feature acceptance and whether its PR has merged are separate facts.

## Submission checklist

- Build from a clean extraction using only the README commands on another team member's machine
- Verify Java/Maven versions and first-build dependency needs; no reliance on a developer's temporary installation
- Package source, pom.xml, configuration, required resources, README, and the GX-TY presentation deck (actual group/section identifier to be supplied by the team)
- Maven is explicitly permitted in place of compile.sh/run.sh and the alternate classes/lib layout
- Keep generated build workspaces, temporary archives, logs, and local caches out of the submission
- Intentionally broken student fixtures are test data, not application source compiled by Maven
- Use free/open-source dependencies only; list choices and licenses
- Human reviewers complete the method-level AI declarations and presentation appendix after reviewing code
- Submit via eLearn Assignments; the brief says email submissions are ignored
- Brief deadline: 11 November, 11:59 PM, Week 13; team should check course announcements for any changes

## Presentation checklist

Prepare 10–15 minutes of presentation/demonstration and 5 minutes of Q&A. Explain architecture/OOP, library choices, algorithms considered with trade-offs, and useful features. Use readable diagrams and fonts. Demonstrate on one prepared machine with a rehearsed dataset and avoid unnecessary app/laptop switching. Every member must explain the whole pipeline, including code written with assistance. Record genuine contributions and cross-member reviews; the brief considers peer and interview evidence.

## Foundation verification record

The final foundation verification passed 30 Maven tests. The packaged preparation command processed six supplied archives with 4 READY, 2 REVIEW_REQUIRED, 0 FAILED and 22 working-copy corrections. All 63 supplied input file hashes were unchanged. David and Xing remain flagged for nonempty source-name headers that differ from the roster; the tool did not overwrite those names. This is the accepted historical foundation baseline. The completed grading verification record follows below.

## Remaining MVP verification

The remaining MVP is **DONE**. Boey supplied a successful clean build on 2026-10-10 with **48 tests, zero failures/errors/skips**, followed by the expected six-submission grading output (12, 10, 10, 0, 20, 1). The human build-confirmation gate is satisfied. `mvn clean verify` passed **48 tests, zero failures/errors**. After final diagnostic wording/formatting fixes, the affected execution suite passed **7/7** and the executable JAR was rebuilt successfully. The packaged CLI graded all six supplied submissions in one fresh run, producing 120 case records (20 per submission). All scores reconciled between case points, submission summaries and exported CSV numerators. All **67** supplied-file hashes in the new snapshot remained unchanged.

| Student | Q1a | Q1b | Q2a | Q2b | Q3 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Chee | 3 | 3 | 5 | 1 | 0 | 12 |
| David | 3 | 3 | 4 | 0 | 0 | 10 |
| Fen | 1 | 0 | 5 | 4 | 0 | 10 |
| Jing | 0 | 0 | 0 | 0 | 0 | 0 |
| Ping | 3 | 3 | 5 | 5 | 4 | 20 |
| Xing | 0 | 0 | 1 | 0 | 0 | 1 |

David and Xing retain name-header warnings while their uniquely matched submissions are graded. Xing passes Q2a's missing-file case; the remaining four Q2a cases time out. Inspection shows the loop advances to the next line only when a surname matches, so an unmatched line can repeat forever. The other parts fail their assertions. The reference file's components sum to 15 but its stated total is 1; actual execution independently awards 1. Reference files were not changed.

Verification includes original-tester equivalence on Ping (20/20), wrong-answer and expected-exception equivalence, pass–loop–pass retaining 2 points, independent compilation failure, bounded output, interrupted-process cleanup, missing results/System.exit(0), duplicate/unresolved withholding, missing sources, configured-path checks and exact CSV integration. The final reports are under `build/grading/run-10952970798376735826`. Tests ran on the available JDK 25 with Maven's Java 17 compilation target; actual Java 17 runtime and Windows behavior have not been verified.
