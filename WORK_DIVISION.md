# Bonus work division

Updated 2026-10-16 after PR #1 merged and PR #5 began integrating feature 1 with modularisation. The original proposals remain available in Git history. This document describes the current integration points; algorithms and implementation details still require an approved feature plan.

[agents.md](agents.md) is the authoritative ownership/status board. Start with the [README kickoff](README.md#start-here-teammate-agent-kickoff), confirm identity and coordinate claims before editing. The core MVP and feature 2 are DONE. Features 1 (Kiya), 3 (Sera), 4 (William), 5 (Gabriel), and 6 (Kai Cong) are in REVIEW pending their recorded human completion gates. See [the dashboard architecture](offline_html_dashboard_architecture.md).

| Feature | Owner | Goal | Existing contract in autograder.api |
| --- | --- | --- | --- |
| 1: Token-based plagiarism detection | Kiya | Compare normalized Java tokens and report similarity evidence without automatically accusing students | PlagiarismDetector.compare(submissions, assessment) returns similarity findings |
| 2: Offline HTML dashboard | Boey | Present exported-score summaries, filters and expandable diagnostics in a self-contained offline report | HtmlReportGenerator.generate(context, outputDirectory) returns a report path; context includes actual withholding reasons |
| 3: Starter-code comparison | Sera (locked) | Compare answers with starter files and report unchanged or minimally changed implementations | BoilerplateDetector.analyze(submission, starterDirectory) returns analysis findings |
| 4: Parameterized test ingestion | William (locked) | Load additional varied inputs and expected outcomes through an approved case format | TestCaseProvider.load(assessment) returns case definitions |
| 5: Markdown student feedback | Gabriel | Explain scores and failure reasons in individual student reports | FeedbackGenerator.generate(grade, outputDirectory) returns a report path |
| 6: ANSI terminal dashboard | Kai Cong | Display grading progress with a readable plain-text fallback | ProgressListener.onProgress(stage, item, completed, total) |

Kiya owns feature 1; its PR #1 is merged, while the combined PR #5 integration checks remain pending. Boey owns feature 2. Gabriel owns feature 5. Sera and William retain locked ownership of features 3 and 4. Kai Cong confirmed feature 6 ownership on 2026-10-25. Coordinate any reassignment rather than overwriting a teammate's claim.

## Implementation boundaries

- Every member implements each feature in their own feature branch, starting new branches from current `main`; coordinate shared changes even across separate branches
- Use descriptive new branch names without a `codex/` prefix and follow [docs/CODING_STYLE.md](docs/CODING_STYLE.md). Shared-code refactoring requires a concrete approved plan; [docs/CODE_REVIEW.md](docs/CODE_REVIEW.md) records recommendations, not implementation approval
- After approved implementation and verification, commit/push only the reviewed feature files and create/update a PR from that branch into `main`. Use a draft for pending checks/human acceptance, tell the driver the PR URL and ask them to contact the group for review. Leave merging for explicit authorization after teammate review and required checks; see [the team PR workflow](docs/TEAM_WORKFLOW.md#feature-branches-and-pull-requests)
- Use the existing contracts under src/main/java/autograder/api and shared models under autograder.model; do not recreate the older proposed interfaces or restore the legacy src/autograder tree
- Give each bonus its own implementation package after the plan/interfaces are approved; coordinate shared configuration, models and GradingPipeline integration with the main agent
- Token normalization, n-grams/winnowing, starter normalization and configurable similarity/edit thresholds are candidate approaches, not already implemented requirements
- HTML must work offline and escape student-controlled text; feedback should use recorded outcomes and avoid inventing expected/actual values absent from the case data
- Parameterized cases need an explicit plan for runner generation, compiler integration, result validation and mark weights; TestCaseDefinition alone does not provide these. Preserve original testers and the explicit result-file protocol; do not award marks from printed score markers
- Terminal rendering should tolerate unsupported terminals and display failures. Richer stage events require coordinated pipeline changes; the current ProgressListener is intentionally small
- Externalize operational settings, preserve supplied inputs and review/withholding policy, and consume shared results rather than rerunning grading for reports
- Obtain approval before substantial implementation. Move implemented work to REVIEW, provide exact build/run commands, and wait for human clean-build confirmation before DONE
- When a bonus becomes DONE, create its required architecture document with actual OOP principles and slide talking points

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) and [docs/TEAM_WORKFLOW.md](docs/TEAM_WORKFLOW.md) for the current design, acceptance criteria and human-owned submission responsibilities.

## Feature 4 status (2026-10-18): REVIEW

William's feature 4 was broken in its earlier commit and is now fixed and in REVIEW. It no longer relies on answer-key comparison (which would require the team to act as an oracle — incompatible with "only Project/ is supplied"). It uses property-based / metamorphic testing: a default-package `DynamicRunner` compiled alongside student code asserts per-question invariants (Q1a isogram filter, Q1b even-sum, Q2a mean-age for probed surnames, Q2b top-student for probed courses, Q3 area-descending permutation) over seeded random inputs. `DynamicTestCaseProvider` emits 5 seeded dynamic cases per question; `StudentCompiler` compiles `DynamicRunner.java` into the student classes directory. Verified by `GradingPipelineTest` (PASS, 45 cases) and a standalone harness (correct → PASSED, hard-coded → FAILED). Pending human clean-build confirmation before DONE.
