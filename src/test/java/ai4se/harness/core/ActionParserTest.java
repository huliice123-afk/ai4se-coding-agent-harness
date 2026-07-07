package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ActionParserTest {
    private final ActionParser parser = new ActionParser();

    @Test
    void parseToolCall() {
        LlmResponse resp = new LlmResponse(null, "shell", "{\"command\":\"mvn test\"}", "tool_use");
        Action action = parser.parse(resp);
        assertThat(action).isNotNull();
        assertThat(action.getToolName()).isEqualTo("shell");
        assertThat(action.getToolArgs()).isEqualTo("{\"command\":\"mvn test\"}");
    }

    @Test
    void parseTextOnly() {
        LlmResponse resp = new LlmResponse("Task completed.", null, null, "end_turn");
        Action action = parser.parse(resp);
        assertThat(action).isNull();
    }

    @Test
    void parseEmptyResponse() {
        LlmResponse resp = new LlmResponse(null, null, null, null);
        Action action = parser.parse(resp);
        assertThat(action).isNull();
    }

    @Test
    void parseNullResponse() {
        Action action = parser.parse(null);
        assertThat(action).isNull();
    }
}
