package ai4se.harness.llm;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class DeepSeekProviderTest {
    private MockWebServer server;
    private DeepSeekProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        provider = new DeepSeekProvider("sk-test-key", "deepseek-chat",
            server.url("/v1/chat/completions").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void parseOpenAiFormat_toolCallResponse() {
        String rawJson = "{\"id\":\"msg_1\",\"choices\":[{\"index\":0,\"message\":{"
            + "\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{"
            + "\"id\":\"call_123\",\"type\":\"function\",\"function\":{"
            + "\"name\":\"FileTool\",\"arguments\":\"{\\\"path\\\":\\\"test.txt\\\"}\""
            + "}}]},\"finish_reason\":\"tool_calls\"}]}";

        LlmResponse resp = DeepSeekProvider.parseResponse(rawJson);

        assertThat(resp.hasToolCall()).isTrue();
        assertThat(resp.getToolName()).isEqualTo("FileTool");
        assertThat(resp.getToolArgs()).isEqualTo("{\"path\":\"test.txt\"}");
        assertThat(resp.getText()).isNull();
        assertThat(resp.getStopReason()).isEqualTo("tool_calls");
    }

    @Test
    void parseOpenAiFormat_textResponse() {
        String rawJson = "{\"id\":\"msg_1\",\"choices\":[{\"index\":0,\"message\":{"
            + "\"role\":\"assistant\",\"content\":\"Hello!\""
            + "},\"finish_reason\":\"stop\"}]}";

        LlmResponse resp = DeepSeekProvider.parseResponse(rawJson);

        assertThat(resp.hasToolCall()).isFalse();
        assertThat(resp.getText()).isEqualTo("Hello!");
        assertThat(resp.getToolName()).isNull();
        assertThat(resp.getToolArgs()).isNull();
        assertThat(resp.getStopReason()).isEqualTo("stop");
    }

    @Test
    void parseOpenAiFormat_nullContent() {
        String rawJson = "{\"id\":\"msg_1\",\"choices\":[{\"index\":0,\"message\":{"
            + "\"role\":\"assistant\",\"content\":null"
            + "},\"finish_reason\":\"stop\"}]}";

        LlmResponse resp = DeepSeekProvider.parseResponse(rawJson);

        assertThat(resp.hasToolCall()).isFalse();
        assertThat(resp.getText()).isEqualTo("");
        assertThat(resp.getToolName()).isNull();
        assertThat(resp.getStopReason()).isEqualTo("stop");
    }

    @Test
    void shouldConstructWithApiKey() {
        DeepSeekProvider p = new DeepSeekProvider("sk-test", "deepseek-chat");
        assertThat(p).isNotNull();
    }

    @Test
    void completeShouldParseToolUseResponse() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"id\":\"msg_1\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
                + "\"content\":null,\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\","
                + "\"function\":{\"name\":\"shell\",\"arguments\":\"{\\\"command\\\":\\\"echo hello\\\"}\"}}]},"
                + "\"finish_reason\":\"tool_calls\"}]}")
            .setResponseCode(200));

        LlmResponse resp = provider.complete(
            List.of(new Message("user", "say hello")), List.of());

        assertThat(resp.hasToolCall()).isTrue();
        assertThat(resp.getToolName()).isEqualTo("shell");
        assertThat(resp.getToolArgs()).isEqualTo("{\"command\":\"echo hello\"}");
    }

    @Test
    void completeShouldParseTextResponse() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"id\":\"msg_1\",\"choices\":[{\"index\":0,\"message\":{"
                + "\"role\":\"assistant\",\"content\":\"Hello!\"},\"finish_reason\":\"stop\"}]}")
            .setResponseCode(200));

        LlmResponse resp = provider.complete(
            List.of(new Message("user", "hi")), List.of());

        assertThat(resp.getText()).isEqualTo("Hello!");
        assertThat(resp.getStopReason()).isEqualTo("stop");
    }

    @Test
    void completeShouldHandleErrorResponse() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"error\":{\"message\":\"Invalid API key\"}}")
            .setResponseCode(401));

        LlmResponse resp = provider.complete(
            List.of(new Message("user", "hi")), List.of());

        assertThat(resp.getText()).contains("API error");
        assertThat(resp.getText()).contains("401");
    }
}
