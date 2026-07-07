package ai4se.harness.core;

import ai4se.harness.config.HarnessConfig;
import ai4se.harness.feedback.FeedbackPipeline;
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