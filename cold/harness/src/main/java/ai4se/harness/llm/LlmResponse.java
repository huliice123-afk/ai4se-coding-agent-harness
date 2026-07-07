package ai4se.harness.llm;

import java.util.Map;

public class LlmResponse {
    private final String text;
    private final String actionName;
    private final Map<String, Object> actionParams;
    private final String stopReason;

    public LlmResponse(String text, String actionName, Map<String, Object> actionParams, String stopReason) {
        this.text = text;
        this.actionName = actionName;
        this.actionParams = actionParams;
        this.stopReason = stopReason;
    }

    public String getText() { return text; }
    public String getActionName() { return actionName; }
    public Map<String, Object> getActionParams() { return actionParams; }
    public String getStopReason() { return stopReason; }
    public boolean hasAction() { return actionName != null; }
}