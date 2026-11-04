# Entity-relationship diagram

The auto-grader has no database. Its "entities" are the data objects in `src/main/java/autograder/model` (plus `GradingRun` in `grading`), which every stage passes along. This diagram shows what each entity holds and how many of one relate to another.

Generated from the code on `main` at commit `4d25864`. A rendered copy is in [er-diagram.svg](er-diagram.svg).

## How to read it

| Notation | Meaning |
| --- | --- |
| `PK` | Identifier for that entity (unique within a run) |
| `FK` | Field that refers to another entity's identifier |
| Solid line | One object holds the other directly (a field or list) |
| Dashed line | Linked by a matching value, such as a question ID or archive path, not by an object reference |
| `\|\|` | Exactly one |
| `o\|` | Zero or one |
| `\|{` | One or more |
| `o{` | Zero or more |

## Diagram

```mermaid
erDiagram
    ASSESSMENT_DEFINITION {
        string id PK
        string name
    }
    QUESTION_DEFINITION {
        string id PK "e.g. Q2a"
        string folder "e.g. Q2"
        string sourceFile "e.g. Q2a.java"
        path testerFile
        int caseCount
        double pointsPerCase
        list companionSourceFiles
        list supportFiles
        list fixtures
    }
    TEST_CASE_DEFINITION {
        string id PK "e.g. Q2a-3 or Q2a-dynamic-1"
        string questionId FK
        string runnerClass
        list arguments
        double maximumScore
    }
    SCORE_SHEET {
        path sourcePath
        list headers
        map columns
    }
    STUDENT_RECORD {
        string id PK "OrgDefinedId"
        string username
        string firstName
        string lastName
        string email
    }
    SUBMISSION_RESULT {
        path archivePath PK "the student's ZIP"
        path extractionPath
        path studentRoot
        path workingRoot
        enum status "READY, REVIEW_REQUIRED, FAILED"
        boolean extracted
        list questionDirectories
        list anomalies
    }
    STUDENT_IDENTITY {
        string name
        string emailId
        string username
        string studentRecord FK "null when unresolved"
        string source
        list strongEvidence
    }
    SUBMISSION_CORRECTION {
        string type "layout or header"
        path relativePath
        string field
        string originalValue
        string correctedValue
        boolean headerAdded
    }
    STUDENT_GRADE {
        double totalScore "derived from case results"
    }
    TEST_CASE_RESULT {
        string testCase FK
        enum status "PASSED, FAILED, TIMED_OUT, COMPILE_ERROR, RUNTIME_ERROR, NOT_RUN"
        long durationMillis
        string standardOutput
        string standardError
        double score "derived: maximum if PASSED, else 0"
    }
    BATCH_RESULT {
        string assessment FK
    }
    ANALYSIS_FINDING {
        string code "e.g. UNCHANGED_STARTER"
        path sourceFile
        string message
    }
    SIMILARITY_FINDING {
        path firstArchive FK
        path secondArchive FK
        double similarity "0 to 1"
        list evidence
    }
    GRADING_RUN {
        path directory "build/grading/run-*"
        map withheld "submission to reason"
    }

    ASSESSMENT_DEFINITION ||--|{ QUESTION_DEFINITION : contains
    QUESTION_DEFINITION ||..|{ TEST_CASE_DEFINITION : "tested by"
    SCORE_SHEET ||--|{ STUDENT_RECORD : lists
    SUBMISSION_RESULT ||--|| STUDENT_IDENTITY : "identified as"
    STUDENT_IDENTITY }o--o| STUDENT_RECORD : "matched to"
    SUBMISSION_RESULT ||--o{ SUBMISSION_CORRECTION : "corrected by"
    STUDENT_GRADE ||--|| SUBMISSION_RESULT : grades
    STUDENT_GRADE ||--|{ TEST_CASE_RESULT : "made of"
    TEST_CASE_RESULT }o--|| TEST_CASE_DEFINITION : "outcome of"
    BATCH_RESULT }o--|| ASSESSMENT_DEFINITION : "graded against"
    BATCH_RESULT ||--|{ STUDENT_GRADE : contains
    BATCH_RESULT ||--o{ ANALYSIS_FINDING : "starter-code findings"
    BATCH_RESULT ||--o{ SIMILARITY_FINDING : "similarity findings"
    SIMILARITY_FINDING }o..|| SUBMISSION_RESULT : "first submission"
    SIMILARITY_FINDING }o..|| SUBMISSION_RESULT : "second submission"
    GRADING_RUN ||--|| BATCH_RESULT : produces
    GRADING_RUN |o..o{ SUBMISSION_RESULT : withholds
```

## Notes on the relationships

- **One submission, one grade.** Every prepared ZIP becomes one `SUBMISSION_RESULT`, and the pipeline creates exactly one `STUDENT_GRADE` for it, even when it is withheld. That grade always has one `TEST_CASE_RESULT` per test case definition (20 instructor cases plus the dynamic cases), using `NOT_RUN` when grading was blocked.
- **A student can be unmatched or matched twice.** `STUDENT_IDENTITY.studentRecord` is empty when ownership could not be resolved. Two ZIPs can also match the same `STUDENT_RECORD`. The pipeline detects that as a duplicate and withholds both rather than guessing.
- **Scores are derived, not stored.** A case's score is its maximum if it passed and zero otherwise. A student's total is the sum of their case scores. Nothing stores a total that could get out of step with the case results.
- **Findings never change marks.** `SIMILARITY_FINDING` (feature 1) and `ANALYSIS_FINDING` (feature 3) are attached to the batch for review only. A similarity finding names its two submissions by archive path.
- **Withheld is a property of the run.** `GRADING_RUN.withheld` maps a submission to the reason its score was not exported. `GradingReportContext` (used by the HTML dashboard) carries the same batch and withheld map, so it is not drawn separately.
- **Type names are simplified.** `path`, `list`, `map` and `enum` stand for Java's `Path`, `List`, `Map` and the `SubmissionStatus`/`TestStatus` enums.
