# Starter Code Comparison Architecture

Owner: Sera · Feature 3 · Updated: 2026-10-28

Status: **REVIEW**. Sera confirmed that the displayed report works. Her own
successful clean-build/run confirmation successful; 
This document covers the whole-file detector,
method comparison and overview navigation currently on `sera-method-comparison`.

## Purpose and scope

The feature helps an instructor inspect what a student changed from the supplied
starter code. It compares configured answer files and configured student-written
companion files against their corresponding starter files. It does not scan every
Java file in a submission.

There are two complementary outputs:

| Output | Question answered | Location within a grading run |
| --- | --- | --- |
| Whole-file findings | Is this file unchanged or almost unchanged from the starter? | `starter-analysis.csv`, CLI and the main dashboard's code analysis findings |
| Method comparison | Which methods changed, appeared or disappeared, and what differs? | `starter-comparison/index.html` |

Both are advisory. Neither changes scores or export eligibility. Student-to-student
plagiarism comparison belongs to feature 1; parameterized testing belongs to
feature 4. This feature does not determine who copied whom, detect hardcoding,
trace method calls or decide whether a solution is correct.

## Components and responsibilities

The implementation package is `autograder.analysis.boilerplate`. Internal
collaborators are package-private; public entry points remain small.

| Component | Responsibility |
| --- | --- |
| `BoilerplateDetector` | Shared interface for analyzing one submission against a starter directory |
| `BoilerplateConfig` | Validated immutable settings loaded from external properties |
| `StarterCodeDetector` | Select configured files and produce whole-file review findings |
| `ComparisonSourceReader` | Read bounded UTF-8 source with containment and file checks |
| `JavaSourceTokens` | Ignore comments/formatting outside literals, retain meaningful lexical differences and calculate whole-file token edit distance |
| `JavaMethods` | Use the JDK compiler's parse-only tree API to extract explicit methods and constructors, source ranges and tokens |
| `MethodComparison` | Match method keys, assign statuses and explain token changes |
| `MethodDiffHtml` | Escape text and render expandable side-by-side source comparisons |
| `ComparisonOverview` | Render per-submission/file counts, availability and evidence links |
| `StarterComparisonReport` | Coordinate generation of the self-contained method report |
| `StarterAnalysis` | Coordinate optional analysis after grading, attach findings and attempt both feature-3 outputs |
| `comparison-head.html` / `comparison.js` | Report styling, filters, expansion and View why navigation |

## Processing flow

```text
Completed grading run + assessment file definitions
                    |
              StarterAnalysis
              /             \
   BoilerplateDetector       StarterComparisonReport
   (StarterCodeDetector)     source reader -> JavaMethods -> MethodComparison
              |                         |
     whole-file findings       overview + detailed HTML
              |                         |
   starter-analysis.csv       starter-comparison/index.html
              |
   findings attached to a new BatchResult
              |
   existing HTML / Markdown report flow continues
```

Analysis uses prepared working copies and the configured starter directory.
It reads source; it does not modify supplied inputs, compile student classes for
comparison or execute student methods. The method parser runs with annotation
processing disabled and Java 17 source syntax; parsing is not type checking.

### Whole-file comparison

1. Build a distinct list of configured answer and companion paths.
2. Read each starter/student pair through `ComparisonSourceReader`.
3. Tokenize, ignoring comments and whitespace outside literals. Identifiers,
   literal contents and operator boundaries remain significant.
4. Calculate minimum token edit distance: insertion, deletion and replacement
   each cost one.
5. Calculate `similarity = 1 - edits / max(starterTokens, studentTokens)`.

| Finding | Meaning |
| --- | --- |
| `UNCHANGED_STARTER` | Token sequences are identical, giving 100% similarity |
| `MINIMAL_STARTER_CHANGES` | Tokens differ, but similarity meets the configured threshold |
| `STARTER_COMPARISON_UNAVAILABLE` | A comparison could not be completed; the reason is recorded |

Below-threshold results are not emitted as whole-file findings. They are not
converted to 0%. The method report still compares the configured files regardless
of this warning threshold. Unchanged supplied helper methods may be expected;
unchanged answer stubs need instructor review. The detector does not automatically
classify methods as required answer sections or protected helper code.

### Method matching and explanations

`JavaMethods` constructs keys from enclosing class, method name and parameter
types, for example `Q1a#getIsogramWords(ArrayList<String>)`. It includes explicit
constructors and methods in top-level and member classes. Parameter names are not
part of the key. Return types and declaration modifiers can change while the key
still matches; their token changes contribute to MODIFIED status.

