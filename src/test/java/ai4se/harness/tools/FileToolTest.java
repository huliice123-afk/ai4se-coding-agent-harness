package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class FileToolTest {
    @Test
    void shouldReadFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute("{\"action\":\"read\",\"path\":\"test.txt\"}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("hello world");
    }

    @Test
    void shouldWriteFile(@TempDir Path tempDir) {
        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute("{\"action\":\"write\",\"path\":\"out.txt\",\"content\":\"hello\"}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(Files.exists(tempDir.resolve("out.txt"))).isTrue();
    }

    @Test
    void shouldBlockPathTraversal(@TempDir Path tempDir) {
        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute("{\"action\":\"read\",\"path\":\"../../etc/passwd\"}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOutput()).contains("outside project root");
    }

    @Test
    void shouldReturnErrorForMissingFile(@TempDir Path tempDir) {
        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute("{\"action\":\"read\",\"path\":\"nonexistent.txt\"}");

        assertThat(result.isSuccess()).isFalse();
    }
}
