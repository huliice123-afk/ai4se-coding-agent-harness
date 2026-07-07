package ai4se.harness.tools;

public interface Tool {
    String getName();
    String getDescription();
    String getParameters();
    ToolResult execute(String args);
}
