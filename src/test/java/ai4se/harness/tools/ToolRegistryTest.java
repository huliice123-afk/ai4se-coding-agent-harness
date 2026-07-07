package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ToolRegistryTest {
    @Test
    void getToolReturnsRegisteredTool() {
        ToolRegistry registry = new ToolRegistry();
        Tool tool = new StubTool("test", "test tool", "{}");
        registry.register(tool);
        assertThat(registry.getTool("test")).isPresent();
        assertThat(registry.getTool("test").get().getName()).isEqualTo("test");
    }

    @Test
    void getToolReturnsEmptyForNonExistent() {
        ToolRegistry registry = new ToolRegistry();
        assertThat(registry.getTool("unknown")).isEmpty();
    }

    @Test
    void getToolDefinitionsReturnsAllToolsWithSchemas() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new StubTool("a", "desc a", "{\"type\":\"object\"}"));
        registry.register(new StubTool("b", "desc b", "{\"type\":\"object\"}"));
        var defs = registry.getToolDefinitions();
        assertThat(defs).hasSize(2);
        assertThat(defs.get(0).name()).isEqualTo("a");
        assertThat(defs.get(0).description()).isEqualTo("desc a");
        assertThat(defs.get(0).parameters()).isEqualTo("{\"type\":\"object\"}");
        assertThat(defs.get(1).name()).isEqualTo("b");
    }

    static class StubTool implements Tool {
        private final String name;
        private final String desc;
        private final String params;

        StubTool(String name, String desc, String params) {
            this.name = name;
            this.desc = desc;
            this.params = params;
        }

        @Override public String getName() { return name; }
        @Override public String getDescription() { return desc; }
        @Override public String getParameters() { return params; }
        @Override public ToolResult execute(String args) { return new ToolResult(true, "ok"); }
    }
}
