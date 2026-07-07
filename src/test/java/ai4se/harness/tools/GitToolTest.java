package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class GitToolTest {
    @Test
    void shouldReturnGitStatus(@TempDir Path tempDir) throws Exception {
        initGitRepo(tempDir);
        GitTool tool = new GitTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "status"));
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldReturnErrorForNonGitRepo(@TempDir Path tempDir) {
        GitTool tool = new GitTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "status"));
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void shouldHandleUnknownAction(@TempDir Path tempDir) throws Exception {
        initGitRepo(tempDir);
        GitTool tool = new GitTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "unknown"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOutput()).contains("Unknown action");
    }

    private void initGitRepo(Path dir) throws Exception {
        new ProcessBuilder("git", "init").directory(dir.toFile()).start().waitFor();
    }
}