package ai4se.harness.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import ai4se.harness.tools.Tool;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClaudeProvider implements LlmProvider {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String apiKey;
    private final String model;
    private final OkHttpClient client;
    private final String baseUrl;

    public ClaudeProvider(String apiKey, String model) {
        this(apiKey, model, API_URL);
    }

    ClaudeProvider(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public LlmResponse complete(List<Message> messages, List<Tool> tools) {
        try {
            Map<String, Object> body = buildRequestBody(messages, tools);
            String json = mapper.writeValueAsString(body);

            Request request = new Request.Builder()
                .url(baseUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return new LlmResponse("API error: " + response.code() + " " + responseBody, null, null, "end_turn");
                }
                return parseResponse(responseBody);
            }
        } catch (IOException e) {
            return new LlmResponse("API call failed: " + e.getMessage(), null, null, "end_turn");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestBody(List<Message> messages, List<Tool> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);

        List<Map<String, Object>> msgs = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            msgs.add(m);
        }
        body.put("messages", msgs);

        if (!tools.isEmpty()) {
            List<Map<String, Object>> toolDefs = new ArrayList<>();
            for (Tool tool : tools) {
                Map<String, Object> td = new LinkedHashMap<>();
                td.put("name", tool.name());
                td.put("description", tool.description());
                td.put("input_schema", Map.of("type", "object", "properties", Map.of()));
                toolDefs.add(td);
            }
            body.put("tools", toolDefs);
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String responseBody) throws IOException {
        Map<String, Object> resp = mapper.readValue(responseBody, Map.class);
        String stopReason = (String) resp.getOrDefault("stop_reason", "end_turn");

        List<Map<String, Object>> content = (List<Map<String, Object>>) resp.get("content");
        if (content == null) {
            return new LlmResponse("No content in response", null, null, stopReason);
        }

        for (Map<String, Object> block : content) {
            String type = (String) block.get("type");
            if ("tool_use".equals(type)) {
                String name = (String) block.get("name");
                Map<String, Object> input = (Map<String, Object>) block.get("input");
                return new LlmResponse(null, name, input, stopReason);
            }
            if ("text".equals(type)) {
                return new LlmResponse((String) block.get("text"), null, null, stopReason);
            }
        }

        return new LlmResponse("No parsable content", null, null, stopReason);
    }
}