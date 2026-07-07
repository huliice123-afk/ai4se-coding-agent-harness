package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class FileGuardrailTest {
    @Test
    void shouldBlockPathOutsideRoot(@TempDir Path tempDir) {
        FileGuardrail guard = new FileGuardrail(tempDir);
        GuardResult result = guard.check("file", Map.of("action", "read", "path", "../../secret.txt"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldPassPathInsideRoot(@TempDir Path tempDir) {
        FileGuardrail guard = new FileGuardrail(tempDir);
        GuardResult result = guard.check("file", Map.of("action", "read", "path", "src/main.java"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldPassNonFileAction() {
        FileGuardrail guard = new FileGuardrail(Path.of("/tmp"));
        GuardResult result = guard.check("shell", Map.of("command", "ls"));
        assertThat(result.isPass()).isTrue();
    }
}