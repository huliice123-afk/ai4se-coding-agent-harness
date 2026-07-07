# Task 5 Report: PR #5 - Config & Memory

## Status: DONE

## Summary

Implemented config loading, credential management with `.env` file support, and cross-session memory persistence for the Java Coding Agent Harness.

## Commits Created

| SHA | Subject |
|-----|---------|
| 8d7f33d | feat: add HarnessConfig, ConfigLoader, and CredentialManager (cherry-pick) |
| c18daa0 | feat: add MemoryStore, FileMemoryStore, and MemoryRetriever (cherry-pick) |
| 4d7801c | fix: add network_allowed_hosts to GuardrailsConfig, ignore unknown YAML properties (cherry-pick) |
| c7f5caf | feat: add config, credential manager, and memory with .env support (fixes) |

Branch: `feature/config-memory` → pushed to `origin/feature/config-memory`
PR: https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/4

## Key Fixes Applied

### 1. CredentialManager — .env file loading (rewrote from in-memory)

The cherry-picked `CredentialManager` only used an in-memory `cachedKey` field. Rewrote it to:

- **`getKey()`**: checks env var `DEEPSEEK_API_KEY` first, then falls back to `.env` file. Returns `Optional<String>`.
- **`storeKey(String key)`**: writes/updates the `DEEPSEEK_API_KEY=` line in the `.env` file. Preserves other entries already in the file.
- **`clearKey()`**: removes the `DEEPSEEK_API_KEY=` line from the `.env` file.
- **`getMaskedKey()`**: returns `Optional<String>` in format `sk-****xxxx` (first 3 chars + `****` + last 4 chars). Never exposes plain text. Returns `****` for keys too short to mask.
- **`hasKey()`**: checks both env var and `.env` file.
- **`loadFromEnvFile()`** (package-private): reads key from `.env` file directly, used by tests to avoid env var interference.
- Constructor accepts a `Path` to the `.env` file (default `.env`), enabling test isolation via `@TempDir`.
- `.env` is in `.gitignore` — not committed.

### 2. MemoryRetriever — cross-session persistence

The cherry-picked `MemoryRetriever` only had `search(String query, int topK)`. Added:

- **`saveSessionSummary(String summary)`**: saves to `session_latest` key via the `MemoryStore`.
- **`retrieve(String query)`**: loads `session_latest` (previous session memory) if present; falls back to keyword `search()` across `decisions_1`/`decisions_2`/`session_latest`. Returns `String` (empty if no memory).
- Cross-session persistence verified: new `FileMemoryStore` instance pointing at the same directory can read memory saved by a previous instance.

### 3. HarnessConfig — `@JsonIgnoreProperties` on all config classes

The cherry-picked fix (commit `1c9452f`) only added `@JsonIgnoreProperties(ignoreUnknown = true)` to the outer `HarnessConfig` class. Added the annotation to **all inner static config classes**:

- `LlmConfig`
- `ToolsConfig`
- `GuardrailsConfig`
- `FeedbackConfig`
- `LoopConfig`
- `MemoryConfig`

This ensures unknown YAML properties are ignored at every nesting level.

## Tests

```
mvnw.cmd test
Tests run: 59, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### CredentialManagerTest (7 tests)
- `shouldStoreAndRetrieveKey` — storeKey then getKey returns the key
- `shouldClearKey` — clearKey removes the key from .env
- `shouldReturnEmptyWhenNoKey` — no .env file → empty
- `shouldMaskKeyNotShowPlainText` — getMaskedKey contains `****`, does not contain middle of key, starts with first 3, ends with last 4
- `shouldLoadFromEnvFile` — reads `DEEPSEEK_API_KEY=sk-...` from .env file
- `shouldHaveKeyAfterStore` — hasKey false before, true after store
- `shouldPreserveOtherEnvEntries` — storeKey preserves other variables in .env

### MemoryRetrieverTest (5 tests)
- `shouldSaveAndRetrieveSessionLatest` — saveSessionSummary then retrieve returns it
- `shouldPersistAcrossSessions` — new store instance, same directory, retrieve finds previous session's summary
- `shouldReturnEmptyWhenNoMemory` — no memory stored → retrieve returns empty string
- `shouldRetrieveByKeyword` — keyword search across decisions
- `shouldReturnEmptyForNoMatch` — no keyword match → empty list

### ConfigLoaderTest (2 tests)
- `shouldLoadValidConfig` — loads all config sections from YAML
- `shouldIgnoreUnknownYamlProperties` — unknown fields at every nesting level are ignored (verifies `@JsonIgnoreProperties` on all classes)

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/ai4se/harness/config/CredentialManager.java` | Rewrote: .env file loading, masked key, store/clear |
| `src/main/java/ai4se/harness/config/HarnessConfig.java` | Added `@JsonIgnoreProperties` to all inner classes |
| `src/main/java/ai4se/harness/config/ConfigLoader.java` | Cherry-picked (unchanged) |
| `src/main/java/ai4se/harness/memory/MemoryRetriever.java` | Added `saveSessionSummary`, `retrieve` |
| `src/main/java/ai4se/harness/memory/MemoryStore.java` | Cherry-picked (unchanged) |
| `src/main/java/ai4se/harness/memory/FileMemoryStore.java` | Cherry-picked (unchanged) |
| `src/test/java/ai4se/harness/config/CredentialManagerTest.java` | Rewrote: 7 tests for .env functionality |
| `src/test/java/ai4se/harness/config/ConfigLoaderTest.java` | Added unknown-properties test |
| `src/test/java/ai4se/harness/memory/MemoryRetrieverTest.java` | Added session_latest + cross-session tests |

## Concerns

- **`getMaskedKey()` format**: The task example showed `sk-****-xxxx` but the literal instruction "first 3 chars + **** + last 4 chars" produces `sk-****xxxx` (no extra dash). Implemented the literal interpretation. The test verifies `****` is present and plain text is not exposed, which satisfies the security requirement.
- **Env var in tests**: `getKey()` checks `DEEPSEEK_API_KEY` env var first. Tests assume this env var is not set in the test environment (verified: not set). The `loadFromEnvFile()` method is package-private for direct testing without env var interference.
- **PR number**: Created as PR #4 (not #5) — the repo's PR numbering doesn't match the task sequence numbering.
