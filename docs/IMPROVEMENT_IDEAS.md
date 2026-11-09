# Improvement ideas

Proposed by Boey on 2026-10-26. They come from the instructor feedback: "extra features
that improve the user experience" and "ensure minimal amount of work required by
instructors to use this autograder". Each idea is judged from the point of view of an
instructor marking hundreds of scripts. The goal is less manual work and fewer grading
errors.

These are proposals only. Claim one on the board in `agents.md` and get its plan approved
before implementing it. None of them changes the six numbered bonus features.

| # | Idea | What it does | Why it helps instructors | Status |
| --- | --- | --- | --- | --- |
| 1 | Mistake clustering | Groups identical failures across students (same case, same error/output signature) and lets the instructor award partial credit once per group, with a decision log | Turns hundreds of individual judgements into a handful; brief p6 lists a common-mistakes report as a bonus idea | Idea |
| 2 | Hard-coding detector | Flags students who pass the fixed tests but fail the seeded dynamic tests for the same question | Catches printed or hard-coded answers that similarity checks miss; uses data the grader already produces | Idea |
| 3 | Grade the grader | Before grading, checks that a model answer scores full marks and the untouched starter code scores about zero | One broken tester or config mistake mis-marks every student; this stops the run first | Idea |
| 4 | Review queue | One screen listing only submissions that need a human: withheld, name warnings, timeouts, all-compile-error, near grade boundaries, similarity flags; one-click decisions | The instructor reviews about 20 scripts instead of scanning hundreds of rows | Idea |
| 5 | Persistent overrides | Stores manual decisions in a sidecar file, reapplies them on every re-run, and shows a "what changed since last run" diff | Re-running after a tester fix no longer wipes manual corrections | Idea |
| 6 | Parallel grading | Grades several students at once on a bounded worker pool, about 5–8× faster | A batch of hundreds drops from hours to minutes; late submissions are cheap to add | In progress (Boey, 2026-10-26) |
| 7 | Compile rescue | For trivial compile failures (wrong class/file name, stray package line, missing import), grades a fixed copy separately and reports "would score X with a one-line fix"; the instructor decides whether to award it | Compile errors are the main source of regrade appeals; extends the brief's auto-correct requirement (p4) | Idea |
| 8 | Student pre-check tool | A small checker students run before submitting: ZIP structure, file names, name/email header, compiles | Fewer malformed submissions means fewer identity warnings and withheld rows | Idea |
| 9 | Score sanity checks | Flags suspicious patterns per question or case: nearly everyone scores zero, everyone fails one case the same way, clustered timeouts, large fixed-vs-dynamic pass-rate gaps | Catches mistakes in the assessment itself, not just in student answers | In progress (Boey, 2026-10-26) |
| 10 | Learning-platform import/export | Reads submissions and roster from the platform's bulk download; writes its gradebook import and bulk feedback files | Removes manual renaming and copy-paste, where transcription errors happen; needs a real export sample first | Idea |

## Suggested priorities

- Highest value for the effort: 1, 2 and 3.
- 5 should come before 1 and 4, because both need stored decisions.
- 6 changes shared grading code, so coordinate with the team before merging.
- 10 needs a real export sample from the school's learning platform before any design.

## Constraints that apply to every idea

- Java only, Maven allowed, free/open-source libraries only (brief p2–3).
- The project must keep compiling and running: a non-compiling source can cost up to 50% (brief p1).
- Do not change scoring, withholding, CSV schemas or supplied inputs without explicit approval.
- Every member must be able to explain the code. AI use must be declared by the human team (method-level attribution and the AI Use appendix).
