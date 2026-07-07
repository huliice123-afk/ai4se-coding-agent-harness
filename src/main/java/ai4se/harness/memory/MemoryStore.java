package ai4se.harness.memory;

import java.util.Optional;

public interface MemoryStore {
    void save(String key, String content);
    Optional<String> load(String key);
}