package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class SearchToolTest {
    @Test
    void shouldGrepContent(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "hello world\nfoo bar");
        SearchTool tool = new SearchTool(tempDir);
        ToolResult result = tool.execute("{\"action\":\"grep\",\"pattern\":\"hello\"}");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("a.txt");
        assertThat(result.getOutput()).contains("hello world");
    }

    @Test
    void shouldReturnEmptyForNoMatch(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "nothing here");
        SearchTool tool = new SearchTool(tempDir);
        ToolResult result = tool.execute("{\"action\":\"grep\",\"pattern\":\"nonexistent\"}");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo("No matches found");
    }

    @Test
    void shouldReturnErrorForMissingPattern(@TempDir Path tempDir) {
        SearchTool tool = new SearchTool(tempDir);
        ToolResult result = tool.execute("{\"action\":\"grep\"}");
        assertThat(result.isSuccess()).isFalse();
    }
}
