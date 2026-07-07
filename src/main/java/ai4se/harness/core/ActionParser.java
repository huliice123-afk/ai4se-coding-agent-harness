package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;

public class ActionParser {
    public Action parse(LlmResponse response) {
        if (response == null || !response.hasToolCall()) {
            return null;
        }
        return new Action(response.getToolName(), response.getToolArgs());
    }
}
