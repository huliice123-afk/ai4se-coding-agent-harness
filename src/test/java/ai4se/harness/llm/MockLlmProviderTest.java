package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class MockLlmProviderTest {
    @Test
    void shouldReturnScriptedResponse() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.whenInputContains("write test").thenReturn(
            new LlmResponse(null, "shell", Map.of("command", "mvn test"), "tool_use")
        );

        LlmResponse resp = mock.complete(
            List.of(new Message("user", "please write test")),
            List.of()
        );

        assertThat(resp.hasAction()).isTrue();
        assertThat(resp.getActionName()).isEqualTo("shell");
        assertThat(resp.getActionParams()).containsEntry("command", "mvn test");
    }

    @Test
    void shouldReturnDefaultResponseWhenNoMatch() {
        MockLlmProvider mock = new MockLlmProvider();
        LlmResponse resp = mock.complete(
            List.of(new Message("user", "unknown task")),
            List.of()
        );

        assertThat(resp.getText()).isEqualTo("Task completed.");
        assertThat(resp.getStopReason()).isEqualTo("end_turn");
    }

    @Test
    void shouldReturnSequenceResponses() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "step1"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "step2"), "tool_use"),
            new LlmResponse("all done", null, null, "end_turn")
        ));

        assertThat(mock.complete(List.of(), List.of()).getActionParams().get("command")).isEqualTo("step1");
        assertThat(mock.complete(List.of(), List.of()).getActionParams().get("command")).isEqualTo("step2");
        assertThat(mock.complete(List.of(), List.of()).getText()).isEqualTo("all done");
    }

    @Test
    void shouldFallbackToDefaultAfterSequenceExhausted() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse("only one", null, null, "end_turn")
        ));

        mock.complete(List.of(), List.of());
        LlmResponse resp = mock.complete(List.of(), List.of());
        assertThat(resp.getText()).isEqualTo("Task completed.");
    }
}