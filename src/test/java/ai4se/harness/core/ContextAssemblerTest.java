package ai4se.harness.core;

import ai4se.harness.llm.Conversation;
import ai4se.harness.llm.Message;
import ai4se.harness.memory.FileMemoryStore;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.Tool;
import ai4se.harness.tools.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ContextAssemblerTest {
    private final ContextAssembler assembler = new ContextAssembler();

    @Test
    void shouldAssembleContextWithSystemPrompt(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store);
        List<Tool> tools = List.of(new TestTool());

        List<Message> messages = assembler.assemble("write a test", tools, retriever, new Conversation());

        assertThat(messages).hasSizeGreaterThanOrEqualTo(2);
        assertThat(messages.get(0).getRole()).isEqualTo("system");
        assertThat(messages.get(0).getContent()).contains("coding agent");
        assertThat(messages.get(messages.size() - 1).getRole()).isEqualTo("user");
        assertThat(messages.get(messages.size() - 1).getContent()).isEqualTo("write a test");
    }

    @Test
    void shouldIncludeToolParameterDescriptions(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store);
        List<Tool> tools = List.of(new TestTool());

        List<Message> messages = assembler.assemble("write a test", tools, retriever, new Conversation());

        String systemPrompt = messages.get(0).getContent();
        assertThat(systemPrompt).contains("test-tool");
        assertThat(systemPrompt).contains("test description");
        assertThat(systemPrompt).contains("test-params");
        assertThat(systemPrompt).contains("Parameters");
    }

    static class TestTool implements Tool {
        @Override
        public String getName() { return "test-tool"; }
        @Override
        public String getDescription() { return "test description"; }
        @Override
        public String getParameters() { return "test-params"; }
        @Override
        public ToolResult execute(String args) { return new ToolResult(true, "ok"); }
    }
}
