package ai4se.harness.feedback;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SeverityJudgeTest {
    private final SeverityJudge judge = new SeverityJudge();

    @Test
    void shouldJudgeCompileErrorAsError() {
        assertThat(judge.judge(FailureType.COMPILE_ERROR)).isEqualTo(Severity.ERROR);
    }

    @Test
    void shouldJudgeCommandRejectedAsFatal() {
        assertThat(judge.judge(FailureType.COMMAND_REJECTED)).isEqualTo(Severity.FATAL);
    }

    @Test
    void shouldJudgeTimeoutAsWarning() {
        assertThat(judge.judge(FailureType.TIMEOUT)).isEqualTo(Severity.WARNING);
    }
}