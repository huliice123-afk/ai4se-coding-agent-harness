package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ShellToolTest {
    @Test
    void shouldExecuteSuccessfulCommand() {
        ShellTool tool = new ShellTool(10);
        ToolResult result = tool.execute(Map.of("command", "echo hello"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("hello");
        assertThat(result.getExitCode()).isEqualTo(0);
    }

    @Test
    void shouldReturnFailureForFailedCommand() {
        ShellTool tool = new ShellTool(10);
        ToolResult result = tool.execute(Map.of("command", "exit 1"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getExitCode()).isEqualTo(1);
    }

    @Test
    void shouldHandleMissingCommand() {
        ShellTool tool = new ShellTool(10);
        ToolResult result = tool.execute(Map.of());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOutput()).contains("Missing required parameter: command");
    }
}