package ai4se.harness.llm;

public class LlmResponse {
    private final String text;
    private final String toolName;
    private final String toolArgs;
    private final String stopReason;

    public LlmResponse(String text, String toolName, String toolArgs, String stopReason) {
        this.text = text;
        this.toolName = toolName;
        this.toolArgs = toolArgs;
        this.stopReason = stopReason;
    }

    public String getText() { return text; }
    public String getToolName() { return toolName; }
    public String getToolArgs() { return toolArgs; }
    public String getStopReason() { return stopReason; }
    public boolean hasToolCall() { return toolName != null; }
}
