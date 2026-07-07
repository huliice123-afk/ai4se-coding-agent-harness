package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LlmResponseTest {
    @Test
    void textOnlyResponseShouldNotHaveToolCall() {
        LlmResponse resp = new LlmResponse("done", null, null, "end_turn");

        assertThat(resp.getText()).isEqualTo("done");
        assertThat(resp.hasToolCall()).isFalse();
        assertThat(resp.getToolName()).isNull();
        assertThat(resp.getToolArgs()).isNull();
        assertThat(resp.getStopReason()).isEqualTo("end_turn");
    }

    @Test
    void toolCallResponseShouldHaveToolCall() {
        LlmResponse resp = new LlmResponse(null, "shell", "{\"command\":\"ls\"}", "tool_use");

        assertThat(resp.hasToolCall()).isTrue();
        assertThat(resp.getToolName()).isEqualTo("shell");
        assertThat(resp.getToolArgs()).isEqualTo("{\"command\":\"ls\"}");
        assertThat(resp.getText()).isNull();
        assertThat(resp.getStopReason()).isEqualTo("tool_use");
    }

    @Test
    void emptyResponseShouldBeHandledGracefully() {
        LlmResponse resp = new LlmResponse(null, null, null, null);

        assertThat(resp.hasToolCall()).isFalse();
        assertThat(resp.getText()).isNull();
        assertThat(resp.getToolName()).isNull();
        assertThat(resp.getToolArgs()).isNull();
        assertThat(resp.getStopReason()).isNull();
    }
}
