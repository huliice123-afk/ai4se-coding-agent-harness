package ai4se.harness.feedback;

public enum FailureType {
    COMPILATION_ERROR,
    RUNTIME_ERROR,
    TEST_FAILURE,
    COMMAND_REJECTED,
    FILE_NOT_FOUND,
    PERMISSION_DENIED,
    TIMEOUT,
    UNKNOWN
}