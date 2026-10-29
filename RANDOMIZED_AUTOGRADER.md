# Randomized question mode

This is an optional second grading mode inside the existing application. The original CS102 lab-test mode remains the default: it still uses the fixed adapters and the existing feature 4 DynamicTestCaseProvider/DynamicRunner. The randomized question mode is selected with its own CLI command and does not replace those cases or change their scores.

The mode provides reusable question definitions that can generate student files and run seeded tests against instructor reference implementations. Q1 (is-even) and Q2 (array sum) remain small API examples. The current lab-test questions are also registered with their original method names and Java signatures:

| ID | Method signature | Behavior and inputs |
| --- | --- | --- |
| Q1a | `ArrayList<String> getIsogramWords(ArrayList<String>)` | Preserve input order and duplicates; keep words without repeated letters, ignoring case and non-letters |
| Q1b | `int getSumOfEvenIntegers(ArrayList<Object>)` | Sum even `Integer` values and ignore values of other types |
| Q2a | `double getAverageAge(String, String)` | Read the named people file and average matching first or last name tokens; return `0` for no matches and `-1` when the file is unavailable |
| Q2b | `String getTopStudent(String, String)` | Read the named student file and return the highest-GPA matching student as `Name-GPA`; throw `DataException` if the file/course has no matching record |
| Q3 | `List<Shape> sortShapes(List<Shape>)` | Keep shapes with area at most 1000 and sort by descending area, then descending perimeter |

Generated resources include the lab's text fixtures, `DataException`, the `Shape` support types, and the `ShapeComparator.java` starter companion needed to compile the examples. Supplied `Project` files remain untouched. Q1 and Q2 proposal examples are still available under their own IDs.

## Commands

Generate a stub and example tester:

~~~text
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-resources Q1 config.properties
~~~

The output directory is randomized.resources.directory (default students/resources, relative to the configuration file). The command writes Q1.java and Q1Tester.java.

Grade a folder containing the student's Q1.java:

~~~text
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade Q1 students/submissions/alice config.properties 482910374651
~~~

Grade one question for every matching ZIP in a selected directory:

~~~text
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade-batch Q1a C:/courses/CS102/submissions config.properties 482910374651
~~~

The optional seed is shared by all archives in this batch. The directory argument selects where the ZIPs are stored; `archive.file.regex` from the configuration controls which filenames are processed. Archives are ordered by filename. Each is extracted using the existing bounded `SafeZipExtractor`; the batch then requires exactly one source file named for the selected question and copies it, plus required companion files, into a temporary staging directory before calling the existing `RandomizedGrader`. Extraction and staging files are removed after the batch.

Each archive gets an individual text result and the batch gets a `summary.csv` in a unique subdirectory under `randomized.results.directory`. Missing/ambiguous sources, corrupt ZIPs, extraction limit violations, and student compile/runtime failures are recorded as FAIL for that archive; later submissions still run. A completed batch returns success even if some archives fail, while a missing input directory or no matching archives returns an error. The shared seed makes each student face the same generated trials. To grade the whole lab, invoke the command separately for each desired question ID.

The seed is optional. If omitted, the app chooses and prints one. Use the same seed to reproduce the same generated inputs. The result goes to randomized.results.directory (default grading/results) and is named <submission-folder>-<class-name>.txt. A completed grading operation returns process exit code 0 even when the student receives FAIL; invalid configuration, missing files, or grader infrastructure errors return a nonzero exit code.

For a lab-test demo, generate all five question packs into the configured resource folder:

~~~powershell
@("Q1a", "Q1b", "Q2a", "Q2b", "Q3") | ForEach-Object {
    java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-resources $_ config.properties
}
~~~

Then put one student's answer in a separate submission folder and select a question ID, for example:

~~~text
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade Q2a students/submissions/alice config.properties 482910374651
java -jar target/auto-grading-system-1.0-SNAPSHOT.jar --randomized-grade Q3 students/submissions/alice config.properties 482910374651
~~~

The Q3 submission folder must include both `Q3.java` and `ShapeComparator.java`. The grader compiles the companion source with the answer and supplies the same Shape support API used by the generated tester.

The first result line is exactly PASS or FAIL. The remaining lines contain the pass count, seed, and up to the configured number of failing inputs. Compile errors, timeouts, crashes, and student exceptions are reported as failures with a reason.

## Student workflow

1. Put the generated question stub and its `*Tester.java` in the same folder. For Q3, keep the generated `ShapeComparator.java` and supplied shape support files alongside them.
2. Implement the declared public method without changing its class name, method name, parameter types, or return type.
3. Run `javac <Question>.java <Question>Tester.java` followed by `java <Question>Tester` to check the fixed examples.
4. Submit the implemented question source file. The tester is for practice; grading uses additional generated inputs.

## Adding an instructor question