| Status | Recorded evidence |
| --- | --- |
| `UNCHANGED` | Matching key and identical method tokens after ignoring comments/formatting |
| `MODIFIED` | Matching key with different tokens; explanation distinguishes declaration-only changes from body changes |
| `ADDED` | Key appears only in the student file |
| `REMOVED` | Key appears only in the starter file |

Renaming a method or changing its parameter types produces REMOVED and ADDED,
rather than a semantic rename inference. Adding a helper is visible but is not
automatically treated as a rule violation.

For modified methods, a dynamic-programming edit matrix supports backtracking to
count inserted, deleted and replaced tokens. The explanation explicitly calls
this **one minimum-edit alignment**: several equally small alignments may exist.
Counts are not correctness scores or percentages of student effort.

The source view separately aligns original lines using a longest-common-subsequence
matrix. Red lines are removed and green lines are added. This highlights original
text differences, including formatting; it can therefore show changes inside a
method whose normalized tokens are UNCHANGED. Original source line numbers are
retained.

### Overview and View why navigation

There is one overview row for each submission/file, with method-status counts,
comparison availability and a View why link. Counts come from the same comparison
results rendered in the details. Ordinal IDs distinguish identical filenames
across different submissions without embedding student text in fragment IDs.

Activating View why clears detail filters, reveals the matching section, expands
its methods, scrolls to it and focuses its heading. Repeat clicks and fragment
navigation are handled. The overview remains visible while detail filters change.
Without JavaScript, native fragment links still reach file sections; the user
opens the method details manually.

Detailed HTML is streamed through a temporary file, then copied after the compact
overview into the final report. This avoids parsing files again solely for counts
and avoids retaining the entire batch's source comparison HTML in memory. The
temporary file is deleted in a `finally` block.

## Actual OOP principles used

| Principle | Concrete implementation and benefit |
| --- | --- |
| Abstraction | `BoilerplateDetector` exposes an analysis contract without exposing tokenization or edit-distance details to `StarterAnalysis` |
| Interface polymorphism | `StarterAnalysis.analyze` receives a `BoilerplateDetector`; production uses `StarterCodeDetector`, while tests can supply alternate implementations to verify failure handling |
| Encapsulation | Package-private parser/comparison/rendering collaborators hide implementation details. `BoilerplateConfig` validates settings at construction; token lists are copied into unmodifiable lists |
| Single Responsibility Principle | Source reading, tokenization, method extraction, comparison, overview rendering and source-diff rendering have separate owners |
| Composition | Coordinators use these focused collaborators rather than inheriting parser, grading or rendering behavior |
| Separation of concerns | Comparison produces review evidence; grading retains scoring/export decisions, and report code presents the evidence |

The feature does not introduce a custom inheritance hierarchy. `StarterCodeDetector`
implements an interface; that should not be presented as class inheritance.
`JavaMethods` does use an anonymous subclass of JDK `SimpleJavaFileObject`,
overriding `getCharContent` to expose in-memory source to the parser. That is a
specific JDK integration use of inheritance, not the overall architecture.

## Configuration, limits and failure behavior

| Property | Default when absent |
| --- | --- |
| `grading.boilerplate.enabled` | `false` |
| `grading.boilerplate.starter.directory` | `Project/RenameToYourUsername` |
| `grading.boilerplate.similarity.threshold` | `0.98` |
| `grading.boilerplate.max.source.bytes` | `1048576` |
| `grading.boilerplate.max.tokens` | `4000` |

Relative starter paths resolve against the configuration file's directory.
Configuration rejects nonfinite/out-of-range thresholds and invalid limits.
Source reading checks normalized and real-path containment, rejects direct
symbolic-link files, limits bytes and tokens and rejects invalid UTF-8.

Whole-file edit distance uses two rows: O(m*n) time and O(n) auxiliary memory.
Detailed method edit explanations use a matrix capped at 1,000,000 cells; beyond
that, the report states that tokens differ but detailed edit counts exceed the
budget. Line alignment is capped at 250,000 cells; beyond that, complete methods
are displayed without change highlighting. These are explicit fallbacks.

Missing/unsafe files, unavailable working copies, parser errors and unsupported
input produce per-file unavailable entries, allowing other comparisons to continue.
Files with no explicit methods receive a separate notice, not a whole-file-match
claim. Unicode escape syntax is conservatively rejected for manual comparison.
Fields, initializers, imports, local/anonymous-class methods and implicit
constructors are outside the method comparison; whole-file analysis is separate.
Signature matching is syntactic, without symbol resolution or semantic equivalence.

