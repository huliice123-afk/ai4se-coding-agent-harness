package ai4se.harness.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CredentialManager {
    private static final String ENV_VAR = "DEEPSEEK_API_KEY";

    private final Path envFilePath;

    public CredentialManager() {
        this(Path.of(".env"));
    }

    public CredentialManager(Path envFilePath) {
        this.envFilePath = envFilePath.toAbsolutePath().normalize();
    }

    public Optional<String> getKey() {
        String envValue = System.getenv(ENV_VAR);
        if (envValue != null && !envValue.isBlank()) {
            return Optional.of(envValue);
        }
        return loadFromEnvFile();
    }

    Optional<String> loadFromEnvFile() {
        if (!Files.exists(envFilePath)) {
            return Optional.empty();
        }
        try {
            for (String raw : Files.readAllLines(envFilePath)) {
                String line = raw.trim();
                if (line.startsWith(ENV_VAR + "=")) {
                    String value = line.substring(ENV_VAR.length() + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (!value.isEmpty()) {
                        return Optional.of(value);
                    }
                }
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public void storeKey(String key) {
        List<String> lines = new ArrayList<>();
        boolean found = false;
        if (Files.exists(envFilePath)) {
            try {
                lines = new ArrayList<>(Files.readAllLines(envFilePath));
            } catch (IOException e) {
                lines = new ArrayList<>();
            }
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().startsWith(ENV_VAR + "=")) {
                    lines.set(i, ENV_VAR + "=" + key);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            lines.add(ENV_VAR + "=" + key);
        }
        try {
            Path parent = envFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(envFilePath, lines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store key to .env file: " + envFilePath, e);
        }
    }

    public void clearKey() {
        if (!Files.exists(envFilePath)) {
            return;
        }
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(envFilePath));
            lines.removeIf(line -> line.trim().startsWith(ENV_VAR + "="));
            Files.write(envFilePath, lines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear key from .env file: " + envFilePath, e);
        }
    }

    public boolean hasKey() {
        return getKey().isPresent();
    }

    public Optional<String> getMaskedKey() {
        return getKey().map(this::mask);
    }

    private String mask(String key) {
        if (key.length() <= 7) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }
}
