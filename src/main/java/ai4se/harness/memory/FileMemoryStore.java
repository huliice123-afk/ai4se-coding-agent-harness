package ai4se.harness.memory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

public class FileMemoryStore implements MemoryStore {
    private final Path storePath;

    public FileMemoryStore(Path storePath) {
        this.storePath = storePath.toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, String content) {
        try {
            Files.createDirectories(storePath);
            Files.writeString(storePath.resolve(key + ".md"), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save memory: " + key, e);
        }
    }

    @Override
    public Optional<String> load(String key) {
        Path file = storePath.resolve(key + ".md");
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}