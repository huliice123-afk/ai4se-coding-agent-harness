package ai4se.harness.llm;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.Map;
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
    void shouldConstructWithApiKey() {
        DeepSeekProvider p = new DeepSeekProvider("sk-test", "deepseek-chat");
        assertThat(p).isNotNull();
    }

    @Test
    void shouldParseToolUseResponse() {
        server.enqueue(new MockResponse()
            .setBody("{\"id\":\"msg_1\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"shell\",\"arguments\":\"{\\\"command\\\":\\\"echo hello\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}]}")
            .setResponseCode(200));

        LlmResponse resp = provider.complete(
            List.of(new Message("user", "say hello")), List.of());

        assertThat(resp.hasAction()).isTrue();
        assertThat(resp.getActionName()).isEqualTo("shell");
        assertThat(resp.getActionParams()).containsEntry("command", "echo hello");
    }

    @Test
    void shouldParseTextResponse() {
        server.enqueue(new MockResponse()
            .setBody("{\"id\":\"msg_1\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"Hello!\"},\"finish_reason\":\"stop\"}]}")
            .setResponseCode(200));

        LlmResponse resp = provider.complete(
            List.of(new Message("user", "hi")), List.of());

        assertThat(resp.getText()).isEqualTo("Hello!");
        assertThat(resp.getStopReason()).isEqualTo("stop");
    }

    @Test
    void shouldHandleErrorResponse() {
        server.enqueue(new MockResponse()
            .setBody("{\"error\":{\"message\":\"Invalid API key\"}}")
            .setResponseCode(401));

        LlmResponse resp = provider.complete(
            List.of(new Message("user", "hi")), List.of());

        assertThat(resp.getText()).contains("API error");
        assertThat(resp.getText()).contains("401");
    }
}