1. Add a Question subclass under src/main/java/autograder/autotester/.
2. In its constructor, declare the class/method signature, description, parameters, and test cases. Each test case needs one generator per parameter.
3. Implement answerKey(List<Object> arguments) in the instructor subclass. It runs in the trusted application process and is never copied into student resources or the student JVM classpath.
4. Register the question in QuestionRegistry.createQuestions().
5. Generate the student resources, then grade a correct solution and a deliberately hard-coded or incorrect solution with known seeds.

Example:

~~~java
public final class Q1 extends Question {
    public Q1() {
        signature("Q1", "isEven", boolean.class);
        describe("Return true when the integer is even.");
        param("n", int.class);
        addTestCase("small", new RandomInt(-100, 100));
        addTestCase("edge", new RandomInt(-3, 3));
    }

    @Override
    public Object answerKey(List<Object> arguments) {
        return ((Integer) arguments.get(0)) % 2 == 0;
    }
}
~~~

Available built-in generators are RandomInt(minimum, maximum) and RandomIntArray(minimumLength, maximumLength, minimumValue, maximumValue). Questions can also generate serializable lists, including heterogeneous `ArrayList<Object>` values, and may include explicitly declared fixture files, support sources/classes, and companion sources. The runner supports int, boolean, long, double, String, int[], String[], `ArrayList`, and `List` parameters/returns, including boxed primitive values where applicable. Q3 shape descriptors are converted to Shape objects inside the student process and its returned shapes are compared by their recorded representation. The authoring API rejects mismatched generated values, invalid Java identifiers, duplicate test-case IDs, missing signatures, unsupported values, and questions without test cases before grading.

Generated testers use a few fixed examples per test case. Expected values are computed when the tester is generated; the reference implementation itself is not written to the tester. Grading generates the configured number of inputs for every case from one Random(seed), computes expected values in the parent process, invokes the student's public method in a separate JVM, and compares results with deep equality or a question's declared textual comparison. Array and list inputs are serialized to the child process; the answer key receives a separate copy in the parent, so student mutations cannot change the reference calculation.

## Configuration

These optional properties live in the same file passed to either command. Paths are resolved relative to that file.

| Property | Default | Purpose |
| --- | --- | --- |
| randomized.resources.directory | students/resources | Generated student stub/tester directory |
| randomized.results.directory | grading/results | Per-submission result directory and temporary workspace parent |
| randomized.java.command | java | Java executable for the child runner |
| randomized.javac.command | javac | Java compiler for student source |
| randomized.runs.per.case | 50 | Generated grading inputs for each declared case |
| randomized.timeout.seconds | 10 | Deadline for the complete student run; student compilation has a separate 60-second minimum deadline |
| randomized.max.failures.shown | 10 | Maximum detailed failures written to the result |
| randomized.examples.per.case | 2 | Distinct examples requested for each generated tester |
| randomized.max.output.bytes | 1048576 | Maximum captured compiler/runner output |

The values are range-checked. A grading run is limited to 10,000 trials total and an estimated 16 MiB of generated inputs. If a generator repeatedly produces duplicate examples, the tester contains the distinct examples available within a bounded number of attempts.

## Design and boundaries

| Component | Responsibility |
| --- | --- |
| Question | Holds one question's public signature, generic source types, parameter definitions, generators, artifacts, and answer-key contract |
| RandomInt, RandomIntArray | Produce values from the seeded Random supplied by the caller |
| ResourceGenerator | Writes the student stub and fixed-example tester |
| RandomizedGrader | Generates the seeded trials, compiles the submission, runs the child JVM, compares outputs, and writes structured result data |
| RandomizedBatchGrader | Finds configured ZIP files, safely extracts and stages one answer at a time, continues across per-archive failures, and writes per-archive reports plus a CSV summary |
| RandomizedStudentRunner.java | Standalone child-process entry point; it sees generated inputs, declared fixtures/support classes, the student's classes, and the runner |
| RandomizedConfig | Reads and validates the mode's external settings |
| QuestionRegistry | Maps question IDs to instructor-authored definitions |

For the lab demo, `Shape`, `Circle`, `Rectangle`, and `DataException` support sources are packaged under `src/main/resources/autograder/autotester/labtest/`. They reproduce the small public contracts used by the supplied lab artifacts; the supplied `Project` files and compiled classes are not rewritten.

Randomized results are intentionally separate from scorereport.csv. The single-submission and ZIP-batch commands report PASS/FAIL plus evidence but do not add randomized outcomes to the existing assessment's score sheet. The regular configured batch pipeline remains unchanged.

The child JVM protects the main application from ordinary student exceptions, System.exit, and non-terminating code by running separately with one deadline for the complete run. A loop in one call therefore times out the question's run as a whole. Temporary compilation and protocol files are removed after grading, and the submitted source is copied before compilation. This is not a security sandbox: student code has the grading user's file, network, and process permissions and may tamper with files or launch processes. Use only trusted course submissions unless the child process is additionally restricted by an external container or account policy.
