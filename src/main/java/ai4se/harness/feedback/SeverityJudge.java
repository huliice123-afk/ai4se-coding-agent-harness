package ai4se.harness.feedback;

import java.util.Map;

public class SeverityJudge {
    private static final Map<FailureType, Severity> MAPPING = Map.of(
        FailureType.COMMAND_REJECTED, Severity.CRITICAL,
        FailureType.PERMISSION_DENIED, Severity.CRITICAL,
        FailureType.COMPILATION_ERROR, Severity.ERROR,
        FailureType.RUNTIME_ERROR, Severity.ERROR,
        FailureType.TEST_FAILURE, Severity.ERROR,
        FailureType.TIMEOUT, Severity.WARNING,
        FailureType.FILE_NOT_FOUND, Severity.WARNING,
        FailureType.UNKNOWN, Severity.ERROR
    );

    public Severity judge(FailureType type) {
        return MAPPING.getOrDefault(type, Severity.ERROR);
    }
}