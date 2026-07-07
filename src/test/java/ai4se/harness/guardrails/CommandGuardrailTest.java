package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class CommandGuardrailTest {
    @Test
    void shouldBlockDangerousCommand() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf", "sudo", "chmod 777"));
        GuardResult result = guard.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldPassSafeCommand() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf", "sudo"));
        GuardResult result = guard.check("shell", Map.of("command", "echo hello"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldPassNonShellAction() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf"));
        GuardResult result = guard.check("file", Map.of("action", "read", "path", "test.txt"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldBlockDropTable() {
        CommandGuardrail guard = new CommandGuardrail(List.of("DROP TABLE", "DELETE FROM"));
        GuardResult result = guard.check("shell", Map.of("command", "echo 'DROP TABLE users' | mysql"));
        assertThat(result.isBlock()).isTrue();
    }
}