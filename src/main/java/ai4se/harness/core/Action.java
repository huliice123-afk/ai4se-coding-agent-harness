package ai4se.harness.core;

public class Action {
    private final String toolName;
    private final String toolArgs;

    public Action(String toolName, String toolArgs) {
        this.toolName = toolName;
        this.toolArgs = toolArgs;
    }

    public String getToolName() { return toolName; }
    public String getToolArgs() { return toolArgs; }
}
