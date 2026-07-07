package ai4se.harness.guardrails;

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
}