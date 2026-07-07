package ai4se.harness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class MemoryRetrieverTest {
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
}