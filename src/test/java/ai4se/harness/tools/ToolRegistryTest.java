package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ToolRegistryTest {
    @Test
    void shouldRegisterAndRetrieveTool() {
        ToolRegistry registry = new ToolRegistry();
        Tool tool = new Tool() {
            public String name() { return "test"; }
            public String description() { return "test tool"; }
            public ToolResult execute(Map<String, Object> params) {
                return new ToolResult(true, "ok");
            }
        };
        registry.register(tool);
        assertThat(registry.get("test")).isPresent();
        assertThat(registry.get("test").get().name()).isEqualTo("test");
    }

    @Test
    void shouldReturnEmptyForUnknownTool() {
        ToolRegistry registry = new ToolRegistry();
        assertThat(registry.get("unknown")).isEmpty();
    }

    @Test
    void shouldListAllTools() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new Tool() {
            public String name() { return "a"; }
            public String description() { return "a"; }
            public ToolResult execute(Map<String, Object> p) { return new ToolResult(true, ""); }
        });
        registry.register(new Tool() {
            public String name() { return "b"; }
            public String description() { return "b"; }
            public ToolResult execute(Map<String, Object> p) { return new ToolResult(true, ""); }
        });
        assertThat(registry.getAll()).hasSize(2);
    }
}