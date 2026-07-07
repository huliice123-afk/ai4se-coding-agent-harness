package ai4se.harness.demo;

import ai4se.harness.core.*;
import ai4se.harness.feedback.*;
import ai4se.harness.guardrails.*;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import ai4se.harness.config.HarnessConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
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
            new LlmResponse(null, "shell", Map.of("command", "javac Broken.java"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "echo fixed"), "tool_use"),
            new LlmResponse("Task completed successfully", null, null, "end_turn")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10) {
            private int callCount = 0;
            @Override
            public ToolResult execute(Map<String, Object> params) {
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
    }

    @Test
    @DisplayName("Demo 3: FailureClassifier covers 3+ failure types")
    void demo3_failureClassifierCoversMultipleTypes() {
        FailureClassifier classifier = new FailureClassifier();

        assertThat(classifier.classify("Main.java:5: error: ';' expected", 1, "shell"))
            .isEqualTo(FailureType.COMPILE_ERROR);
        assertThat(classifier.classify("Tests run: 3, Failures: 1", 1, "shell"))
            .isEqualTo(FailureType.TEST_FAILURE);
        assertThat(classifier.classify("Exception in thread \"main\" NullPointerException", 1, "shell"))
            .isEqualTo(FailureType.RUNTIME_ERROR);
        assertThat(classifier.classify("Command timed out after 30s", 143, "shell"))
            .isEqualTo(FailureType.TIMEOUT);
        assertThat(classifier.classify("Dangerous command detected: rm -rf", 1, "shell"))
            .isEqualTo(FailureType.COMMAND_REJECTED);
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