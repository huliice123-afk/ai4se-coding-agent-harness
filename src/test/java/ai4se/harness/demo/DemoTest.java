package ai4se.harness.demo;

import ai4se.harness.config.HarnessConfig;
import ai4se.harness.core.*;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.*;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DemoTest {

    @Test
    @DisplayName("Demo 1: Guardrail blocks dangerous command")
    void demo1_guardrailBlocksDangerousCommand() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf", "sudo", "chmod 777"));
        GuardResult result = guard.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isBlock()).isTrue();
        assertThat(result.getReason()).contains("rm -rf");
    }

    @Test
    @DisplayName("Demo 2: Feedback loop drives self-correction")
    void demo2_feedbackLoopDrivesSelfCorrection(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", "{\"command\":\"javac Broken.java\"}", "tool_use"),
            new LlmResponse(null, "shell", "{\"command\":\"echo fixed\"}", "tool_use"),
            new LlmResponse("Task completed successfully", null, null, "end_turn")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10) {
            private int callCount = 0;

            @Override
            public ToolResult execute(String args) {
                callCount++;
                if (callCount == 1) {
                    return new ToolResult(false, "Broken.java:5: error: ';' expected", 1);
                }
                return new ToolResult(true, "Compilation successful", 0);
            }
        });

        AgentLoop loop = new AgentLoop(
            mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            createConfig()
        );

        String result = loop.run("compile and fix code");
        assertThat(result).contains("successful");
        assertThat(mock.getCallCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Demo 3: Chat mode - text response stops after 1 call (no spin)")
    void demo3_chatMode_textResponseStops(@TempDir Path tempDir) {
        String input = "hello\n退出\n";
        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        try {
            MockLlmProvider mock = new MockLlmProvider();
            mock.setSequence(List.of(
                new LlmResponse("Hi there! How can I help?", null, null, "end_turn")
            ));

            ToolRegistry registry = new ToolRegistry();
            AgentLoop loop = new AgentLoop(
                mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
                new ContextAssembler(), new ActionParser(), new StopCondition(),
                new MemoryRetriever(new FileMemoryStore(tempDir)),
                createConfig()
            );

            loop.chat();

            assertThat(mock.getCallCount()).isEqualTo(1);
        } finally {
            System.setIn(originalIn);
        }
    }

    private HarnessConfig createConfig() {
        HarnessConfig c = new HarnessConfig();
        HarnessConfig.LoopConfig lc = new HarnessConfig.LoopConfig();
        lc.setMaxRounds(10);
        c.setLoop(lc);
        HarnessConfig.FeedbackConfig fc = new HarnessConfig.FeedbackConfig();
        fc.setMaxRounds(3);
        c.setFeedback(fc);
        return c;
    }
}
