package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class LlmResponseTest {
    @Test
    void shouldCreateTextResponse() {
        LlmResponse resp = new LlmResponse("done", null, null, "end_turn");
        assertThat(resp.getText()).isEqualTo("done");
        assertThat(resp.hasAction()).isFalse();
    }

    @Test
    void shouldCreateActionResponse() {
        LlmResponse resp = new LlmResponse(null, "shell", Map.of("command", "ls"), "tool_use");
        assertThat(resp.hasAction()).isTrue();
        assertThat(resp.getActionName()).isEqualTo("shell");
        assertThat(resp.getActionParams()).containsEntry("command", "ls");
    }
}