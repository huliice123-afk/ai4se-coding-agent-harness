package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MockLlmProviderTest {
    @Test
    void scriptModeShouldReturnToolCallWhenKeywordMatched() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.whenInputContains("write test").thenReturn(
            new LlmResponse(null, "shell", "{\"command\":\"mvn test\"}", "tool_use")
        );

        LlmResponse resp = mock.complete(
            List.of(new Message("user", "please write test")),
            List.of()
        );

        assertThat(resp.hasToolCall()).isTrue();
        assertThat(resp.getToolName()).isEqualTo("shell");
        assertThat(resp.getToolArgs()).isEqualTo("{\"command\":\"mvn test\"}");
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
        assertThat(resp.hasToolCall()).isFalse();
    }

    @Test
    void sequenceModeShouldReturnResponsesInOrder() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", "{\"command\":\"step1\"}", "tool_use"),
            new LlmResponse(null, "shell", "{\"command\":\"step2\"}", "tool_use"),
            new LlmResponse("all done", null, null, "end_turn")
        ));

        LlmResponse first = mock.complete(List.of(), List.of());
        LlmResponse second = mock.complete(List.of(), List.of());
        LlmResponse third = mock.complete(List.of(), List.of());

        assertThat(first.getToolArgs()).isEqualTo("{\"command\":\"step1\"}");
        assertThat(second.getToolArgs()).isEqualTo("{\"command\":\"step2\"}");
        assertThat(third.getText()).isEqualTo("all done");
        assertThat(third.hasToolCall()).isFalse();
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
