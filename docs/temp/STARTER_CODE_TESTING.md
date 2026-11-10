# Starter-code comparison (Sera, feature 3)

## Method comparison and visual report (2026-10-26 revision)

Sera approved this revision on branch `sera-method-comparison`. Feature 3 remains
REVIEW until her clean-build/run and display/interaction acceptance. The report
is generated automatically when `grading.boilerplate.enabled=true`; the existing
grading command needs no additional argument:

```powershell
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

For this Windows checkout, the available Maven fallback is:

```powershell
& "$env:TEMP\cs102-sera-tools\apache-maven-3.9.9\bin\mvn.cmd" "-Dmaven.repo.local=C:\Users\SU MYAT MYAT HTAY\.m2\repository" clean verify
```

Open the **Method comparison report** path printed at the end of the run:
`build/grading/run-*/starter-comparison/index.html`. This folder is separate from
the configurable feature-2 dashboard, so the two reports cannot share a filename.
The report embeds all CSS and JavaScript and works offline without a server.

1. Search by archive/student username, question filename or method signature.
2. Choose **Changed methods**, **Unchanged methods** or **Unavailable comparisons**.
3. Press **Expand visible** or open a method individually to see starter/student
   source with original line numbers. **Collapse all** closes method details.
4. Read the method status and token-edit explanation. Red `−` and green `+`
   lines show original text changes, including formatting. Code status ignores
   comments/formatting; a reformatted method can therefore remain UNCHANGED.

Method matching uses enclosing top-level/member-class path, method name and
parameter types, independently of declaration order. Explicit constructors are
included. Overloads and array/varargs parameters are distinguished. A renamed
method, changed parameter type, or changed containing class is reported as a
removal plus addition; rename inference and type-resolution equivalence are not
claimed. Parameter-name or return-type edits are declaration modifications.

The parser uses the JDK compiler tree API in parse-only Java 17 mode with
annotation processing disabled. It does not compile, resolve dependencies, run
student code or invoke external libraries. A full JDK is required. Syntax errors,
ambiguous signatures, Unicode escapes, missing/unsafe files and configured
source/token limits produce an explicit unavailable entry; other files continue.
The in-memory source adapter specializes `SimpleJavaFileObject` to supply source
text to the parser, providing a concrete use of inheritance alongside the
composition used by the comparison and reporting collaborators.

Each matched method is UNCHANGED or MODIFIED according to its declaration/body
code tokens. Explanations identify unchanged versus changed body tokens and
count token insertion, deletion and replacement along one minimum-edit alignment.
An unchanged body is not a claim of correct implementation. Fields, imports,
initializers, implicit constructors and independent entries for local/anonymous
classes are outside method comparison. Such source still participates in the
existing whole-file analysis; local-class text inside a method remains in that
method's body comparison. Files without explicit methods have a separate notice.

Detailed token alignment is capped at 1,000,000 matrix cells per method; above
that budget the MODIFIED status remains but counts are explicitly unavailable.
Original-line alignment is capped at 250,000 cells; above that budget complete
method source is displayed without change highlighting and with an explicit
notice. These bounds supplement the existing source-byte/token limits.

The original `starter-analysis.csv` schema and whole-file warning threshold are
unchanged. The method report includes all compared methods regardless of the
whole-file warning threshold. It neither changes grades nor export eligibility.
Both CSV and method-report writes are attempted independently; a write failure
returns nonzero while subsequent feature-2 HTML and feature-5 Markdown reports
are still attempted. Existing comparison output is never overwritten.

Focused verification command:

```powershell
mvn "-Dtest=MethodComparisonTest,StarterComparisonReportTest,StarterCodeDetectorTest,BoilerplateConfigTest,AppTest" test
```

`ComparisonSourceReader` shares the original bounded UTF-8/path checks;
`JavaMethods` extracts source ranges; `MethodComparison` matches methods and
explains token edits; `MethodDiffHtml` aligns/escapes original lines;
`StarterComparisonReport` streams the completed comparison report. The small
`StarterAnalysis` hook coordinates output failures. Shared public domain models,
bonus interfaces, grading policy and other bonus implementations are unchanged.
These focused responsibilities and use of composition support the brief's page 6
code/design criteria; explicit failures, batch operation and review controls
support page 7 robustness/usability criteria. Human method attribution and the
presentation AI Use appendix remain the team's release obligations.

### Verification evidence for the method revision

- Clean Maven verification passed 139 tests with zero failures/errors/skips on
  Windows, Temurin JDK 25 and Maven 3.9.9, targeting Java 17. After the final
  line-wrapping-only cleanup, the focused command passed 33 tests with zero
  failures/errors/skips. An actual Java 17 runtime was not tested.
- Packaged sample exited 0 at `build/grading/run-7730138310631506160`, retaining
  totals 12,10,10,0,20,1 and 120 case records. It wrote all existing reports,
  11 whole-file starter findings, six Markdown files, and the new method report.
  The report contains 22 MODIFIED, 38 UNCHANGED and 4 ADDED methods, plus three
  NO_METHODS entries. All 67 supplied Project file hashes match the initial
  snapshot, including file-set membership.
- Headless Chrome loaded the final report directly from its local file path.
  Eight interaction checks covered initial visibility, changed/unchanged filters,
  expand/collapse, search/empty state and unavailable filtering. No JavaScript
  errors or external HTTP(S) report requests were observed. Desktop 1440x1000
  and narrow 390x844 screenshots were inspected; the narrow page does not
  overflow horizontally (the comparison table scrolls within its container).
  These are automated browser checks, not Sera's human acceptance.
- Local evidence commands were `node build/sera-method-browser-check.cjs
  build/grading/run-7730138310631506160/starter-comparison/index.html` and
  `python build/sera-method-evidence.py build/grading/run-7730138310631506160`.
  These helpers/screenshots/logs are ignored local build artifacts. Browser
  screenshots are `build/sera-method-report.png` and
  `build/sera-method-report-narrow.png`.
- Responsibility review: all new production classes are below 300 physical
  lines and new methods are at most 40 nonblank/noncomment lines. No size
  exception is required. `git diff --check` passed. Existing shared contracts,
  pipeline, other bonus implementations and dependencies were preserved.

Please run the clean build and packaged command above, then open the report and
confirm the method explanations, filters and side-by-side differences are useful.
Feature 3 remains REVIEW. No commit, push or PR was made for this revision,
honoring Sera's existing local-review restriction.

The implementation compares configured answer files and student-written companions against the starter directory. It reports warnings in the terminal, `starter-analysis.csv` and the HTML dashboard's **Analysis findings** section. It does not change scores, preparation status or export eligibility.

On the PR #6 follow-up, a failed starter CSV write returns a nonzero process status while keeping in-memory findings available to HTML and still attempting Markdown feedback. The completed grading and similarity CSVs are preserved. `AppTest` blocks the CSV destination with a directory to verify this failure path portably, and also tests combined starter/HTML failures. Normal output still uses the same starter CSV schema.

## Build and run

Run from the repository root:

```powershell
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

