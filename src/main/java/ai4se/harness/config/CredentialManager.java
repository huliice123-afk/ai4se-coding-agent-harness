package ai4se.harness.config;

import java.util.Optional;

public class CredentialManager {
    private String cachedKey;

    public void storeKey(String key) {
        this.cachedKey = key;
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(cachedKey);
    }

    public void clearKey() {
        this.cachedKey = null;
    }
}