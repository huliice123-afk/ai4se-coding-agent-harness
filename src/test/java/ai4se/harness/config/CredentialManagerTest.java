package ai4se.harness.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class CredentialManagerTest {
    @Test
    void shouldStoreAndRetrieveKey(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        cm.storeKey("sk-test1234567890abcd");
        assertThat(cm.getKey()).isPresent();
        assertThat(cm.getKey().get()).isEqualTo("sk-test1234567890abcd");
    }

    @Test
    void shouldClearKey(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        cm.storeKey("sk-test1234567890abcd");
        cm.clearKey();
        assertThat(cm.getKey()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoKey(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        assertThat(cm.getKey()).isEmpty();
        assertThat(cm.hasKey()).isFalse();
    }

    @Test
    void shouldMaskKeyNotShowPlainText(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        cm.storeKey("sk-test1234567890abcd");
        assertThat(cm.getMaskedKey()).isPresent();
        String masked = cm.getMaskedKey().get();
        assertThat(masked).contains("****");
        assertThat(masked).doesNotContain("test1234567890");
        assertThat(masked).startsWith("sk-");
        assertThat(masked).endsWith("abcd");
    }

    @Test
    void shouldLoadFromEnvFile(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "DEEPSEEK_API_KEY=sk-fromfile1234\n");
        CredentialManager cm = new CredentialManager(envFile);
        assertThat(cm.loadFromEnvFile()).isPresent();
        assertThat(cm.loadFromEnvFile().get()).isEqualTo("sk-fromfile1234");
        assertThat(cm.hasKey()).isTrue();
    }

    @Test
    void shouldHaveKeyAfterStore(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        assertThat(cm.hasKey()).isFalse();
        cm.storeKey("sk-test1234567890abcd");
        assertThat(cm.hasKey()).isTrue();
    }

    @Test
    void shouldPreserveOtherEnvEntries(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "OTHER_VAR=keepme\nDEEPSEEK_API_KEY=sk-old1234\n");
        CredentialManager cm = new CredentialManager(envFile);
        cm.storeKey("sk-new5678");
        String content = Files.readString(envFile);
        assertThat(content).contains("OTHER_VAR=keepme");
        assertThat(content).contains("DEEPSEEK_API_KEY=sk-new5678");
        assertThat(content).doesNotContain("sk-old1234");
    }
}
