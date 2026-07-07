package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class StopConditionTest {
    private final StopCondition stopCondition = new StopCondition();

    @Test
    void shouldStopOnEndTurn() {
        LlmResponse resp = new LlmResponse("done", null, null, "end_turn");
        assertThat(stopCondition.shouldStop(resp, 1, 10)).isTrue();
    }

    @Test
    void shouldStopOnMaxRounds() {
        LlmResponse resp = new LlmResponse(null, "shell", "{\"command\":\"ls\"}", "tool_use");
        assertThat(stopCondition.shouldStop(resp, 10, 10)).isTrue();
    }

    @Test
    void shouldContinueOnToolUse() {
        LlmResponse resp = new LlmResponse(null, "shell", "{\"command\":\"ls\"}", "tool_use");
        assertThat(stopCondition.shouldStop(resp, 5, 10)).isFalse();
    }
}
