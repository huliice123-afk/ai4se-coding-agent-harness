package ai4se.harness.core;

import ai4se.harness.llm.Conversation;
import ai4se.harness.llm.Message;
import ai4se.harness.memory.FileMemoryStore;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ContextAssemblerTest {
    private final ContextAssembler assembler = new ContextAssembler();

    @Test
    void shouldAssembleContextWithSystemPrompt(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store);
        List<Tool> tools = List.of(new TestTool());

        List<Message> messages = assembler.assemble("write a test", tools, retriever, new Conversation());

        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getRole()).isEqualTo("system");
        assertThat(messages.get(0).getContent()).contains("coding agent");
        assertThat(messages.get(messages.size() - 1).getRole()).isEqualTo("user");
        assertThat(messages.get(messages.size() - 1).getContent()).isEqualTo("write a test");
    }

    static class TestTool implements Tool {
        public String name() { return "test"; }
        public String description() { return "test tool"; }
        public ai4se.harness.tools.ToolResult execute(Map<String, Object> p) { return null; }
    }
}