If Maven is not on PATH, the temporary installation used for this checkout can be invoked with:

```powershell
& "$env:TEMP\cs102-sera-tools\apache-maven-3.9.9\bin\mvn.cmd" clean verify
```

Wait for BUILD SUCCESS before running the JAR. Read the output directory printed by this run, then open its `dashboard.html` and scroll to **Analysis findings**. Inspect `starter-analysis.csv` for archive, source file, finding code and explanation. Expected sample scores remain Chee 12, David 10, Fen 10, Jing 0, Ping 20 and Xing 1.

## Focused behaviour checks

```powershell
mvn "-Dtest=StarterCodeDetectorTest,BoilerplateConfigTest,AppTest" test
```

Substitute the temporary Maven command above for `mvn` if needed. Tests create temporary synthetic files; do not edit the supplied files under `Project` to test this feature.

The tests cover:

- An identical starter or changes only to comments, identity headers and formatting produce `UNCHANGED_STARTER`
- A small token edit produces `MINIMAL_STARTER_CHANGES` when it meets the configured threshold; substantial implementation changes do not
- String/character literals, text blocks and multi-character operators retain meaningful differences
- Missing files, malformed/unsupported syntax, invalid UTF-8 and resource-limit failures produce `STARTER_COMPARISON_UNAVAILABLE`; other files/submissions continue
- Configured student companions are compared; unrelated support files are excluded
- Disabled analysis does not run; findings reach escaped HTML while grades, withholding and the three existing CSVs are preserved

