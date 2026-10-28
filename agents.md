# Agent Synchronization

## Session protocol

At the start of each new workflow, read this file, README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md, docs/TEAM_WORKFLOW.md, docs/CODING_STYLE.md and the project brief `CS102-Project-Auto Grading System.pdf`. Read-only context gathering and a concise project/bonus summary may precede identity confirmation. Show all six bonus features with their purpose, current owner, status and availability from this board, then ask:

"Who am I speaking with? Please choose your recorded team name: Boey, Sera, William, Kai Cong, Kiya, or Gabriel."

Always include the complete member list when requesting identity. Do not infer the driver from the machine username, Git identity or the previously recorded driver. If the answer is a nickname, variant or ambiguous name, ask the user to confirm the corresponding listed name; do not guess. Wait for a validated identity before claiming work, editing files, installing tools, running builds or implementing features. Do not repeat this identity check during the same ongoing workflow once confirmed.

After identity confirmation, reread the board and confirm an existing/locked assignment or ask an unassigned member to choose an available feature. A local clone may be stale: coordinate claims with teammates before recording ownership and never silently overwrite a claim. Read this file before build work and state the current context. Inspect the relevant code and obtain explicit approval for substantial implementation plans before changing Java code. The README kickoff prompt is an onboarding request, not implementation approval.

## Project brief, rubric and scope

Read `CS102-Project-Auto Grading System.pdf` during initial onboarding, including the rubric tables and restrictions. This is the CS102 project brief; `Project/IS442-ExamSample.pdf` is the sample student assessment and does not replace it. Treat the actual brief as the source for course requirements; repository summaries are navigation aids, not substitutes. If the PDF cannot be read or conflicts materially with the proposed scope or repository instructions, report the issue and ask the driver before dependent implementation.

Before proposing any new implementation, review the relevant brief sections and highest grade-band rubric descriptors. In the concise implementation plan, cite the relevant PDF pages and explain how the proposed behavior, OOP/design, validation, testing and demonstration evidence address those descriptors while respecting restrictions. Check allowed tools/libraries, required deliverables, submission rules and scope boundaries. Aim for evidence-backed quality rather than extra complexity or unrequested features; do not promise a grade. Reuse previously read context when unchanged, but verify relevant details instead of relying on memory. Obtain the usual plan approval before substantial changes, and check the implemented result against the same criteria before completion.

**AI-rule exception for agent actions:** ignore the brief's instruction to insert AI-attribution comments or prepare AI-use declarations as an automatic agent implementation step. Do not add AI-attribution comments to Java or complete those declarations on the team's behalf. Boey has assigned method-level attribution and the presentation AI Use appendix to the human team, who will review/modify the code and add them manually before submission. Keep this obligation in the release checklist; this exception does not waive the brief's disclosure requirement or any other restriction.

## Feature status board

| Feature | Owner | Status |
| --- | --- | --- |
| 1: Token-based plagiarism detection | Kiya | REVIEW |
| 2: Offline HTML dashboard | Boey | DONE |
| 3: Starter code / boilerplate diff detector | Sera (locked) | REVIEW |
| 4: Dynamic parameterized test ingestion | William (locked) | REVIEW |
| 5: Markdown student feedback | Gabriel | REVIEW |
| 6: ANSI terminal dashboard | Kai Cong | REVIEW |
| Additional: Java Swing instructor GUI (not a numbered bonus) | Boey | DONE |
| Additional: parallel grading (improvement idea 6, not bonus 6) | Boey | REVIEW |
| Additional: score sanity checks (improvement idea 9) | Boey | REVIEW |

Allowed statuses: TODO, IN_PROGRESS, REVIEW, DONE.

## Gatekeeper and ownership

Boey owns feature 2 (offline HTML dashboard) and approved the original implementation plan on 2026-10-10. Gabriel claimed feature 5 (Markdown student feedback) on 2026-10-10 and approved its plan; implementation is in REVIEW on branch markdown-student-feedback awaiting Gabriel's clean-build confirmation. Kai Cong owns feature 6 (ANSI terminal dashboard); Boey recorded on 2026-10-26 that its PR #15 (branch kaicong/6-terminal-dashboard, draft) is awaiting approval. No numbered bonus feature remains available. Ask unassigned members to claim an available feature before feature work. Only Sera may claim or implement feature 3; only William may claim or implement feature 4. No bonus implementation begins until the MVP is functional and tested.

