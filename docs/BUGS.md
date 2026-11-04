# Known bugs

Updated 2026-10-14. This is the team's bug register: keep each issue's stable ID, status, evidence, affected area, contact and acceptance checks here. [CODE_REVIEW.md](CODE_REVIEW.md) retains the review context; [agents.md](../agents.md) remains the authoritative feature-ownership board. GitHub Issues can provide assignment/discussion later; add an issue URL here if one is created rather than maintaining conflicting statuses.

Both issues below predate the modularisation and remain unfixed. Documentation is approved; their fixes have not been approved. Contacts are coordination recommendations, not new work assignments or changes to feature ownership. Source locations below were checked at modularisation commit `1c19147`.

## Open issues

| ID | Issue | Priority | Status | Affected area | Contact / coordination |
| --- | --- | --- | --- | --- | --- |
| [B1](#b1-csv-filename-case-can-overwrite-diagnostics) | Case variants of reserved CSV names can replace diagnostics | P2 | Fixed 2026-10-24 (Kiya); awaiting review | Shared MVP grading/configuration; no specific bonus | Boey coordinates the shared-core fix; implementing human owner is unassigned |
| [B2](#b2-dashboard-test-uses-a-windows-invalid-filename) | Dashboard escaping fixture creates a Windows-invalid filename | P2 | Open; fix approval pending | Feature 2: offline HTML dashboard tests | Boey, the recorded feature-2 owner |

P2 means a normal-priority correctness/portability fix to schedule and verify. It does not mean every ordinary grading run fails.

## B1: CSV filename case can overwrite diagnostics

**Trigger and impact:** On a case-insensitive filesystem, configure `grading.report.file=Cases.csv` or `Submissions.csv`. The current guard rejects only the exact lowercase names `cases.csv` and `submissions.csv`, so those variants pass. Diagnostic CSVs are written first, then the score-sheet writer replaces the configured destination. When the variant aliases the diagnostic file, score-sheet content can replace the case or submission diagnostics. The supplied default `scorereport.csv` does not trigger this issue.

**Expected behaviour:** Reject every case variant of a reserved diagnostic filename before grading starts; preserve valid custom score-report filenames.

**Evidence and limits:** Source inspection establishes the validation gap and write order. A disposable local Python filesystem probe confirmed that `Cases.csv` aliases `cases.csv` on the tested filesystem and that writing through the variant replaces its contents. The probe directory was removed. No full Java grading reproduction or Windows reproduction was run; do not describe the probe as one.

**Relevant code:**

- [ExecutionConfig.java](../src/main/java/autograder/config/ExecutionConfig.java), lines 35-40: filename validation
- [GradingDiagnosticsWriter.java](../src/main/java/autograder/grading/GradingDiagnosticsWriter.java), lines 57-61: diagnostics first, then score export; this logic previously lived in `GradingPipeline.writeReports`
- [CsvScoreReportWriter.java](../src/main/java/autograder/report/CsvScoreReportWriter.java), lines 134-137: replacement of the configured destination

**Suggested fix and acceptance checks:** Reject reserved names case-insensitively in `ExecutionConfig.load`. Add focused configuration tests for case variants of both reserved names and an accepted custom filename. Use an otherwise valid disposable fixture with protected inputs/output paths to demonstrate the guard; preserve supplied settings and inputs. Changing direct constructor invariants is a separate API decision, not an automatic part of this fix.

**Who to contact:** Boey should coordinate this as shared-core work with the main integration agent and a human teammate reviewer. The board does not assign the core grading/configuration layer to another named member. It is not automatically William's feature 4 (dynamic test ingestion) or Gabriel's feature 5 (Markdown feedback). If another member implements it, agree their ownership and check overlapping shared-file work first. Suggested focused branch: `csv-report-filename-validation`.

**Resolution (2026-10-24, Kiya):** `ExecutionConfig` now checks `grading.report.file` against one list of reserved diagnostic filenames (`cases.csv`, `submissions.csv`, `similarity.csv`, `starter-analysis.csv`), ignoring case. `ExecutionConfigTest.rejectsEveryReservedReportFilenameCaseInsensitively` covers case variants of all four, and the existing acceptance test still allows distinct names such as `scores.csv` and `starter-analysis-results.csv`. Verified with `mvn clean verify`; no Windows run was performed.

## B2: Dashboard test uses a Windows-invalid filename

**Trigger and impact:** `OfflineHtmlReportGeneratorTest.linksQuestionScoresToGroupedEvidenceAndKeepsUnmatchedCasesVisible` uses `Q<&script>` to test escaping. Its question fixture also uses that ID in `id + "Tester.java"`, creating a filename containing characters Windows rejects. The test can fail with `InvalidPathException` before exercising the renderer. This is a test-fixture portability defect; no production dashboard defect was established.

**Expected behaviour:** Keep hostile question/case text in the report models and assertions, while giving the unrelated synthetic tester path a portable filename.

**Evidence and limits:** The merged upstream record reports Windows 11, Temurin 25.0.4 and Maven 3.9.16, with 80/81 tests passing and this test failing. Source inspection supports the reported cause. The modularisation's 86-test macOS run passed with the fixture unchanged. The agent did not run Windows verification; the macOS pass does not resolve the Windows failure.

**Relevant code:** [OfflineHtmlReportGeneratorTest.java](../src/test/java/autograder/report/html/OfflineHtmlReportGeneratorTest.java), line 175 (hostile display ID), lines 306-308 (tester-path fixture).

**Suggested fix and acceptance checks:** Decouple the synthetic tester basename from the display ID. Retain hostile IDs/output and the escaping, question-navigation and unmatched-evidence assertions. Run the affected test and obtain actual Windows confirmation when available; record any unavailable platform check honestly. The earlier CRLF tester fix is already merged and is a different issue.

**Who to contact:** Boey owns feature 2 and should coordinate this test fix. Request review from another human teammate. Suggested focused branch: `dashboard-test-portability`.

## Observations kept separate from bugs

[H1 in the code review](CODE_REVIEW.md#h1-conditional-multiline-anomaly-records) is conditional log hardening, not an established defect in the supplied intake flow. The feedback withholding-contract observation and existing execution/browser/runtime limitations also remain review notes. They are not additional confirmed bugs or automatically approved enhancements.

## Updating this register

When work begins, record the agreed human owner and branch/issue/PR link. After a fix, record the exact checks and their results. Mark an issue resolved only with the fix and required verification/acceptance recorded; distinguish a proposed fix, a passing local test and a merged PR. Follow the existing branch, review and human-acceptance workflow. Keep B1/B2 IDs when linking discussions or commits.
