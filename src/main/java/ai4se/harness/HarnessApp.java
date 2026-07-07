package ai4se.harness;

import ai4se.harness.config.*;
import ai4se.harness.core.*;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.*;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(name = "harness", mixinStandardHelpOptions = true,
    subcommands = {HarnessApp.RunCommand.class, HarnessApp.ChatCommand.class, HarnessApp.ConfigCommand.class})
public class HarnessApp implements Callable<Integer> {
    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    static AgentLoop buildLoop(HarnessConfig config) {
        CredentialManager cm = new CredentialManager();
        String provider = config.getLlm().getProvider();
        LlmProvider llm;

        if ("deepseek".equals(provider)) {
            String deepseekKey = cm.getKey().orElse(System.getenv("DEEPSEEK_API_KEY"));
            if (deepseekKey == null) {
                throw new IllegalStateException("No DEEPSEEK_API_KEY found. Run 'harness config set-key' first.");
            }
            llm = new DeepSeekProvider(deepseekKey, config.getLlm().getModel());
        } else if ("claude".equals(provider)) {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null) {
                throw new IllegalStateException("No ANTHROPIC_API_KEY found. Run 'harness config set-key' first.");
            }
            llm = new ClaudeProvider(apiKey, config.getLlm().getModel());
        } else {
            throw new IllegalStateException("Unknown LLM provider: " + provider);
        }

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

        return new AgentLoop(
            llm, registry, guardrails, new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            retriever, config
        );
    }

    @Command(name = "run", description = "Run a coding agent task")
    static class RunCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Task description")
        private String task;

        @Override
        public Integer call() throws Exception {
            HarnessConfig config;
            try {
                config = ConfigLoader.load(Path.of("harness.yaml"));
            } catch (Exception e) {
                System.err.println("Failed to load harness.yaml: " + e.getMessage());
                return 1;
            }
            AgentLoop loop;
            try {
                loop = buildLoop(config);
            } catch (IllegalStateException e) {
                System.err.println(e.getMessage());
                return 1;
            }
            String result = loop.run(task);
            System.out.println("\n=== Result ===");
            System.out.println(result);
            return 0;
        }
    }

    @Command(name = "chat", description = "Interactive chat mode (type '退出' to exit)")
    static class ChatCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            HarnessConfig config;
            try {
                config = ConfigLoader.load(Path.of("harness.yaml"));
            } catch (Exception e) {
                System.err.println("Failed to load harness.yaml: " + e.getMessage());
                return 1;
            }
            AgentLoop loop;
            try {
                loop = buildLoop(config);
            } catch (IllegalStateException e) {
                System.err.println(e.getMessage());
                return 1;
            }
            loop.chat();
            return 0;
        }
    }

    @Command(name = "config", description = "Manage configuration",
        subcommands = {ConfigCommand.SetKeyCommand.class, ConfigCommand.ShowKeyCommand.class, ConfigCommand.ClearKeyCommand.class})
    static class ConfigCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }

        @Command(name = "set-key", description = "Store API key (hidden input)")
        static class SetKeyCommand implements Callable<Integer> {
            @Override
            public Integer call() {
                java.io.Console console = System.console();
                if (console == null) {
                    System.err.println("No console available. Run 'harness config set-key' from a terminal.");
                    return 1;
                }
                char[] input = console.readPassword("Enter API key: ");
                new CredentialManager().storeKey(new String(input));
                Arrays.fill(input, ' ');
                System.out.println("Key stored.");
                return 0;
            }
        }

        @Command(name = "show-key", description = "Show masked API key")
        static class ShowKeyCommand implements Callable<Integer> {
            @Override
            public Integer call() {
                System.out.println(new CredentialManager().getMaskedKey().orElse("(no key set)"));
                return 0;
            }
        }

        @Command(name = "clear-key", description = "Clear stored API key")
        static class ClearKeyCommand implements Callable<Integer> {
            @Override
            public Integer call() {
                new CredentialManager().clearKey();
                System.out.println("Key cleared.");
                return 0;
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new HarnessApp()).execute(args);
        System.exit(exitCode);
    }
}
