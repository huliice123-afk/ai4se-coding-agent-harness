package ai4se.harness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MemoryRetrieverTest {
    @Test
    void shouldSaveAndRetrieveSessionLatest(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store);
        retriever.saveSessionSummary("Fixed authentication bug in UserService");
        String result = retriever.retrieve("authentication");
        assertThat(result).contains("authentication");
        assertThat(result).contains("UserService");
    }

    @Test
    void shouldPersistAcrossSessions(@TempDir Path tempDir) {
        FileMemoryStore store1 = new FileMemoryStore(tempDir);
        MemoryRetriever retriever1 = new MemoryRetriever(store1);
        retriever1.saveSessionSummary("Implemented OAuth2 login flow for the API");

        FileMemoryStore store2 = new FileMemoryStore(tempDir);
        MemoryRetriever retriever2 = new MemoryRetriever(store2);
        String result = retriever2.retrieve("OAuth");
        assertThat(result).contains("OAuth2");
        assertThat(result).contains("login flow");
    }

    @Test
    void shouldReturnEmptyWhenNoMemory(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store);
        assertThat(retriever.retrieve("anything")).isEmpty();
    }

    @Test
    void shouldRetrieveByKeyword(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("decisions_1", "Chose Jackson for YAML parsing because of performance");
        store.save("decisions_2", "Used Mockito for testing because of JUnit integration");
        MemoryRetriever retriever = new MemoryRetriever(store);

        var results = retriever.search("YAML", 3);
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains("Jackson");
    }

    @Test
    void shouldReturnEmptyForNoMatch(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("decisions_1", "Used Java 17");
        MemoryRetriever retriever = new MemoryRetriever(store);

        var results = retriever.search("python", 3);
        assertThat(results).isEmpty();
    }
    @Test
    void shouldUseCustomKeys(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("custom_key_1", "Chose gRPC for service communication");
        store.save("custom_key_2", "Used PostgreSQL for the database");
        MemoryRetriever retriever = new MemoryRetriever(store, List.of("custom_key_1", "custom_key_2"));

        var results = retriever.search("gRPC", 3);
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains("gRPC");
    }

    @Test
    void shouldNotSearchDefaultKeysWhenCustomKeysProvided(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("decisions_1", "Default key content");
        store.save("custom", "Custom key content");
        MemoryRetriever retriever = new MemoryRetriever(store, List.of("custom"));

        var results = retriever.search("Default", 3);
        assertThat(results).isEmpty();
    }
}