CSV and method-report writes are attempted independently. A feature-3 write
failure returns a nonzero result while later HTML and Markdown reporting is still
attempted. Completed grading/CSV results are retained. Existing report destinations
are not overwritten. Student-controlled labels, messages and source are HTML
escaped; browser code uses DOM APIs rather than executing student content.

The application requires a full JDK for method parsing. It adds no third-party
runtime dependency; tests use the project's existing JUnit/Maven setup.

## Verification and acceptance

The recorded overview revision passed `clean verify`: **140 tests, zero
failures/errors/skips**. This is the whole project's test total, not 140 tests
exclusive to feature 3. Coverage includes normalized comparison, method statuses,
declaration/body distinctions, unavailable files, escaping, unique overview links,
count/detail consistency, resource bounds, output preservation and report failures.

The confirmed sample exited 0 with scores **12,10,10,0,20,1**, 120 case records
and six Markdown reports. All 67 supplied Project file hashes were preserved.
The method report contained 22 modified, 38 unchanged and 4 added methods, plus
3 no-method notices. The overview has 36 file rows; method counts and file counts
are different units.

Headless Chrome passed 15 assertions plus Enter-key activation and narrow viewport
checks. All 36 overview links had distinct destinations, filter-hidden destinations
were revealed, repeat navigation and focus worked, and no JavaScript errors or
external report requests were observed. These automated checks do not replace
human review of the assessment rules or final team acceptance.

Sera subsequently confirmed the displayed report works. Her clean-build command
initially failed because Maven was absent from PATH; a temporary-Maven command was
provided, but no successful human build/run result has yet been reported here.

See [the testing guide](docs/STARTER_CODE_TESTING.md) for commands, paths and full
evidence limits. Standard commands are:

```powershell
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
```

For Sera's existing temporary installation:

```powershell
& "$env:TEMP\cs102-sera-tools\apache-maven-3.9.9\bin\mvn.cmd" "-Dmaven.repo.local=C:/Users/SU MYAT MYAT HTAY/.m2/repository" clean verify
```

Human clean-build/run confirmation and teammate review remain release checks.
The team must complete method-level AI attribution and the presentation AI Use
appendix under its existing policy; this document does not complete those
declarations. The project brief's pages 3, 6 and 7 cover the relevant tooling,
disclosure, modularity, reporting and usability requirements.

## Slide Deck Talking Points

### Slide 1: Problem and purpose

- Instructors need to distinguish untouched starter files from student changes.
- We compare student working copies against the instructor's starter, without
  changing marks or modifying supplied inputs.
- This is different from comparing one student's work against another's.

### Slide 2: Design and OOP

- Show the processing flow above: coordinator, detector, parser/comparison and reports.
- Explain SRP using source reading versus comparison versus HTML rendering.
- Explain abstraction/polymorphism using `BoilerplateDetector` and injected test detectors.
- Explain why composition and package-private collaborators fit this feature.

### Slide 3: Algorithm and trade-offs

- Comments and formatting do not count as implementation changes; literals do.
- Token edit distance gives an explainable whole-file similarity measure.
- JDK parsing finds method boundaries more reliably than splitting Java with regex.
- Matching is syntactic; renamed methods appear as removed/added.
- Bounded matrices trade detailed alignment for an explicit fallback on large inputs.

### Slide 4: Live demonstration

1. Run the application once and open its newly printed method-report path.
2. Show the overview and explain that counts represent methods, not percentages.
3. Filter details, then use View why to reveal a matching student/file.
4. Expand a modified method and explain its token edit counts and source lines.
5. Show an unchanged method and an added helper. Use a separate synthetic fixture
   for REMOVED/unavailable behavior if needed; do not edit supplied assessment files.

### Slide 5: Evidence and limitations

- Report the actual 140-test project verification and browser navigation checks.
- Show unchanged sample scores and preserved supplied inputs.
- An unchanged helper can be legitimate; an unchanged stub may be incomplete.
- Similarity and changed-method counts do not prove correctness, effort or copying.
- State the remaining human build/review checks accurately before submission.

### Questions to rehearse

| Question | Answer to explain |
| --- | --- |
| Why does a 100% starter match appear? | Code tokens match after ignoring comments/formatting; inspect whether it is an answer stub or supplied helper |
| Where did intermediate percentages go? | The whole-file detector emits only results at or above its warning threshold; omitted results are not 0% |
| Can students add helper methods? | The report marks them ADDED; permissibility depends on assessment instructions |
| Does a green line mean a correct answer? | No; it is added source text, not a passed test |
| What if comparison fails? | Record an unavailable reason, continue other comparisons and preserve grading results |
| Does this detect hardcoding? | No; testing with different inputs is a separate responsibility |
