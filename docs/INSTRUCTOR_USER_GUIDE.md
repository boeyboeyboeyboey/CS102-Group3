# Instructor guide: randomized lab-test mode

This guide explains how to generate student practice resources and use seeded randomized grading for the current CS102 lab test. The mode is optional; the original batch grader remains the normal way to process the supplied lab submissions and score sheet.

## What the current version supports

The randomized question definitions are built into the application registry. They cover the current lab-test questions:

| Question | Student method | Main behavior |
| --- | --- | --- |
| Q1a | `getIsogramWords(ArrayList<String>)` | Return words without repeated letters, ignoring case |
| Q1b | `getSumOfEvenIntegers(ArrayList<Object>)` | Sum even `Integer` values and ignore other types |
| Q2a | `getAverageAge(String, String)` | Average matching people-file ages; return `0` for no matches and `-1` for an unavailable file |
| Q2b | `getTopStudent(String, String)` | Return the highest-GPA student for a course; throw `DataException` when there is no matching data |
| Q3 | `sortShapes(List<Shape>)` | Keep shapes with area at most 1000 and sort by descending area, then perimeter |

The application does not discover arbitrary question definitions by scanning a directory. For these five registered questions, the instructor runs one resource-generation command per question; the guide below provides a loop that creates one `studentResources` folder containing all five packs.

## Prepare the instructor machine

Run these commands from the repository root after building the project:

~~~powershell
mvn clean verify
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --help
~~~

The randomized commands need both `java` and `javac` from a JDK. Their command names can be configured with `randomized.java.command` and `randomized.javac.command` in `config.properties`.

## Generate `studentResources`

In `config.properties`, set the output directory to the folder you want to send:

~~~properties
randomized.resources.directory=studentResources
~~~

Relative paths resolve from the directory containing `config.properties`. The default is `students/resources`. Choose a clean output folder: generation replaces files with the same names.

Run this PowerShell loop from the repository root:

~~~powershell
$questionIds = @("Q1a", "Q1b", "Q2a", "Q2b", "Q3")
foreach ($questionId in $questionIds) {
    java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-resources $questionId config.properties
    if ($LASTEXITCODE -ne 0) {
        throw "Resource generation failed for $questionId"
    }
}
~~~

The generated folder contains a default answer stub and a fixed-example tester for each question. It also contains the Q2 text fixtures, the `DataException.java` helper, and the `Shape`/`Circle`/`Rectangle` sources and `ShapeComparator.java` starter needed by the examples. The answer key is not included. You can send the folder directly or package it yourself:

~~~powershell
Compress-Archive -Path .\studentResources\* -DestinationPath .\studentResources.zip -Force
~~~

Students can compile a question and its tester in the resource folder, then run the tester. For example:

~~~powershell
javac Q1a.java Q1aTester.java
java Q1aTester
~~~

For Q2a and Q2b, keep the generated fixture text files beside the Java files. For Q3, compile `Shape.java`, `Circle.java`, `Rectangle.java`, `Q3.java`, `ShapeComparator.java`, and `Q3Tester.java` together. Students should submit their implemented question source; Q3 submissions also need `ShapeComparator.java`.

## Randomized grading for one student and question

Put the student's source files in one folder, then run one grading command for a selected question:

~~~powershell
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade Q1a students/submissions/alice config.properties 482910374651
~~~

The seed is optional. Save the printed seed to reproduce the same randomized inputs. The output report is written under `randomized.results.directory` (default `grading/results`), for example `alice-Q1a.txt`. Its first line is `PASS` or `FAIL`; the remaining lines show the pass count, seed, and a capped list of failures. A completed grading run exits successfully even when the student gets `FAIL`, so inspect the report instead of relying only on the process exit code.

By default, the grader generates 50 inputs for each declared test case. You can change this with `randomized.runs.per.case`; the total is capped at 10,000 trials per run. The timeout applies to the complete student run. Compilation has its own 60-second minimum deadline.

The randomized result is separate from `scorereport.csv`. Single-submission mode takes one extracted folder; batch mode below accepts a ZIP directory. Neither mode combines randomized results into the class score sheet.

## Randomized grading for many ZIP submissions

Use the batch command to grade one question across ZIPs in an instructor-selected folder:

~~~powershell
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade-batch Q1a C:/courses/CS102/submissions config.properties 482910374651
~~~

The seed is optional. If omitted, the application selects and prints one. The same seed is used for every archive in that batch, so students receive the same generated inputs for the selected question. The input directory is the second argument; the configured `archive.file.regex` determines which files count as submissions.

### What happens during a batch run

1. The application validates the question ID and checks that the ZIP folder exists and does not overlap the randomized results folder.
2. It lists matching ZIPs in name order, making the report sequence repeatable.
3. Each archive is unpacked into a temporary directory with the existing bounded safe extractor. Corrupt archives, unsafe paths, and configured size or entry-count limit violations become failures for that archive.
4. It searches the extracted files for exactly one `<Question>.java`, such as `Q1a.java`. Missing or duplicate matches become clear failures. Required companion files, including `ShapeComparator.java` for Q3, must be beside the answer source.
5. The existing randomized grader compiles and runs the answer in its child JVM using that question's generated trials and the shared seed.
6. It writes one text report per archive and a `summary.csv` in a new batch folder under `randomized.results.directory` (default `grading/results`), then removes temporary extracted and staged files.

The output will look similar to this:

~~~text
grading/results/randomized-batch-Q1a-482910374651-a1b2c3/
  0001-alice-Q1a.txt
  0002-bob-Q1a.txt
  summary.csv
~~~

The summary includes each archive name, report filename, PASS/FAIL status, passed and total trials, seed, and any error. A compile error or invalid ZIP marks that archive FAIL; processing continues for later archives. A completed batch exits successfully even if some submissions fail, so review the summary and individual reports. No matching ZIP files is a command error. Run the command once per question to randomized-grade a full lab set.

Randomized batch results remain separate from `scorereport.csv`; this command does not change the standard score-sheet workflow.

## Processing ZIP submissions in a batch

The original batch mode can process multiple student ZIP files from a directory configured in `input.directory`. For example, set an absolute path with forward slashes in `config.properties`:

~~~properties
input.directory=C:/courses/CS102/submissions
~~~

Then run the ordinary batch command:

~~~powershell
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar config.properties
~~~

The configured archive pattern defaults to ZIP files. The normal batch pipeline validates and extracts each archive, prepares the configured lab questions, runs the fixed and existing dynamic test cases, and writes the normal score and diagnostics reports. Keep the supplied assessment layout, roster, score sheet, and other configured paths available.

This is the original fixed/dynamic grading workflow. The randomized ZIP command above is separate and produces its own per-question reports.

## Results, boundaries, and safe use

- `PASS` means the selected question passed every generated trial for that seed. `FAIL` includes mismatches, student exceptions, expected exceptions that were not thrown, compile errors, crashes, and timeouts.
- A run evaluates one question. Run separate commands for Q1a, Q1b, Q2a, Q2b, and Q3 if you want a full randomized review of one student.
- Student code runs in a separate JVM with a timeout, but this is not a security sandbox. It has the grading account's filesystem, network, and process permissions. Use trusted submissions or add operating-system/container restrictions.
- The generated practice tester uses a few visible examples. Randomized grading uses additional generated inputs and the instructor answer key kept in the host application.
- The supplied `Project` files are not modified by resource generation or randomized grading.

See [the randomized autograder technical notes](../RANDOMIZED_AUTOGRADER.md) for the design and instructor question-authoring details. See [the repository README](../README.md) for the build steps and existing batch-grading reports.
