package ai4se.harness.core;

import ai4se.harness.config.HarnessConfig;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.CommandGuardrail;
import ai4se.harness.guardrails.GuardrailChain;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AgentLoopTest {
    @Test
    void shouldCompleteSimpleTask(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "echo hello"), "tool_use"),
            new LlmResponse("Task completed", null, null, "end_turn")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10));

        AgentLoop loop = new AgentLoop(
            mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            createDefaultConfig()
        );

        String result = loop.run("say hello");
        assertThat(result).contains("Task completed");
    }

    @Test
    void shouldBlockDangerousCommandAndReturnFailure(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "rm -rf /"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "rm -rf /"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "rm -rf /"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "rm -rf /"), "tool_use")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10));

        GuardrailChain guardrails = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf"))
        ));

        AgentLoop loop = new AgentLoop(
            mock, registry, guardrails, new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            createDefaultConfig()
        );

        String result = loop.run("delete everything");
        assertThat(result).contains("Failed after 4 correction rounds");
    }

    @Test
    void shouldCorrectAfterFailure(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "exit 1"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "echo success"), "tool_use"),
            new LlmResponse("Done", null, null, "end_turn")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10));

        AgentLoop loop = new AgentLoop(
            mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            createDefaultConfig()
        );

        String result = loop.run("do something");
        assertThat(result).isEqualTo("Done");
    }

    @Test
    void shouldStopAfterMaxRounds(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "echo hello"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "echo hello"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "echo hello"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "echo hello"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "echo hello"), "tool_use")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10));

        HarnessConfig config = createDefaultConfig();
        config.getLoop().setMaxRounds(3);

        AgentLoop loop = new AgentLoop(
            mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            config
        );

        String result = loop.run("keep going");
        assertThat(result).isEqualTo("Completed.");
    }

    @Test
    void shouldHandleUnknownTool(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "nonexistent_tool", Map.of("key", "val"), "tool_use"),
            new LlmResponse(null, "nonexistent_tool", Map.of("key", "val"), "tool_use"),
            new LlmResponse(null, "nonexistent_tool", Map.of("key", "val"), "tool_use")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10));

        HarnessConfig config = createDefaultConfig();
        config.getLoop().setMaxRounds(3);

        AgentLoop loop = new AgentLoop(
            mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            config
        );

        String result = loop.run("use unknown tool");
        assertThat(result).isEqualTo("Completed.");
    }

    private HarnessConfig createDefaultConfig() {
        HarnessConfig config = new HarnessConfig();
        HarnessConfig.LoopConfig loopConfig = new HarnessConfig.LoopConfig();
        loopConfig.setMaxRounds(10);
        config.setLoop(loopConfig);
        HarnessConfig.FeedbackConfig feedbackConfig = new HarnessConfig.FeedbackConfig();
        feedbackConfig.setMaxRounds(3);
        config.setFeedback(feedbackConfig);
        return config;
    }
}