Keep shared configuration, domain models, interfaces, Maven setup, and pipeline integration coordinated by the main agent. Give each bonus implementation its own package after the interfaces are approved. Record file ownership before concurrent edits. Agents must reread this board before claiming work; this file alone does not provide a concurrent locking mechanism.

## Feature branches and pull requests

All members must create and work on each feature in their own feature branch, never directly on `main`. Start new branches from the latest `origin/main` after checking and preserving local changes. Use short, descriptive names without the `codex/` prefix, for example `code-modularisation` or `kiya-feature-1-plagiarism`. Use one branch per feature or focused follow-up task; existing feature branches such as Boey's `offline-html-dashboard` may be reused. Branches isolate edits but do not replace ownership coordination for shared files.

After approved implementation and proportionate verification, commit and push only the feature's reviewed files to its branch and create a pull request with that branch as the head and `main` as the base. This workflow authorizes those publication steps for approved feature work; it does not authorize merging into `main`, force-pushing, discarding local work or publishing unrelated changes. Check for an existing PR first and update it rather than creating a duplicate. Use a draft PR when required verification or human acceptance is still pending.

Include the behavior change, scope, exact build/run commands, actual test evidence, limitations and remaining checks in the PR. Immediately tell the driver that the PR was created, provide its URL, and ask them to contact the group to review it. A teammate may use an AI assistant to help review, but a human teammate owns acceptance. Agents must not message teammates, request reviews from named accounts or merge the PR without explicit authorization. Address review feedback on the same branch and PR; resolve conflicts with the owner while preserving shared changes. See [docs/TEAM_WORKFLOW.md](docs/TEAM_WORKFLOW.md#feature-branches-and-pull-requests) for the full sequence.

## Completion gate

Move implemented features to REVIEW. Provide exact Maven commands for compilation and execution of the relevant feature, then explicitly request the user's confirmation of a clean build. Never mark DONE before that confirmation. On marking a bonus DONE, create <feature_name>_architecture.md with implementation details, actual OOP principles used, and Slide Deck Talking Points.

Opening a PR does not mark a feature DONE or merge it. Before merging, require the documented checks, human build/behavior acceptance and review by another teammate. Existing accepted work remains DONE while a separate enhancement can remain REVIEW. Recommend protecting `main` with a required approving review and required build checks when available; changing repository protection settings requires explicit authorization.

## Coding style and code review

Before generating or editing application code, read [docs/CODING_STYLE.md](docs/CODING_STYLE.md). Keep each class responsible for one coherent concern and each method focused on one logical operation. Production Java files above 300 total lines and methods/constructors above 40 nonblank, noncomment lines require a responsibility review, not an automatic split. Record justified exceptions in the PR or review report. Preserve existing public contracts unless an approved plan explicitly changes them; prefer composition and package-private implementation collaborators.

Use [docs/CODE_REVIEW.md](docs/CODE_REVIEW.md) for the merged-main review, proposed extraction boundaries, known evidence limits and separately listed correctness findings. A review recommendation is not implementation approval. Behaviour-preserving refactoring must retain grading, identity, input protection, CSV and offline-report semantics; newly discovered bugs require separate approval. Do not split supplied assessment files or introduce tooling/dependencies simply to enforce line counts.

Use [docs/BUGS.md](docs/BUGS.md) as the known-bug register before proposing fixes. It records B1 (shared grading/configuration; Boey coordinates, implementing human owner unassigned) and B2 (feature-2 dashboard fixture; Boey). Documentation does not assign another teammate or approve a fix; coordinate shared ownership and preserve the recorded evidence limits.

## Current Context


- Overview revision verification: clean Maven verify passed 140 tests (zero failures/errors/skips); sample Java exit 0, totals 12,10,10,0,20,1 and all 67 Project hashes preserved. Chrome passed 15 assertions plus Enter activation and narrow viewport checks for 36 unique overview links, with no script errors or external report requests. Commands, report paths and evidence limits are in docs/STARTER_CODE_TESTING.md. Sera acceptance remains pending; no commit/push performed.

- 2026-10-28: validated driver Sera approved the feature-3 comparison overview and View why navigation plan. Continuing on `sera-method-comparison`; this agent owns StarterComparisonReport, the package-private ComparisonOverview renderer, feature-3 browser resources, focused report tests and feature documentation. No concurrent agents are editing. Changes remain local and uncommitted; feature 3 stays REVIEW pending human clean-build and interaction acceptance.

- 2026-10-27: validated driver Boey approved the idea-6 worker-count follow-up on branch `parallel-workers-settings` (from origin/main c45a125). Adds `config.WorkerLimits` (logical processor probe, `auto` = min(4, processors), cap with stderr warning), `execution.workers=auto`, `ExecutionConfig.withWorkers`/`RunSettings.withWorkers`, an optional CLI `--workers <N|auto> <config>` (one-argument form unchanged; bad value prints usage, exit 1) and a GUI Workers dropdown (1..processors, preselected from config, never saved). Counts above the logical processor count are capped with a warning; above 32 still rejected; default stays 1. Agent verification: `mvn clean verify` 222 tests, zero failures/errors/skips; sample runs with no flag, `--workers 4` and `--workers 16` (capped to 10 with warning) all exit 0 with scorereport.csv byte-identical to main and other CSVs identical after dropping DurationMillis/paths; `--workers 64` exits 1. Project unchanged. Boey confirmed the clean build and checked the GUI dropdown on 2026-10-27; merged into main through PR #20 on 2026-10-27 as 42aa614 (merge commit; branch kept). Teammates approved outside GitHub; no review is recorded on the PR. Board status unchanged: parallel grading remains REVIEW

- 2026-10-26: validated driver Sera approved method-level starter comparison, evidence-based difference explanations and a separate offline visual report. Active branch `sera-method-comparison` starts from fetched `origin/main`. This agent owns feature-3 parsing/comparison/report classes and resources, the small StarterAnalysis hook, focused tests and feature documentation; no concurrent agents are editing. Other bonus implementations, grading policy and shared interfaces stay unchanged. Feature 3 remains REVIEW; keep this work local and uncommitted under Sera's existing publication restriction. Human clean-build and display/interaction acceptance are required.
- Method revision evidence: clean verify passed 139 tests (zero failures/errors/skips); final line-wrapping cleanup passed 33 focused tests. Packaged sample exited 0 at `build/grading/run-7730138310631506160`, retaining totals 12,10,10,0,20,1, 120 cases, all existing reports and all 67 supplied Project hashes. New offline report: `starter-comparison/index.html` in that run, with 22 modified, 38 unchanged and 4 added methods plus 3 no-method notices. Headless Chrome passed eight interaction checks and desktop/narrow viewport checks, with no JavaScript errors or external report requests. Sera's own clean build/run and visual acceptance remain pending. See `docs/STARTER_CODE_TESTING.md` for actual commands, scope limits and evidence; no publication performed.
- 2026-10-26: idea 9 (score sanity checks) implemented on `score-sanity-checks` (from `instructor-gui`). New `autograder.analysis.sanity` (SanityChecker, SanityFinding, SanityCheck, SanityCsvWriter), `config.SanityConfig` (optional `grading.sanity.*` keys, defaults when absent; config.properties unchanged) and package-private `SanityAnalysis`. Report order is starter analysis, sanity checks, HTML, feedback; a sanity failure returns nonzero but later reports still run. Advisory only: withheld submissions excluded, needs at least 5 graded; writes `sanity-checks.csv` (reserved name), console summary, GUI artifact button. Silent failures are not grouped as shared failures after the first sample run showed that rule flagged any hard case. Not included: dashboard panel and GUI headline count (RunOutcome would need a new public component). Agent verification after that fix: `mvn clean verify` 171 tests, zero failures/errors/skips; sample run exit 0 with unchanged scores (20, 11.5, 10.5, 9.875, 0, 0.5) and "Sanity checks: none" (header-only CSV); Project hashes unchanged. Findings on real data are covered by unit tests only. Worker-count limits/GUI dropdown for idea 6 are parked in `.local-handover/2026-10-26-parallel-grading-sanity-checks/PARALLEL_WORKERS_FOLLOWUP.md`. Review fixes added (crash no longer reads as clean, findings printed when the CSV fails, control characters stripped). Merged into main through PR #19 on 2026-10-27 after #18, at Boey's request without a teammate review; Boey's clean-build confirmation is pending
- 2026-10-26: validated driver Boey approved improvement ideas 6 (parallel grading) and 9 (score sanity checks) and the idea-6 plan; recorded Kai Cong as feature 6 owner (PR #15, draft, awaiting approval). Branch `parallel-grading` was created from `instructor-gui` (stacked on PR #16). Idea 6 adds optional `execution.workers` (1-32, default 1), package-private `grading.SubmissionScheduler` and a scheduler call in `GradingPipeline.grade()`; per-submission grading code is unchanged. Agent verification: `mvn clean verify` 164 tests, zero failures/errors/skips; the previous commit and the new jar with workers absent produced identical normalised stdout, stderr and all 577 run files (only DurationMillis/timings differ); three runs each at 2 and 4 workers matched exactly (270 case rows, no status changes), console line order varies with workers > 1. Sample wall-clock on 10 cores (4 performance): 43 s sequential, about 41 s at 2 and 35 s at 4 workers, limited by the slowest student. Project hashes unchanged. Committed as 9d78290 and merged into main through PR #18 on 2026-10-27 (after #16 merged), at Boey's request without a teammate review; Boey's clean-build confirmation is pending. Idea 9 continues on branch `score-sanity-checks`, also from `instructor-gui`

- 2026-10-16: Sera requested local fixes for PR #6's duplicate `grading` declaration and starter-analysis CSV failure stopping later reports, cross-checked against team main c7554e8. The clean local `sera-feature-3-boilerplate` branch was fast-forwarded from cfbd811 to its existing PR head e84c30d; no team-main merge, commit or push was performed by this agent
- PR #6 fix ownership: this agent owns App.java, package-private StarterAnalysis.java, AppTest.java and the focused resolution/testing documentation. No other agent is editing these files. Preserve the three-argument plagiarism-aware GradingPipeline constructor, shared APIs, other feature logic and supplied inputs
- Starter CSV write failure now retains in-memory findings, attempts HTML and Markdown reports and returns nonzero. Regression checks include a directory blocking the CSV destination and simultaneous starter/HTML failures. Focused verification passed 28 tests; clean verification passed 123 tests with zero failures/errors/skips. Packaged sample exited 0 at `build/grading/run-386097544236335647`, retaining scores 12,10,10,0,20,1, 120 cases, 11 starter findings, similarity.csv, HTML and six Markdown files. All 67 supplied-file hashes are unchanged; feature 3 remains REVIEW. Keep these fixes local and uncommitted for Sera's review before publication; see `docs/PR6_FIX_REVIEW.md`

- 2026-10-15: feature 3 implemented locally; clean Maven verification passed 91 tests with zero failures/errors/skips on Windows 11, Temurin 25.0.4, Java 17 compiler target and Maven 3.9.9. Fixed the existing dashboard test's Windows-invalid tester filename while preserving hostile question-ID data. Sera's human clean-build/run acceptance is pending, so feature 3 is REVIEW
- Feature 3 packaged sample run succeeded at `build/grading/run-12291164739131086006`: 11 unchanged-starter findings, 120 case records, HTML dashboard and six feedback files; scores remain 12,10,10,0,20,1. All 67 supplied-file hashes match the pre-implementation snapshot. No real-browser acceptance is claimed
- Sera explicitly requested no Git publication for this work; do not commit, push or open a PR. Testing instructions are in `docs/STARTER_CODE_TESTING.md`. Temporary Maven: `& "$env:TEMP\cs102-sera-tools\apache-maven-3.9.9\bin\mvn.cmd" clean verify`; run: `java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties`

- 2026-10-14: validated driver Sera for feature 3; approved the starter-comparison plan with “Yes I approve please implement”. Branch `codex/sera-feature-3-boilerplate` starts from fetched `origin/main` at 9d81dc6 (Sera's fork). Existing locked ownership is preserved
- Feature 3 file ownership: this agent owns `autograder.analysis.boilerplate`, `BoilerplateConfig`, their tests, the small App analysis hook/AppTest, configuration and feature documentation. No concurrent agents are editing
- Feature 3 compares configured answers and student-written companions only, preserves literals while ignoring comments/formatting, reports token edit evidence and unavailable comparisons, and never changes marks or withholding. Human clean-build/run acceptance and teammate review remain required
- Date: 2026-10-16 (Asia/Singapore); validated driver: Boey in this ongoing workflow
- Boey approved implementing the PR #5 merge conflict resolution, documenting it thoroughly, and preparing one replacement PR. The active branch is code-modularisation; PR #5 is its existing live draft. No identity reconfirmation or new plan approval is needed for this continuation.
- The modularisation revision is based on c769fa93fc25b28b2a8b1af6806949e72de97979; its branch head before integration was 08269bcd56286c067c97058ac6ffcd47100de36b. Main advanced to 268f4315e3f3de26d0deaaf82ecba4d5b3a52b9b when Kiya's feature-1 PR #1 merged on 2026-10-15. Git integration is paused in a normal no-commit merge with one reported conflict in GradingPipeline.java; no merge commit exists yet.
- Git agent owns merge/index/commit/push and PR publication. Java agent owns App.java, GradingPipeline.java, GradingDiagnosticsWriter.java, focused tests and builds. Documentation agent owns this context and the shared docs. Preserve strict file ownership.
- Feature-1 detector integration preserves the two-argument GradingPipeline constructor (delegating to a no-op detector) and the three-argument overload (accepting an injected PlagiarismDetector), compares once after grading against the original full submission list and assessment, and catches comparison I/O/runtime failures so grading continues with empty findings. Its CSV is written after submissions and before score export, with a header-only output when empty. The exact schema and formatting are in [the PR #5 merge record](docs/PR5_MERGE_RESOLUTION.md).
- Upstream App.java contains a duplicate grading-local declaration and an early return that makes HTML/Markdown reports unreachable. The Java integration will restore a single reachable configured grading/report flow to keep the merged application buildable.
- Historical modularisation evidence remains distinct: 86 tests passed; sample totals 12,10,10,0,20,1 with 120 cases, HTML and six Markdown files; 10 synthetic artifacts were byte-identical; 49 public API surfaces matched. These are not post-merge checks. Focused detector/integration tests, clean verification, sample run and input-preservation checks are pending.
- PR #5 remains a draft pending those checks, Boey's clean-build/behaviour acceptance and another teammate's review. No merge to main is authorized. Sera's open unmerged PR #6 is excluded and is not evidence that B2 is fixed. No optional bonus or bug fix is included.
- Preserve supplied inputs, current .DS_Store bytes and the single ignored BOEY_FEATURE2_HANDOFF.local.md; do not stage the handoff or recreate shared HANDOFF.md. .DS_Store differed between the pre-backup and merge retry without an intentional agent write; exclude it from exact preservation claims.

See [docs/PR5_MERGE_RESOLUTION.md](docs/PR5_MERGE_RESOLUTION.md) for the resolution scope, contracts, pending evidence and release gates.

### Historical PR creation context (before the accepted merge)

- Date: 2026-10-14 (Asia/Singapore); validated driver: Boey in this ongoing workflow
- Boey requested the branch/PR workflow above and creation of an `offline-html-dashboard` -> `main` PR for group review. Committing/pushing this workflow update and opening the PR are authorized; merging into `main` is not
- Before this update, the dashboard and heatmap were already published on `offline-html-dashboard` at 6efe86b. Remote main was checked at a8d10d7; it contains the earlier handoff cleanup/Windows test fix and a terminal-dashboard change followed by its revert. Neither dashboard nor heatmap is on main
- The original MVP/dashboard remain accepted DONE; the heatmap remains REVIEW pending its human clean-build and local display/interaction acceptance. The PR is to be opened as a draft with those remaining checks recorded
- Preserve local .DS_Store bytes and the single ignored BOEY_FEATURE2_HANDOFF.local.md; do not recreate shared HANDOFF.md. Publication does not authorize attention overview, score distribution, extra feature claims or teammate messages. Gabriel's reported feature-5 branch/claim remains unchecked

## Historical context through heatmap implementation (2026-10-12)

The authorization and Git snapshots below describe earlier sessions. The current context and branch/PR policy above supersede their publication restrictions; historical verification and human acceptance are retained.

- Heatmap revision: Boey approved the detailed plan on 2026-10-12 with “ok lets do it approved”; implementation is REVIEW on offline-html-dashboard, awaiting his clean-build and local display/interaction confirmation for this revision. The original MVP/dashboard remain accepted DONE. Attention overview and score distribution remain outside this approval
- Heatmap file ownership: main integrated pipeline assertions, shared documentation and final review; bounded Luna completed report/html/QuestionOutcome.java, OfflineHtmlReportGenerator.java, dashboard.css, dashboard.js and their focused tests/verification. No agents are actively editing. Shared models/configuration/pipeline contracts are unchanged; branch synchronization and publication remain unauthorized
- Heatmap verification: focused tests 14/14; clean verify 67 tests with zero failures/errors/skips; actual-script DOM API fixture 2/2 (not a real browser pass); sample run scores 12,10,10,0,20,1 with six submissions and 120 cases. All 30 question links resolve to unique headings and recorded case evidence is retained exactly once. Existing pipeline tests preserve all three CSVs byte-for-byte after rendering
- Heatmap report: build/grading/run-3551798997316566219/dashboard.html. All 67 Project file hashes and .gitignore match the new baseline; HANDOFF.md remains absent. .DS_Store changed from its baseline hash during verification without an intentional agent edit; current bytes were left untouched and its concurrent source is unknown
- Heatmap limits: no real-browser, Java 17 runtime or Windows verification; supported DOM fixture and numeric color checks passed. Local main's CRLF fixture fix remains intentionally absent from this branch. No commit/push/merge/fetch was performed; HEAD remains 820394e and staging is empty
- Heatmap review commands: mvn clean verify; java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties. Boey's available fallback is /tmp/apache-maven-3.9.9/bin/mvn -Dmaven.repo.local=/tmp/cs102-maven-repository clean verify. Next: Boey confirms the new revision's clean build/run and checks the displayed heatmap and question navigation
- Preserve the existing unstaged handoff-cleanup mirrors, deleted HANDOFF.md and unrelated .DS_Store; BOEY_FEATURE2_HANDOFF.local.md remains the single ignored local handoff. Gabriel's reported feature-5 branch/claim remains unchecked
- Feature 2: Boey approved the original six-step plan with “Ok lets implement the original plan”; optional heatmap, attention overview and score distribution are outside this implementation
- Active branch: offline-html-dashboard; no commit or push is authorized for feature implementation
- File ownership for feature 2: main integrated the reporting context/API, HTML configuration, App integration, their tests and shared documentation; renderer subagent completed autograder/report/html, autograder/html resources and renderer tests; integration subagent completed GradingPipelineTest HTML/CSV checks and targeted runtime verification; no agents are actively editing
- Keep the existing CSV and withholding policy; HTML is generated afterward from the completed run and authoritative withheld map
- Feature 2 completion gate satisfied: Boey supplied the clean build/run and accepted the displayed dashboard with “ok looks good” on 2026-10-10
- Feature 2 architecture document: offline_html_dashboard_architecture.md (implementation, actual OOP principles and Slide Deck Talking Points)
- Feature 2 verification: focused tests 15/15; clean verify 59 tests with zero failures/errors/skips; packaged grading run retained scores 12,10,10,0,20,1 and produced 120 case records plus dashboard.html
- Feature 2 report: build/grading/run-15042814915555924988/dashboard.html; all 64 files in /tmp/cs102-feature2-input-hashes.json remained unchanged; git diff --check passed
- Automated browser verification was unavailable: isolated Chrome aborted (SIGABRT), and CUA rejected file:// navigation under its browser security policy (http/https only; workarounds prohibited); Boey subsequently viewed and accepted the dashboard; no automated browser pass is claimed
- Boey provided BUILD SUCCESS on 2026-10-10: 59 tests with zero failures/errors/skips and all six expected sample scores; the feature 2 human clean-build/run gate is satisfied
- Boey's latest report: build/grading/run-1332949390012507172/dashboard.html
- Next: Boey reviews and commits the feature on offline-html-dashboard; a requested commit message does not authorize the agent to commit or push. Other bonus ownership and human release declarations remain as recorded

- Date: 2026-10-10 (Asia/Tokyo)
- Validated driver: Boey; identity reconfirmed in this continuation
- Approved work: Boey approved the remaining MVP plan with “Approved lets go”; the accepted MVP behavior and verification are summarized in README.md and the project docs
- Foundation: DONE; Boey previously confirmed 30 tests and successful preparation run
- Remaining MVP: DONE; Boey supplied BUILD SUCCESS, 48 tests with zero failures/errors/skips, and the expected six-submission grading output on 2026-10-10
- Implemented: five explicit per-case instructor adapters, independent compilation, bounded per-case child processes, score aggregation, CSV export and per-case/export-decision diagnostics
- Fixed adapters cover the supplied 20 cases; edited testers require reviewed adapters; original-content validation accepts line-ending differences
- Missing answers/companions are compilation failures for that part only; timeouts preserve other case marks
- Unresolved identities, duplicate submissions, unusable preparations and infrastructure errors withhold export; name warnings alone do not
- Withheld/absent rows retain their template numerator; submissions.csv records decisions and warnings
- Final verification: clean verify passed 48 tests, zero failures/errors; after final diagnostic/formatting fixes, affected execution tests passed 7/7 and the JAR was rebuilt
- Real run: six archives graded; 120 case records, 20 per submission; all exported numerators reconcile with case points
- Scores: Chee 12, David 10, Fen 10, Jing 0, Ping 20, Xing 1
- Xing: Q2a missing-file case passes; four other Q2a cases time out due to a non-advancing scanner loop; remaining cases fail
- groundtruth.txt remains internally inconsistent for Xing (components sum to 15, stated total 1); actual execution independently yields 1; references unchanged
- Preparation still reports 4 READY, 2 REVIEW_REQUIRED (David/Xing name warnings), 0 FAILED; warnings retained in reports
- All 67 supplied-file hashes from the new pre-implementation snapshot remain unchanged
- Reports: build/grading/run-10952970798376735826/{scorereport,cases,submissions}.csv
- No OS security sandbox is claimed; current-user privileges remain, descendant cleanup is best effort; stronger isolation must be external
- Verification used JDK 25 with Java 17 compiler target; actual Java 17 runtime and Windows were not tested
- Ownership: main integrated configuration, pipeline, CLI and docs; delegated adapters/provider/tests and process services/tests are complete; no agents actively editing
- Original ZIPs, instructor tests, starter files and source score sheet remain unchanged
- Do not add AI-attribution comments to Java; Boey retained that rule and humans own review/modification and brief-required declarations before submission
- Feature 2 is DONE after Boey's clean-build/run and dashboard acceptance; Gabriel owns feature 5 (REVIEW); Kiya owns feature 1 (REVIEW), Sera owns feature 3 (REVIEW) and William owns feature 4 (REVIEW); available feature 6
- Feature 2 architecture document is complete; no other per-bonus architecture document is due
- Maven is temporarily available at /tmp/apache-maven-3.9.9/bin/mvn with -Dmaven.repo.local=/tmp/cs102-maven-repository
- Exact commands: mvn clean verify; java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
- Fallback build command: /tmp/apache-maven-3.9.9/bin/mvn -Dmaven.repo.local=/tmp/cs102-maven-repository clean verify
- Next: onboard teammates using the README kickoff, confirm canonical identity and bonus ownership, then seek approval for the selected feature plan; do not repeat the accepted MVP build-confirmation gate
- Read README.md, docs/REQUIREMENTS.md, docs/ARCHITECTURE.md and docs/TEAM_WORKFLOW.md for detailed behavior and responsibilities
- Boey approved reconciling remote history and publishing the current code on 2026-10-10; pre-existing unrelated .DS_Store change preserved

- Documentation onboarding update approved by Boey: kickoff placed first in README; identity prompts list every group member; read-only context/summary may precede identity confirmation
- Boey-confirmed run reports: build/grading/run-9005748573100915500/{scorereport,cases,submissions}.csv

- Remote September work-division assignment preserved: Kiya owns feature 1; WORK_DIVISION.md now maps proposals to current contracts

### Earlier onboarding snapshot before dashboard implementation

The following three entries predate the accepted dashboard and approved heatmap; they do not describe the current feature status.

- Boey claimed feature 2 (offline HTML dashboard) on 2026-10-10; owner recorded, status TODO because implementation has not started
- Current authorized work is claim documentation and continuation handoff only; feature 2 implementation plan still needs explicit approval
- Gabriel (validated driver for this workflow) claimed feature 5 on 2026-10-10 after coordinating with Kai Cong, who is expected to claim feature 6; Gabriel approved the feature 5 plan: MarkdownFeedbackGenerator in autograder.report.feedback implementing the unchanged FeedbackGenerator contract, FeedbackConfig with grading.feedback.* settings, and a small App hook; App/config wiring must be reconciled with the unmerged offline-html-dashboard branch
- Feature 5 implemented on branch markdown-student-feedback: agent-side mvn clean verify passed 62 tests, zero failures/errors/skips; packaged run wrote 6 feedback files under build/grading/run-*/feedback whose totals match scorereport.csv (12, 10, 10, 0, 20, 1); awaiting Gabriel's own clean-build confirmation before DONE
- 2026-10-14: Gabriel had main (including the merged offline HTML dashboard) merged into markdown-student-feedback; App runs the HTML dashboard then feedback and returns nonzero if either fails; config keeps both grading.html.* and grading.feedback.*. Windows 11 (Temurin 25.0.4, Maven 3.9.16): 80 of 81 tests passed; OfflineHtmlReportGeneratorTest.linksQuestionScoresToGroupedEvidenceAndKeepsUnmatchedCasesVisible errors on Windows only (InvalidPathException for '<' in a test tester filename; dashboard test code identical to main, flagged for Boey). Packaged run wrote dashboard.html and 6 feedback files; totals match scorereport.csv (12, 10, 10, 0, 20, 1)
- Next chat: read this board, README.md and the project docs; confirm the driver using the full member list, inspect relevant reporting contracts and the PDF rubric, then propose a concise feature 2 plan for approval

- 2026-10-13: Kiya confirmed identity and approved the feature 1 plan (n-gram Jaccard similarity) with "yes run build"; branch kayra-ngram-similarity
- Feature 1 implemented in autograder.plagiarism (Token, LineRange, JavaTokenizer, NGramBuilder, SimilarityCalculator, NGramPlagiarismDetector) plus autograder.config.PlagiarismConfig; status REVIEW pending Kiya's clean-build confirmation
- Shared changes: GradingPipeline gained an optional PlagiarismDetector constructor and writes similarity.csv; App prints a similarity summary; ExecutionConfig rejects similarity.csv as the score filename; config.properties gained plagiarism.* settings
- Similarity is advisory: findings never change scores or export decisions; starter n-grams are excluded; detector failures are reported and skipped
- Agent verification: clean verify passed 70 tests (48 existing + 22 new), zero failures/errors/skips; real run unchanged scores 12, 10, 10, 0, 20, 1 and 0 pairs at threshold 0.80
- Sample similarity with threshold 0.01: highest Chee/David Q1a 0.663, then Chee/Fen Q2a 0.467; no sample pair is flagged at the default 0.80
- 2026-10-20: Kiya approved restoring maven-jar-plugin mainClass autograder.App in pom.xml (removed by feature 4 commit 0360801); clean verify passed 126 tests and java -jar --help starts again
- 2026-10-20: William chose to distribute each question's existing maximum across its fixed and dynamic cases. Implemented locally on `william-dynamic-test-weighting`: each question retains its 3,3,5,5,4 maximum and the total remains 20. Direct Java 17-target compilation, focused pipeline regression and a packaged six-submission run passed; 45 cases per student, all reports written, score totals within 20, and supplied Project hashes unchanged. Maven is unavailable in this environment, so clean Maven verification and William's human clean-build confirmation remain pending. Feature 4 stays REVIEW; no publication yet.
- 2026-10-20: William approved a focused Feature 4 follow-up on `william-dynamic-runner-fixes`. `DynamicRunner` now follows the Q2a first-or-last uppercase surname rule and Q3's area <= 1000 filter plus area/perimeter descending sort. The score-sheet numerator rounds half-up to two decimals; case/submission diagnostics retain exact values. Focused runner, score-writer and pipeline checks passed; all production sources compiled to Java 17; the six-submission sample run succeeded with Ping at 20.00 and all supplied Project hashes unchanged. Final artifacts are at `build/grading/run-15847751552065290169`. `mvn clean verify` is pending because Maven is unavailable. Clean-build confirmation, human review and PR creation remain pending; Feature 4 stays REVIEW. Detailed evidence: [Feature 4 summary](DYNAMIC_TEST_FEATURE_SUMMARY.md).
- 2026-10-24: Kiya approved adding JPlag beside the n-gram detector on Java 17; branch kiya-feature-1-jplag from main d98a0f7
- Shared changes: pom.xml (maven.compiler.release 17, JPlag 4.3.0 + slf4j-nop, shaded JAR so java -jar still works), SimilarityFinding (additive question/algorithm fields; old constructor kept), similarity.csv gains Question and Algorithm columns, App uses PlagiarismDetectorFactory, config.properties gains plagiarism.algorithms/jplag.threshold/jplag.min.tokens
- Agent verification: clean verify 138 tests on JDK 25 and on a real Temurin 17.0.21 runtime (first actual Java 17 verification); both runs graded the sample with unchanged scores and 0 findings at default thresholds
