package ai4se.harness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class FileMemoryStoreTest {
    @Test
    void shouldSaveAndLoad(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("session_001", "Fixed bug in UserService");
        assertThat(store.load("session_001")).isPresent();
        assertThat(store.load("session_001").get()).isEqualTo("Fixed bug in UserService");
    }

    @Test
    void shouldReturnEmptyForMissingKey(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        assertThat(store.load("nonexistent")).isEmpty();
    }

    @Test
    void shouldOverwriteExistingKey(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("key", "v1");
        store.save("key", "v2");
        assertThat(store.load("key").get()).isEqualTo("v2");
    }
}