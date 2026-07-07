package ai4se.harness.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import ai4se.harness.tools.Tool;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DeepSeekProvider implements LlmProvider {
    private static final String DEFAULT_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final OkHttpClient client;

    public DeepSeekProvider(String apiKey, String model) {
        this(apiKey, model, DEFAULT_URL);
    }

    DeepSeekProvider(String apiKey, String model, String baseUrl) {
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
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", buildMessagesList(messages));
            if (!tools.isEmpty()) {
                body.put("tools", buildToolsList(tools));
            }

            String json = mapper.writeValueAsString(body);
            Request request = new Request.Builder()
                .url(baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return new LlmResponse("API error: " + response.code() + " " + responseBody, null, null, "end_turn");
                }
                LlmResponse parsed = parseResponse(responseBody);
                if (!parsed.hasToolCall() && (parsed.getText() == null || parsed.getText().isBlank())) {
                    System.err.println("DeepSeek raw response: " + responseBody);
                }
                return parsed;
            }
        } catch (IOException e) {
            return new LlmResponse("API call failed: " + e.getMessage(), null, null, "end_turn");
        }
    }

    private List<Map<String, Object>> buildMessagesList(List<Message> messages) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            msgs.add(m);
        }
        return msgs;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildToolsList(List<Tool> tools) {
        List<Map<String, Object>> toolDefs = new ArrayList<>();
        for (Tool tool : tools) {
            Map<String, Object> td = new LinkedHashMap<>();
            td.put("type", "function");
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("name", tool.getName());
            func.put("description", tool.getDescription());
            try {
                func.put("parameters", mapper.readValue(tool.getParameters(), Map.class));
            } catch (IOException e) {
                func.put("parameters", Map.of("type", "object", "properties", Map.of()));
            }
            td.put("function", func);
            toolDefs.add(td);
        }
        return toolDefs;
    }

    @SuppressWarnings("unchecked")
    public static LlmResponse parseResponse(String rawJson) {
        try {
            Map<String, Object> resp = mapper.readValue(rawJson, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) {
                return new LlmResponse("No choices in response", null, null, "end_turn");
            }

            Map<String, Object> choice = choices.get(0);
            String finishReason = (String) choice.getOrDefault("finish_reason", "stop");
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message == null) {
                return new LlmResponse("No message in response", null, null, finishReason);
            }

            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                Map<String, Object> toolCall = toolCalls.get(0);
                Map<String, Object> func = (Map<String, Object>) toolCall.get("function");
                String name = (String) func.get("name");
                String args = (String) func.get("arguments");
                String toolArgs = args != null ? args : "{}";
                return new LlmResponse(null, name, toolArgs, finishReason);
            }

            String content = (String) message.get("content");
            if (content == null) {
                return new LlmResponse("", null, null, finishReason);
            }
            return new LlmResponse(content, null, null, finishReason);
        } catch (IOException e) {
            return new LlmResponse("Failed to parse response: " + e.getMessage(), null, null, "end_turn");
        }
    }
}
