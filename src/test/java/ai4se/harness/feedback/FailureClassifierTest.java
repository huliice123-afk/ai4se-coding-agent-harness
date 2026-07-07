package ai4se.harness.feedback;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FailureClassifierTest {
    private final FailureClassifier classifier = new FailureClassifier();

    @Test
    void shouldClassifyCompileError() {
        FailureType type = classifier.classify(
            "Main.java:5: error: ';' expected", 1, "shell");
        assertThat(type).isEqualTo(FailureType.COMPILE_ERROR);
    }

    @Test
    void shouldClassifyTestFailure() {
        FailureType type = classifier.classify(
            "Tests run: 3, Failures: 1\nExpected: 42 but was: 0", 1, "shell");
        assertThat(type).isEqualTo(FailureType.TEST_FAILURE);
    }

    @Test
    void shouldClassifyRuntimeError() {
        FailureType type = classifier.classify(
            "Exception in thread \"main\" java.lang.NullPointerException", 1, "shell");
        assertThat(type).isEqualTo(FailureType.RUNTIME_ERROR);
    }

    @Test
    void shouldClassifyTimeout() {
        FailureType type = classifier.classify(
            "Command timed out after 30s", 143, "shell");
        assertThat(type).isEqualTo(FailureType.TIMEOUT);
    }

    @Test
    void shouldClassifyCommandRejected() {
        FailureType type = classifier.classify(
            "Dangerous command detected: rm -rf", 1, "shell");
        assertThat(type).isEqualTo(FailureType.COMMAND_REJECTED);
    }

    @Test
    void shouldClassifyFileNotFound() {
        FailureType type = classifier.classify(
            "File not found: src/Main.java", 1, "file");
        assertThat(type).isEqualTo(FailureType.FILE_NOT_FOUND);
    }

    @Test
    void shouldClassifyUnknown() {
        FailureType type = classifier.classify("Something went wrong", 1, "shell");
        assertThat(type).isEqualTo(FailureType.UNKNOWN);
    }
}