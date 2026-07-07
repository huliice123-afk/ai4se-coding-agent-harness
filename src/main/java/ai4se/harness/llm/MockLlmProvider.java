package ai4se.harness.llm;

import java.util.*;

public class MockLlmProvider implements LlmProvider {
    private final Map<String, LlmResponse> script = new LinkedHashMap<>();
    private final List<LlmResponse> sequence = new ArrayList<>();
    private int sequenceIndex = 0;

    public ScriptBuilder whenInputContains(String keyword) {
        return new ScriptBuilder(this, keyword);
    }

    void addScript(String keyword, LlmResponse response) {
        script.put(keyword, response);
    }

    public void setSequence(List<LlmResponse> responses) {
        sequence.clear();
        sequence.addAll(responses);
        sequenceIndex = 0;
    }

    @Override
    public LlmResponse complete(List<Message> messages, List<ai4se.harness.tools.Tool> tools) {
        if (sequenceIndex < sequence.size()) {
            return sequence.get(sequenceIndex++);
        }
        String combined = messages.stream().map(Message::getContent).reduce("", (a, b) -> a + " " + b);
        for (Map.Entry<String, LlmResponse> entry : script.entrySet()) {
            if (combined.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new LlmResponse("Task completed.", null, null, "end_turn");
    }

    public static class ScriptBuilder {
        private final MockLlmProvider mock;
        private final String keyword;

        ScriptBuilder(MockLlmProvider mock, String keyword) {
            this.mock = mock;
            this.keyword = keyword;
        }

        public void thenReturn(LlmResponse response) {
            mock.addScript(keyword, response);
        }
    }
}