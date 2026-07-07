package ai4se.harness.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class ConfigLoaderTest {
    @Test
    void shouldLoadValidConfig(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("harness.yaml");
        Files.writeString(configPath, """
            llm:
              provider: claude
              model: claude-sonnet-4-20250514
              max_tokens: 4096
            tools:
              allowed: [file, shell, git, search]
              shell_timeout: 30
            guardrails:
              hitl: true
              command_denylist: [rm -rf, sudo]
              network_blocked: true
            feedback:
              max_rounds: 3
            loop:
              max_rounds: 10
            memory:
              store_path: .harness/memory
              search_top_k: 3
            """);

        HarnessConfig config = ConfigLoader.load(configPath);
        assertThat(config.getLlm().getProvider()).isEqualTo("claude");
        assertThat(config.getTools().getAllowed()).contains("file", "shell");
        assertThat(config.getGuardrails().isHitl()).isTrue();
        assertThat(config.getLoop().getMaxRounds()).isEqualTo(10);
    }

    @Test
    void shouldIgnoreUnknownYamlProperties(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("harness.yaml");
        Files.writeString(configPath, """
            llm:
              provider: deepseek
              model: glm-5.2
              max_tokens: 8192
              unknown_llm_field: true
            tools:
              allowed: [file]
              shell_timeout: 60
              extra_tools_field: 123
            guardrails:
              hitl: false
              network_blocked: false
              network_allowed_hosts: [api.example.com]
              surprise_field: hello
            feedback:
              max_rounds: 5
              stray: value
            loop:
              max_rounds: 20
              extra: data
            memory:
              store_path: .harness/memory
              search_top_k: 5
              mystery: field
            top_level_unknown: true
            """);

        HarnessConfig config = ConfigLoader.load(configPath);
        assertThat(config.getLlm().getProvider()).isEqualTo("deepseek");
        assertThat(config.getGuardrails().getNetworkAllowedHosts()).contains("api.example.com");
        assertThat(config.getMemory().getSearchTopK()).isEqualTo(5);
    }
}