package ai4se.harness.feedback;

import java.util.Map;

public class CorrectionSuggester {
    private static final Map<FailureType, String> TEMPLATES = Map.of(
        FailureType.COMPILE_ERROR, "Fix the compilation error based on the error output: %s",
        FailureType.TEST_FAILURE, "Fix the test failure based on the test output: %s",
        FailureType.RUNTIME_ERROR, "Fix the runtime error based on the exception: %s",
        FailureType.COMMAND_REJECTED, "Command was blocked by guardrail. Use a safer alternative: %s",
        FailureType.FILE_NOT_FOUND, "File not found. Check the file path: %s",
        FailureType.PERMISSION_DENIED, "Permission denied. Check permissions: %s",
        FailureType.TIMEOUT, "Command timed out. Try splitting the task or optimizing: %s",
        FailureType.UNKNOWN, "Execution failed. Check the error details: %s"
    );

    public String suggest(FailureType type, String errorDetail) {
        String template = TEMPLATES.getOrDefault(type, "Execution failed: %s");
        return String.format(template, errorDetail != null ? errorDetail : "unknown error");
    }
}