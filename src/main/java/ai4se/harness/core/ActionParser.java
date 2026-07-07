package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;

public class ActionParser {
    public Action parse(LlmResponse response) {
        if (response.hasAction()) {
            return new Action(response.getActionName(), response.getActionParams());
        }
        return null;
    }
}