## Configuration and limitations

`grading.boilerplate.enabled=true` enables the feature; older configurations without this key default to disabled. `grading.boilerplate.starter.directory` resolves relative to the configuration file. The default similarity threshold is 0.98; valid values are greater than zero and at most one. Byte and token limits bound reading and comparison work.

Similarity is `1 - token edit distance / max(starter token count, answer token count)`. Insertion, deletion and substitution each cost one. Comparisons ignore comments and whitespace outside literals, but preserve names, literals and operator boundaries. This is a conservative lexical comparison, not a complete Java parser: Unicode escape syntax requires manual comparison. Whole-file similarity can be high because of supplied demo code; warnings require human review and do not establish correctness or misconduct. A low similarity does not prove a complete answer.

The implementation uses the unchanged `BoilerplateDetector` interface and immutable shared findings; a separate package owns comparison logic and external properties own operational settings. These support the brief's page 6 modularity/configuration and anomaly-warning criteria. Edge-case tests and the CLI/report demonstration support page 7 robustness/usability criteria. No runtime library was added. Human review, clean-build acceptance, submission documentation and AI declarations remain team responsibilities.

## Overview and View why revision (2026-10-28)

Sera approved this scope in chat. The report now starts with one overview row per
submission/file, method-status counts, comparison availability and a View why
link. Counts are not whole-file similarity percentages. Each link uses a unique
ordinal ID, jumps to its file, clears detail filters, expands the matching
methods and focuses the file heading. The overview stays visible during detail
filtering. Without JavaScript, native links still reach the file; users expand
methods manually. Unavailable rows retain their reason, and no-method rows do
not imply unchanged files.

Implementation stays within feature 3. ComparisonOverview owns overview markup;
StarterComparisonReport streams detail HTML through a temporary file (removed
in finally) so sources are parsed only once without retaining full-batch diffs.
Existing comparison limits, grading decisions, CSV schemas and other bonus
renderers remain unchanged. Current production files remain under 300 lines and
methods under 40 nonblank lines; no new size exception is needed.

Verification:

- Clean verification: 140 tests, zero failures/errors/skips, including 5 report
  tests. Checks cover all method statuses, unique targets across submissions,
  counts matching detail evidence, escaped labels/source, unavailable/no-method
  rows, input preservation, temporary-file cleanup and existing destinations.
- Headless Chrome on the sample: 36 unique overview links resolve to file
  sections; 15 browser assertions pass plus Enter-key activation and narrow
  viewport checks. Filtering, repeated navigation, expansion and heading focus
  passed, with no JavaScript errors or external report requests.
- Supplied Project hashes: all 67 preserved. Sample has 120 case records and
  six Markdown reports; scores remain 12,10,10,0,20,1.
- Restricted Maven/Chrome runs encountered cache access / connection problems;
  approved unrestricted verification succeeded. Maven used the existing cache.

Commands used for clean verification and sample generation:

```powershell
& "$env:TEMP\cs102-sera-tools\apache-maven-3.9.9\bin\mvn.cmd" '-Dmaven.repo.local=C:/Users/SU MYAT MYAT HTAY/.m2/repository' clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

For a normal configured Maven installation, use `mvn clean verify` then the
same Java command. Open the newly printed starter-comparison/index.html, select
a detail filter that hides a destination, and activate its overview View why
link by mouse and keyboard. Confirm the correct student/file is revealed and
its token edit explanation and source comparison are expanded. Human clean-build
and display/interaction acceptance remain pending; feature 3 stays REVIEW.
Changes remain local and uncommitted under Sera's publication restriction.
Human method attribution and the presentation AI Use appendix remain release
checklist items under the existing team policy (brief page 3).

Confirmed sample Java process exit: 0 (captured with Python subprocess to avoid
PowerShell treating expected stderr name warnings as command failure).
Fresh report: `build/grading/run-15601521570881370854/starter-comparison/index.html`.
Browser checks used the identical-code report in `build/grading/run-40253895348628908`.
