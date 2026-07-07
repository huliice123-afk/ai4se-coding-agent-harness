package ai4se.harness.feedback;

public class FailureClassifier {
    public FailureType classify(String output, int exitCode, String actionName) {
        if (output == null) return FailureType.UNKNOWN;

        String lower = output.toLowerCase();

        if (lower.contains("error:") && (lower.contains(".java:") || lower.contains(".kt:"))) {
            return FailureType.COMPILE_ERROR;
        }
        if (lower.contains("tests run:") && lower.contains("failures:")) {
            return FailureType.TEST_FAILURE;
        }
        if (lower.contains("exception in thread") || lower.contains("stacktrace")) {
            return FailureType.RUNTIME_ERROR;
        }
        if (lower.contains("timed out") || lower.contains("timeout")) {
            return FailureType.TIMEOUT;
        }
        if (lower.contains("dangerous command") || lower.contains("access denied") || lower.contains("blocked")) {
            return FailureType.COMMAND_REJECTED;
        }
        if (lower.contains("file not found") || lower.contains("no such file")) {
            return FailureType.FILE_NOT_FOUND;
        }
        if (lower.contains("permission denied")) {
            return FailureType.PERMISSION_DENIED;
        }

        return FailureType.UNKNOWN;
    }
}