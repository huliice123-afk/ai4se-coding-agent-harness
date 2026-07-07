package ai4se.harness.feedback;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SeverityJudgeTest {
    private final SeverityJudge judge = new SeverityJudge();

    @Test
    void shouldJudgeCompileErrorAsError() {
        assertThat(judge.judge(FailureType.COMPILATION_ERROR)).isEqualTo(Severity.ERROR);
    }

    @Test
    void shouldJudgeCommandRejectedAsFatal() {
        assertThat(judge.judge(FailureType.COMMAND_REJECTED)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void shouldJudgeTimeoutAsWarning() {
        assertThat(judge.judge(FailureType.TIMEOUT)).isEqualTo(Severity.WARNING);
    }

    @Test
    void shouldJudgePermissionDeniedAsCritical() {
        assertThat(judge.judge(FailureType.PERMISSION_DENIED)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void shouldJudgeRuntimeErrorAsError() {
        assertThat(judge.judge(FailureType.RUNTIME_ERROR)).isEqualTo(Severity.ERROR);
    }

    @Test
    void shouldJudgeTestFailureAsError() {
        assertThat(judge.judge(FailureType.TEST_FAILURE)).isEqualTo(Severity.ERROR);
    }

    @Test
    void shouldJudgeFileNotFoundAsWarning() {
        assertThat(judge.judge(FailureType.FILE_NOT_FOUND)).isEqualTo(Severity.WARNING);
    }

    @Test
    void shouldJudgeUnknownAsError() {
        assertThat(judge.judge(FailureType.UNKNOWN)).isEqualTo(Severity.ERROR);
    }
}