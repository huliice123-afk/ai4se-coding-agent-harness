package ai4se.harness.tools;

import java.util.Map;

public interface Tool {
    String name();
    String description();
    ToolResult execute(Map<String, Object> params);
}