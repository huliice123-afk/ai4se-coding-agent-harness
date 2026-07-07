package ai4se.harness.core;

import java.util.Map;

public class Action {
    private final String toolName;
    private final Map<String, Object> params;

    public Action(String toolName, Map<String, Object> params) {
        this.toolName = toolName;
        this.params = params;
    }

    public String getToolName() { return toolName; }
    public Map<String, Object> getParams() { return params; }
}