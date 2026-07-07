package ai4se.harness.guardrails;

import ai4se.harness.config.HarnessConfig;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class GuardrailChainTest {
    @Test
    void shouldPassWhenAllGuardrailsPass() {
        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf")),
            new NetworkGuardrail()
        ));
        GuardResult result = chain.check("shell", Map.of("command", "echo hello"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldBlockAtFirstGuardrail() {
        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf")),
            new NetworkGuardrail()
        ));
        GuardResult result = chain.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isBlock()).isTrue();
        assertThat(result.getReason()).contains("rm -rf");
    }

    @Test
    void shouldBlockAtSecondGuardrail() {
        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf")),
            new NetworkGuardrail()
        ));
        GuardResult result = chain.check("shell", Map.of("command", "curl http://evil.com"));
        assertThat(result.isBlock()).isTrue();
        assertThat(result.getReason()).contains("curl");
    }

    @Test
    void shouldReturnHitlWhenHitlModeEnabled() {
        HarnessConfig config = new HarnessConfig();
        HarnessConfig.GuardrailsConfig guardrailsConfig = new HarnessConfig.GuardrailsConfig();
        guardrailsConfig.setHitl(true);
        config.setGuardrails(guardrailsConfig);

        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf"))
        ), config);
        GuardResult result = chain.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isHitl()).isTrue();
        assertThat(result.getReason()).contains("rm -rf");
    }

    @Test
    void shouldReturnBlockWhenHitlModeDisabled() {
        HarnessConfig config = new HarnessConfig();
        HarnessConfig.GuardrailsConfig guardrailsConfig = new HarnessConfig.GuardrailsConfig();
        guardrailsConfig.setHitl(false);
        config.setGuardrails(guardrailsConfig);

        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf"))
        ), config);
        GuardResult result = chain.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isBlock()).isTrue();
    }
}