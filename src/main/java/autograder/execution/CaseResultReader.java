package autograder.execution;

import autograder.model.TestCaseDefinition;
import autograder.model.TestCaseResult;
import autograder.model.TestStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class CaseResultReader {
    private static final int MAX_RESULT_BYTES = 16;

    private CaseResultReader() {
    }

    static TestCaseResult read(TestCaseDefinition test, ProcessResult process, Path resultFile)
            throws IOException {
        TestStatus status;
        String diagnostic = process.stderr();
        if (process.timedOut()) {
            status = TestStatus.TIMED_OUT;
            diagnostic = "case execution timed out\n" + diagnostic;
        } else if (process.exitCode() != 0) {
            status = TestStatus.RUNTIME_ERROR;
            diagnostic = "runner exited with status " + process.exitCode() + "\n" + diagnostic;
        } else if (!Files.isRegularFile(resultFile)) {
            status = TestStatus.RUNTIME_ERROR;
            diagnostic = "runner did not write a result file\n" + diagnostic;
        } else {
            long resultSize = Files.size(resultFile);
            if (resultSize > MAX_RESULT_BYTES) {
                status = TestStatus.RUNTIME_ERROR;
                diagnostic = "runner result file exceeded " + MAX_RESULT_BYTES + " bytes\n" + diagnostic;
            } else {
                String result = Files.readString(resultFile, StandardCharsets.UTF_8).trim();
                status = switch (result) {
                    case "PASSED" -> TestStatus.PASSED;
                    case "FAILED" -> TestStatus.FAILED;
                    case "ERROR" -> TestStatus.RUNTIME_ERROR;
                    default -> TestStatus.RUNTIME_ERROR;
                };
                if (result.equals("ERROR")) {
                    diagnostic = "runner reported an exception\n" + diagnostic;
                } else if (status == TestStatus.RUNTIME_ERROR) {
                    diagnostic = "runner wrote an invalid result token\n" + diagnostic;
                }
            }
        }
        return new TestCaseResult(test, status, process.durationMillis(),
                process.stdout(), diagnostic);
    }
}
