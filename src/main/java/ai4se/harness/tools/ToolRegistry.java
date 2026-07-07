package ai4se.harness.tools;

import java.util.*;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<Tool> getAll() {
        return new ArrayList<>(tools.values());
    }

    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> defs = new ArrayList<>();
        for (Tool tool : tools.values()) {
            defs.add(new ToolDefinition(tool.getName(), tool.getDescription(), tool.getParameters()));
        }
        return defs;
    }
}
