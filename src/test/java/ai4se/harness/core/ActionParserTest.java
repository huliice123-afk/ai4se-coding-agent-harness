package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ActionParserTest {
    private final ActionParser parser = new ActionParser();

    @Test
    void shouldParseActionResponse() {
        LlmResponse resp = new LlmResponse(null, "shell", Map.of("command", "mvn test"), "tool_use");
        Action action = parser.parse(resp);
        assertThat(action.getToolName()).isEqualTo("shell");
        assertThat(action.getParams()).containsEntry("command", "mvn test");
    }

    @Test
    void shouldReturnNullForTextResponse() {
        LlmResponse resp = new LlmResponse("Task completed.", null, null, "end_turn");
        Action action = parser.parse(resp);
        assertThat(action).isNull();
    }
}