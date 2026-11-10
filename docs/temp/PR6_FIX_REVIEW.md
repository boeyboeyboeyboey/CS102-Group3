# PR 6 local integration fixes

## Compared versions and scope

Sera requested these fixes on 2026-10-16 before any push. The team repository's `main` was fetched at `c7554e807e96c3f5849c01ea8fe38e7bb0b1bea9`. PR 6's existing head was `e84c30db5d995f3e20679f6eb93dd1ff11fc8646`; it already contains that main commit. The clean local `sera-feature-3-boilerplate` branch was fast-forwarded from `cfbd811` to that existing head. No rebase, force-push or new merge commit was needed.

Only App report coordination, package-private starter-analysis handling, their regression tests and related documentation are changed in this follow-up. The existing modularisation and feature-1 implementation are retained.

## P1: duplicate grading declaration

The PR head declared the local `grading` variable twice. Remove the two-argument construction and retain `new GradingPipeline(config, execution, detector)`, so configured plagiarism detection remains connected. The pipeline constructors and the detector implementation are unchanged.

## P2: starter CSV failure skips later reports

`StarterAnalysis` is a package-private collaborator that assembles findings and attempts the existing starter CSV output. Its result carries the updated grading run and a report exit status. If writing fails, the in-memory findings are retained and the failure is reported with the destination and completed grading-output location.

`App.generateReports` passes that completed analysis to HTML, then attempts Markdown feedback, and combines report exit statuses. A starter CSV failure therefore returns nonzero without suppressing subsequent configured reports. The four pre-existing grading/similarity CSVs, grades, withholding and plagiarism findings are retained. Normal starter CSV column order and messages remain the same; no operational setting, public API or runtime dependency was added.

## Regression evidence

- An application regression creates a directory at `starter-analysis.csv`, causing a portable real write failure. It verifies nonzero status, a useful error, real HTML containing escaped starter findings, a real Markdown feedback file and preservation of score/case/submission/similarity CSV bytes
- A second scenario blocks the starter CSV and makes HTML fail, then verifies Markdown is still attempted and written
- Disabled starter analysis invokes no detector and writes no CSV
- Existing success-path analysis, grading and plagiarism tests remain in place

The longer report-failure test is kept as one cohesive integration scenario: arranging a blocked output, invoking the actual report coordinator, and inspecting independent downstream artifacts makes the failure contract reviewable without hiding the setup in fixture helpers. No public model or grading policy is changed.

## Verification and remaining acceptance

Focused tests passed 28/28 with zero failures/errors/skips. The complete clean build passed 123 tests with zero failures/errors/skips. The packaged sample exited 0 and wrote `build/grading/run-386097544236335647`: 120 case records, six exported decisions, unchanged scores 12,10,10,0,20,1, 11 unchanged-starter findings, the HTML dashboard and six Markdown files. Plagiarism detection remained enabled and wrote the header-only `similarity.csv` with zero pairs above the configured threshold for these samples. The existing grading integration tests separately cover actual similarity findings and detector failures.

All 67 supplied-file hashes matched the pre-fix snapshot. A direct source comparison against fetched main found no differences in the grading package, plagiarism package or public bonus interfaces. `git diff --check` passed. These checks verify compilation, the combined normal flow and the regression scenarios; human build/run acceptance and a real-browser check remain separate.

Portable commands from the repository root:

```powershell
mvn "-Dtest=AppTest,StarterCodeDetectorTest,BoilerplateConfigTest,GradingPipelineTest,NGramPlagiarismDetectorTest" test
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

Verification uses temporary Maven 3.9.9, the existing local dependency cache and Windows 11/Temurin 25.0.4 with a Java 17 compiler target. Personal cache paths are omitted. Logs are local under `build/sera-pr6-*.log`, outside version control. No actual Java 17 runtime or real-browser acceptance is claimed.

These fixes support the brief's pages 5-6 compilation/reporting and failure-isolation requirements and page 7 robustness/usability criteria. Feature 3 remains REVIEW pending Sera's own clean-build/run acceptance. Its whole-file similarity heuristic and unvalidated 98% threshold are unchanged. B1 remains outside this task; B2's portable fixture change was already present in the feature branch. Human teammate review and release declarations remain required. No commit, push, PR edit, teammate message or merge into team main is performed for these local fixes.
