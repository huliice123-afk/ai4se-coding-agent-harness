package ai4se.harness;

import ai4se.harness.config.*;
import ai4se.harness.core.*;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.*;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(name = "harness", subcommands = {HarnessApp.RunCommand.class, HarnessApp.ConfigCommand.class})
public class HarnessApp implements Callable<Integer> {
    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "run", description = "Run a coding agent task")
    static class RunCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Task description")
        private String task;

        @Override
        public Integer call() throws Exception {
            Path configPath = Path.of("harness.yaml");
            HarnessConfig config = ConfigLoader.load(configPath);

            CredentialManager cm = new CredentialManager();
            String apiKey = cm.getKey().orElse(System.getenv("ANTHROPIC_API_KEY"));
            if (apiKey == null) {
                System.err.println("No API key found. Run 'harness config set-key' first.");
                return 1;
            }

            LlmProvider llm = new ClaudeProvider(apiKey, config.getLlm().getModel());
            ToolRegistry registry = new ToolRegistry();
            Path projectRoot = Path.of(".").toAbsolutePath().normalize();
            registry.register(new FileTool(projectRoot));
            registry.register(new ShellTool(config.getTools().getShellTimeout()));
            registry.register(new GitTool(projectRoot));
            registry.register(new SearchTool(projectRoot));

            GuardrailChain guardrails = new GuardrailChain(List.of(
                new CommandGuardrail(config.getGuardrails().getCommandDenylist()),
                new FileGuardrail(projectRoot),
                new NetworkGuardrail()
            ));

            FileMemoryStore store = new FileMemoryStore(Path.of(config.getMemory().getStorePath()));
            MemoryRetriever retriever = new MemoryRetriever(store);

            AgentLoop loop = new AgentLoop(
                llm, registry, guardrails, new FeedbackPipeline(),
                new ContextAssembler(), new ActionParser(), new StopCondition(),
                retriever, config
            );

            String result = loop.run(task);
            System.out.println("\n=== Result ===");
            System.out.println(result);
            return 0;
        }
    }

    @Command(name = "config", description = "Manage configuration")
    static class ConfigCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new HarnessApp()).execute(args);
        System.exit(exitCode);
    }
}