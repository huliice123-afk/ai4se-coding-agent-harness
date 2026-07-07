package ai4se.harness.llm;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ClaudeProviderTest {

    private MockWebServer server;
    private ClaudeProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        provider = new ClaudeProvider("sk-ant-test", "claude-sonnet-4-20250514",
                server.url("/v1/messages").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void shouldSendCorrectHeaders() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\"," +
                        "\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}],\"stop_reason\":\"end_turn\"}")
                .setResponseCode(200));

        provider.complete(List.of(new Message("user", "Hi")), List.of());

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("x-api-key")).isEqualTo("sk-ant-test");
        assertThat(request.getHeader("anthropic-version")).isEqualTo("2023-06-01");
        assertThat(request.getHeader("content-type")).isEqualTo("application/json; charset=utf-8");
    }

    @Test
    void shouldParseToolUseResponse() {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"msg_2\",\"type\":\"message\",\"role\":\"assistant\"," +
                        "\"content\":[{\"type\":\"tool_use\",\"name\":\"read_file\"," +
                        "\"input\":{\"path\":\"src/Main.java\"}}],\"stop_reason\":\"tool_use\"}")
                .setResponseCode(200));

        LlmResponse response = provider.complete(List.of(new Message("user", "read file")), List.of());

        assertThat(response.hasAction()).isTrue();
        assertThat(response.getActionName()).isEqualTo("read_file");
        assertThat(response.getActionParams()).containsEntry("path", "src/Main.java");
        assertThat(response.getStopReason()).isEqualTo("tool_use");
    }

    @Test
    void shouldParseTextResponse() {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"msg_3\",\"type\":\"message\",\"role\":\"assistant\"," +
                        "\"content\":[{\"type\":\"text\",\"text\":\"Hello, world!\"}],\"stop_reason\":\"end_turn\"}")
                .setResponseCode(200));

        LlmResponse response = provider.complete(List.of(new Message("user", "say hello")), List.of());

        assertThat(response.getText()).isEqualTo("Hello, world!");
        assertThat(response.hasAction()).isFalse();
        assertThat(response.getStopReason()).isEqualTo("end_turn");
    }

    @Test
    void shouldReturnErrorOnNon200Response() {
        server.enqueue(new MockResponse()
                .setBody("{\"error\":{\"message\":\"invalid api key\"}}")
                .setResponseCode(401));

        LlmResponse response = provider.complete(List.of(new Message("user", "test")), List.of());

        assertThat(response.getText()).contains("API error");
        assertThat(response.getText()).contains("401");
        assertThat(response.hasAction()).isFalse();
    }
}