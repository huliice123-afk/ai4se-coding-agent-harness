package ai4se.harness.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CredentialManagerTest {
    @Test
    void shouldStoreAndRetrieveKey() {
        CredentialManager cm = new CredentialManager();
        cm.storeKey("sk-ant-test-key");
        assertThat(cm.getKey()).isPresent();
        assertThat(cm.getKey().get()).isEqualTo("sk-ant-test-key");
    }

    @Test
    void shouldClearKey() {
        CredentialManager cm = new CredentialManager();
        cm.storeKey("sk-ant-test-key");
        cm.clearKey();
        assertThat(cm.getKey()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoKey() {
        CredentialManager cm = new CredentialManager();
        assertThat(cm.getKey()).isEmpty();
    }
}