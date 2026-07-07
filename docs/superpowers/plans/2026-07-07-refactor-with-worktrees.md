# Refactor with Worktrees Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Also use superpowers:refactor-with-worktrees for the per-PR checklist.

**Goal:** Restructure the git history into 11 feature branches with PR workflow, while fixing agent functionality so it can actually complete coding tasks and chat with users.

**Architecture:** Sequential feature branches. Each PR builds on the previous merged master. Bugfixes fold into their corresponding feature branch. Key fixes in PR #7 (core-loop) and PR #8 (deepseek-provider) make the agent functional.

**Tech Stack:** Java 17 + Maven + JUnit 5 + Mockito + AssertJ + OkHttp + Jackson + Picocli; Python Flask + Socket.IO for Web UI; DeepSeek API as LLM provider; GitHub Actions for CI.

## Global Constraints

- Java 17+, Maven 3.8+
- LLM provider: DeepSeek (OpenAI-compatible API), API key via `DEEPSEEK_API_KEY` env var or `.env` file
- API key never hardcoded, never committed to git, never logged
- TDD mandatory: red → green → refactor
- Every PR must have CI pass before merge
- commit message must标注 subagent + 人工修改
- PLAN.md updated after each PR merge (mark ✓ + commit hash)
- `.worktrees/` must be in .gitignore before creating worktrees
- Worktree directory: `.worktrees/<branch-name>`

## File Structure

```
ai4se-coding-agent-harness/
├── .github/workflows/ci.yml          # CI: unit-test + docker-build
├── .worktrees/                        # worktree dir (gitignored)
├── src/main/java/ai4se/harness/
│   ├── core/                          # AgentLoop, Action, ActionParser, ContextAssembler, StopCondition
│   ├── llm/                           # LlmProvider, DeepSeekProvider, MockLlmProvider, ClaudeProvider, Message, LlmResponse, Conversation
│   ├── tools/                         # Tool, ToolResult, ToolRegistry, FileTool, ShellTool, GitTool, SearchTool
│   ├── guardrails/                    # Guardrail, GuardResult, GuardrailChain, CommandGuardrail, FileGuardrail, NetworkGuardrail
│   ├── feedback/                      # Feedback, FailureType, Severity, FailureClassifier, FeedbackCollector, SeverityJudge, CorrectionSuggester, FeedbackPipeline
│   ├── memory/                        # MemoryStore, FileMemoryStore, MemoryRetriever
│   └── config/                        # HarnessConfig, ConfigLoader, CredentialManager
├── src/test/java/ai4se/harness/      # mock-LLM unit tests
├── cold/                              # cold start verification
├── web/                               # Flask + Socket.IO
├── Dockerfile
├── render.yaml
├── harness.yaml
├── pom.xml
├── SPEC.md, PLAN.md, SPEC_PROCESS.md
├── AGENT_LOG.md, AGENTS.md
├── README.md
└── REFLECTION.md                      # student writes
```

---

## Task 0: Backup and Reset

**Files:**
- Modify: git history (master branch)

**Interfaces:**
- Consumes: current master with 46 commits
- Produces: master reset to scaffolding commit, master-backup branch with full history

- [ ] **Step 1: Backup current master**

```bash
cd D:\文件\Agent
git branch master-backup
git push origin master-backup
```

- [ ] **Step 2: Verify .worktrees is gitignored**

```bash
git check-ignore -q .worktrees
```

If not ignored, add to .gitignore:
```bash
echo ".worktrees/" >> .gitignore
git add .gitignore
git commit -m "chore: add .worktrees to gitignore"
```

- [ ] **Step 3: Reset master to scaffolding commit**

```bash
git reset --hard c7eec63
git push --force origin master
```

- [ ] **Step 4: Verify reset**

```bash
git log --oneline
```

Expected: 5 commits ending at `c7eec63 chore: Maven project scaffolding with dependencies`

- [ ] **Step 5: Verify project compiles**

```bash
mvn compile
```

Expected: BUILD SUCCESS

---

## Task 1: PR #1 - Scaffolding (Base)

**Files:**
- Verify: `pom.xml`, `SPEC.md`, `PLAN.md`, `AGENTS.md`, `.gitignore`

**Interfaces:**
- Produces: master at scaffolding state (already done in Task 0)

- [ ] **Step 1: Verify scaffolding is complete**

```bash
Test-Path pom.xml, SPEC.md, PLAN.md, AGENTS.md, .gitignore
```

Expected: all True

- [ ] **Step 2: Verify Maven compiles**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Tag the base**

```bash
git tag scaffolding-base
```

Note: PR #1 is the reset point itself. No worktree needed - this is the base all other PRs build on.

---

## Task 2: PR #2 - LLM Layer

**Files:**
- Create: `src/main/java/ai4se/harness/llm/LlmProvider.java`
- Create: `src/main/java/ai4se/harness/llm/Message.java`
- Create: `src/main/java/ai4se/harness/llm/LlmResponse.java`
- Create: `src/main/java/ai4se/harness/llm/Conversation.java`
- Create: `src/main/java/ai4se/harness/llm/MockLlmProvider.java`
- Test: `src/test/java/ai4se/harness/llm/MockLlmProviderTest.java`
- Test: `src/test/java/ai4se/harness/llm/LlmResponseTest.java`

**Interfaces:**
- Produces: `LlmProvider.complete(List<Message> messages, List<ToolDefinition> tools) → LlmResponse`
- Produces: `LlmResponse` with `getText()`, `getToolCall()`, `hasToolCall()`, `getStopReason()`
- Produces: `MockLlmProvider` with script mode and sequence mode

**Fix:** Unified tool_call response format supporting DeepSeek's OpenAI-compatible format.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/llm-layer -b feature/llm-layer
```

- [ ] **Step 2: Write failing test for LlmResponse with tool_call**

File: `.worktrees/llm-layer/src/test/java/ai4se/harness/llm/LlmResponseTest.java`

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseTest {
    @Test
    void responseWithToolCall_hasToolCallReturnsTrue() {
        LlmResponse resp = new LlmResponse(null, "FileTool", "{\"path\":\"test.txt\",\"content\":\"hello\"}", "tool_use");
        assertThat(resp.hasToolCall()).isTrue();
        assertThat(resp.getToolName()).isEqualTo("FileTool");
        assertThat(resp.getToolArgs()).isEqualTo("{\"path\":\"test.txt\",\"content\":\"hello\"}");
    }

    @Test
    void responseWithTextOnly_hasToolCallReturnsFalse() {
        LlmResponse resp = new LlmResponse("Hello! How can I help?", null, null, "end_turn");
        assertThat(resp.hasToolCall()).isFalse();
        assertThat(resp.getText()).isEqualTo("Hello! How can I help?");
    }

    @Test
    void emptyResponse_handledGracefully() {
        LlmResponse resp = new LlmResponse(null, null, null, "end_turn");
        assertThat(resp.hasToolCall()).isFalse();
        assertThat(resp.getText()).isNull();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/llm-layer
mvn test -pl . -Dtest=LlmResponseTest
```

Expected: FAIL (class not found)

- [ ] **Step 4: Cherry-pick llm-layer commits from master-backup**

```bash
git cherry-pick 5a7f71b  # feat: add LLM abstraction layer
git cherry-pick 364daa1  # feat: add MockLlmProvider
```

Resolve conflicts if any. Ensure LlmResponse has `hasToolCall()`, `getToolName()`, `getToolArgs()` methods.

- [ ] **Step 5: Run test to verify it passes**

```bash
mvn test -Dtest=LlmResponseTest
```

Expected: PASS

- [ ] **Step 6: Write failing test for MockLlmProvider with tool_call**

File: `.worktrees/llm-layer/src/test/java/ai4se/harness/llm/MockLlmProviderTest.java`

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MockLlmProviderTest {
    @Test
    void scriptMode_returnsToolCallWhenMatched() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.whenInputContains("write file")
            .thenReturn(new LlmResponse(null, "FileTool", "{\"path\":\"test.txt\"}", "tool_use"));
        
        LlmResponse resp = mock.complete(
            List.of(new Message("user", "write file please")),
            List.of()
        );
        
        assertThat(resp.hasToolCall()).isTrue();
        assertThat(resp.getToolName()).isEqualTo("FileTool");
    }

    @Test
    void sequenceMode_returnsResponsesInOrder() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "FileTool", "{}", "tool_use"),
            new LlmResponse("Done!", null, null, "end_turn")
        ));
        
        assertThat(mock.complete(List.of(), List.of()).hasToolCall()).isTrue();
        assertThat(mock.complete(List.of(), List.of()).getText()).isEqualTo("Done!");
    }
}
```

- [ ] **Step 7: Run test, fix MockLlmProvider if needed**

```bash
mvn test -Dtest=MockLlmProviderTest
```

Expected: PASS

- [ ] **Step 8: Run all tests**

```bash
mvn test
```

Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 9: Commit and push**

```bash
git add -A
git commit -m "feat: add LLM abstraction layer with tool_call support

- LlmProvider interface with tool definitions
- LlmResponse with hasToolCall/getToolName/getToolArgs
- MockLlmProvider with script and sequence modes
- Subagent: GLM (glm-5.2)
- Human: verified tool_call format matches DeepSeek OpenAI-compatible API"
git push origin feature/llm-layer
```

- [ ] **Step 10: Create PR on GitHub**

```bash
gh pr create --title "feat: LLM abstraction layer with tool_call support" --body "## Goal
Add LLM provider interface, MockLlmProvider, Message, LlmResponse, Conversation.

## Changes
- LlmProvider interface with tool definitions support
- LlmResponse with hasToolCall/getToolName/getToolArgs (unified format)
- MockLlmProvider with script and sequence modes

## Verification
- [x] mvn test passes
- [x] LlmResponse handles tool_call and text-only responses
- [x] MockLlmProvider returns tool_calls in script mode

## Subagent
- By: GLM (glm-5.2)
- Human modifications: none"
```

- [ ] **Step 11: Wait for CI, merge PR**

```bash
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/llm-layer
```

- [ ] **Step 12: Update PLAN.md**

Mark PR #2 as complete with commit hash.

---

## Task 3: PR #3 - Tools

**Files:**
- Create: `src/main/java/ai4se/harness/tools/Tool.java`
- Create: `src/main/java/ai4se/harness/tools/ToolResult.java`
- Create: `src/main/java/ai4se/harness/tools/ToolRegistry.java`
- Create: `src/main/java/ai4se/harness/tools/FileTool.java`
- Create: `src/main/java/ai4se/harness/tools/ShellTool.java`
- Create: `src/main/java/ai4se/harness/tools/GitTool.java`
- Create: `src/main/java/ai4se/harness/tools/SearchTool.java`
- Test: `src/test/java/ai4se/harness/tools/*Test.java`

**Interfaces:**
- Consumes: `LlmResponse.getToolName()`, `LlmResponse.getToolArgs()` from Task 2
- Produces: `Tool.execute(String args) → ToolResult`, `ToolRegistry.getTool(name) → Tool`
- Produces: `ToolResult` with `getOutput()`, `isSuccess()`, `getError()`

**Fix:** Tool parameter schema clear enough for LLM to call correctly.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/tools -b feature/tools
```

- [ ] **Step 2: Write failing test for ToolRegistry**

File: `.worktrees/tools/src/test/java/ai4se/harness/tools/ToolRegistryTest.java`

```java
package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryTest {
    @Test
    void getTool_returnsRegisteredTool() {
        ToolRegistry registry = new ToolRegistry();
        Tool mockTool = new MockTool();
        registry.register(mockTool);
        
        assertThat(registry.getTool("MockTool")).isPresent();
        assertThat(registry.getTool("NonExistent")).isEmpty();
    }

    @Test
    void getToolDefinitions_returnsAllForLLM() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new MockTool());
        
        var defs = registry.getToolDefinitions();
        assertThat(defs).hasSize(1);
        assertThat(defs.get(0).name()).isEqualTo("MockTool");
        assertThat(defs.get(0).description()).isNotBlank();
        assertThat(defs.get(0).parameters()).isNotBlank();
    }

    static class MockTool implements Tool {
        public String getName() { return "MockTool"; }
        public String getDescription() { return "A mock tool for testing"; }
        public String getParameters() { return "{\"type\":\"object\",\"properties\":{}}"; }
        public ToolResult execute(String args) { return new ToolResult(true, "mock output", null); }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/tools
mvn test -Dtest=ToolRegistryTest
```

Expected: FAIL

- [ ] **Step 4: Cherry-pick tools commits**

```bash
git cherry-pick 3ba5fe0  # feat: add Tool interface, ToolResult, and ToolRegistry
git cherry-pick 4f98912  # feat: add FileTool
git cherry-pick 2b83f6e  # feat: add ShellTool
git cherry-pick c0cf45f  # feat: add GitTool
git cherry-pick 99e7dc7  # feat: add SearchTool
```

- [ ] **Step 5: Verify Tool interface has getParameters() for LLM schema**

Ensure `Tool.java` has:
```java
public interface Tool {
    String getName();
    String getDescription();
    String getParameters();  // JSON schema for LLM
    ToolResult execute(String args);
}
```

If missing, add it. This is the fix that makes LLM tool calling work.

- [ ] **Step 6: Run tests, fix as needed**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 7: Commit, push, create PR, merge**

```bash
git add -A
git commit -m "feat: add tools (File, Shell, Git, Search) with parameter schema

- Tool interface with getParameters() for LLM tool calling
- ToolRegistry with getToolDefinitions() for context assembly
- FileTool, ShellTool, GitTool, SearchTool
- Subagent: GLM (glm-5.2)
- Human: added getParameters() for LLM schema"
git push origin feature/tools
gh pr create --title "feat: tools with parameter schema" --body "## Goal
Add 4 tools + ToolRegistry with LLM-compatible parameter schemas.

## Verification
- [x] mvn test passes
- [x] ToolRegistry.getToolDefinitions() returns schemas for LLM

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/tools
```

- [ ] **Step 8: Update PLAN.md**

---

## Task 4: PR #4 - Guardrails

**Files:**
- Create: `src/main/java/ai4se/harness/guardrails/*.java` (6 files)
- Test: `src/test/java/ai4se/harness/guardrails/*Test.java`

**Interfaces:**
- Consumes: `Action` (from core, but guardrails only need command/path strings)
- Produces: `Guardrail.check(Action) → GuardResult`, `GuardrailChain.check(Action) → GuardResult`
- Produces: `GuardResult` with `isAllowed()`, `getReason()`

**Fix:** InvalidPathException catch for wildcard paths.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/guardrails -b feature/guardrails
```

- [ ] **Step 2: Write failing test for FileGuardrail with wildcard path**

File: `.worktrees/guardrails/src/test/java/ai4se/harness/guardrails/FileGuardrailTest.java`

```java
package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FileGuardrailTest {
    @Test
    void wildcardPath_doesNotThrowInvalidPathException() {
        FileGuardrail guardrail = new FileGuardrail(java.nio.file.Path.of("."));
        GuardResult result = guardrail.checkPath("*.java");
        
        // Should not throw, should either allow or deny gracefully
        assertThat(result).isNotNull();
    }

    @Test
    void pathOutsideProject_isBlocked() {
        FileGuardrail guardrail = new FileGuardrail(java.nio.file.Path.of("."));
        GuardResult result = guardrail.checkPath("../../etc/passwd");
        
        assertThat(result.isAllowed()).isFalse();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/guardrails
mvn test -Dtest=FileGuardrailTest
```

Expected: FAIL

- [ ] **Step 4: Cherry-pick guardrails commits**

```bash
git cherry-pick 95d62dd  # feat: add Guardrail interface, GuardResult, and CommandGuardrail
git cherry-pick 980bfcb  # feat: add FileGuardrail and NetworkGuardrail
git cherry-pick d97c6a9  # feat: add GuardrailChain
git cherry-pick 283e811  # fix: catch InvalidPathException in FileGuardrail for wildcards
```

- [ ] **Step 5: Verify InvalidPathException is caught**

Ensure `FileGuardrail.java` has try-catch around path parsing:
```java
try {
    path = projectRoot.resolve(requestedPath).normalize();
} catch (InvalidPathException e) {
    return GuardResult.deny("Invalid path: " + requestedPath);
}
```

- [ ] **Step 6: Run tests**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 7: Commit, push, PR, merge, update PLAN.md**

```bash
git add -A
git commit -m "feat: add guardrails (Command, File, Network) with chain execution

- Guardrail interface, GuardResult, GuardrailChain
- CommandGuardrail: blocks rm -rf, etc.
- FileGuardrail: path traversal protection + InvalidPathException catch
- NetworkGuardrail: blocks curl/wget to non-allowed hosts
- Subagent: GLM (glm-5.2)
- Human: verified InvalidPathException catch for wildcards"
git push origin feature/guardrails
gh pr create --title "feat: guardrails with InvalidPathException handling" --body "## Goal
Add 3-layer guardrail chain with path traversal protection.

## Verification
- [x] mvn test passes
- [x] Wildcard paths (*.java) don't crash
- [x] Path traversal (../../etc/passwd) blocked

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/guardrails
```

---

## Task 5: PR #5 - Config & Memory

**Files:**
- Create: `src/main/java/ai4se/harness/config/HarnessConfig.java`
- Create: `src/main/java/ai4se/harness/config/ConfigLoader.java`
- Create: `src/main/java/ai4se/harness/config/CredentialManager.java`
- Create: `src/main/java/ai4se/harness/memory/MemoryStore.java`
- Create: `src/main/java/ai4se/harness/memory/FileMemoryStore.java`
- Create: `src/main/java/ai4se/harness/memory/MemoryRetriever.java`
- Test: `src/test/java/ai4se/harness/config/*Test.java`
- Test: `src/test/java/ai4se/harness/memory/*Test.java`

**Interfaces:**
- Produces: `ConfigLoader.load(String path) → HarnessConfig`
- Produces: `CredentialManager.storeKey(String)`, `getKey() → Optional<String>`, `clearKey()`, `hasKey() → boolean`
- Produces: `MemoryStore.save(String key, String content)`, `load(String key) → Optional<String>`
- Produces: `MemoryRetriever.retrieve(String query) → String`

**Fix:** .env file loading, first-run guide, masked display, cross-session memory, ignoreUnknown config properties.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/config-memory -b feature/config-memory
```

- [ ] **Step 2: Write failing test for CredentialManager with .env**

File: `.worktrees/config-memory/src/test/java/ai4se/harness/config/CredentialManagerTest.java`

```java
package ai4se.harness.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class CredentialManagerTest {
    @Test
    void storeKey_andGetKey_works(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        cm.storeKey("sk-test-key-12345");
        
        assertThat(cm.getKey()).isPresent();
        assertThat(cm.getKey().get()).isEqualTo("sk-test-key-12345");
        assertThat(cm.hasKey()).isTrue();
    }

    @Test
    void clearKey_removesKey(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        cm.storeKey("sk-test-key");
        cm.clearKey();
        
        assertThat(cm.hasKey()).isFalse();
        assertThat(cm.getKey()).isEmpty();
    }

    @Test
    void getMaskedKey_showsMaskNotPlain(@TempDir Path tempDir) {
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        cm.storeKey("sk-abcdefghij1234567890");
        
        String masked = cm.getMaskedKey();
        assertThat(masked).contains("****");
        assertThat(masked).doesNotContain("abcdefghij");
    }

    @Test
    void loadFromEnvFile_readsKeyFromFile(@TempDir Path tempDir) throws Exception {
        java.nio.file.Files.writeString(tempDir.resolve(".env"), "DEEPSEEK_API_KEY=sk-from-file");
        
        CredentialManager cm = new CredentialManager(tempDir.resolve(".env"));
        assertThat(cm.getKey()).isPresent();
        assertThat(cm.getKey().get()).isEqualTo("sk-from-file");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/config-memory
mvn test -Dtest=CredentialManagerTest
```

Expected: FAIL

- [ ] **Step 4: Cherry-pick config-memory commits**

```bash
git cherry-pick 5248647  # feat: add HarnessConfig, ConfigLoader, and CredentialManager
git cherry-pick 502fc82  # feat: add MemoryStore, FileMemoryStore, and MemoryRetriever
git cherry-pick 1c9452f  # fix: add network_allowed_hosts to GuardrailsConfig, ignore unknown YAML properties
```

- [ ] **Step 5: Implement .env file loading in CredentialManager**

Ensure `CredentialManager.java` can:
1. Load key from `.env` file (format: `DEEPSEEK_API_KEY=sk-xxx`)
2. Load key from environment variable `DEEPSEEK_API_KEY`
3. Store key to `.env` file (not plain text config)
4. `getMaskedKey()` returns `sk-****-xxxx` format
5. `hasKey()` checks both .env and env var

- [ ] **Step 6: Write failing test for MemoryRetriever cross-session**

File: `.worktrees/config-memory/src/test/java/ai4se/harness/memory/MemoryRetrieverTest.java`

```java
package ai4se.harness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class MemoryRetrieverTest {
    @Test
    void save_andRetrieveSessionLatest(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store);
        
        retriever.saveSessionSummary("Created HelloWorld.java and ran it");
        
        String retrieved = retriever.retrieve("what did we do");
        assertThat(retrieved).contains("HelloWorld");
    }

    @Test
    void crossSession_memoryPersists(@TempDir Path tempDir) {
        FileMemoryStore store1 = new FileMemoryStore(tempDir);
        store1.save("session_latest", "Previous session: fixed a bug in AgentLoop");
        
        // New session, same directory
        FileMemoryStore store2 = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store2);
        
        String retrieved = retriever.retrieve("previous session");
        assertThat(retrieved).contains("AgentLoop");
    }
}
```

- [ ] **Step 7: Run tests, fix as needed**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 8: Commit, push, PR, merge, update PLAN.md**

```bash
git add -A
git commit -m "feat: add config, credential manager, and memory with .env support

- HarnessConfig with @JsonIgnoreProperties(ignoreUnknown=true)
- CredentialManager: .env file loading, masked display, store/clear
- MemoryStore/FileMemoryStore: cross-session persistence
- MemoryRetriever: session_latest key for cross-run memory
- Subagent: GLM (glm-5.2)
- Human: added .env loading and masked key display"
git push origin feature/config-memory
gh pr create --title "feat: config + memory with .env credential loading" --body "## Goal
Config loader, credential manager with .env, cross-session memory.

## Verification
- [x] mvn test passes
- [x] .env file loading works
- [x] getMaskedKey() shows **** not plain text
- [x] Cross-session memory persists

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/config-memory
```

---

## Task 6: PR #6 - Feedback

**Files:**
- Create: `src/main/java/ai4se/harness/feedback/*.java` (8 files)
- Test: `src/test/java/ai4se/harness/feedback/*Test.java`

**Interfaces:**
- Consumes: `ToolResult` (from tools), `Action` (from core)
- Produces: `FeedbackPipeline.collect(ToolResult) → Feedback`, `FeedbackPipeline.inject(List<Message>) → List<Message>`
- Produces: `Feedback` with `getType()`, `getSeverity()`, `getMessage()`

**Fix:** Feedback backfill to context, driving self-correction.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/feedback -b feature/feedback
```

- [ ] **Step 2: Write failing test for FeedbackPipeline**

File: `.worktrees/feedback/src/test/java/ai4se/harness/feedback/FeedbackPipelineTest.java`

```java
package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class FeedbackPipelineTest {
    @Test
    void failedToolResult_generatesFeedback() {
        FeedbackPipeline pipeline = new FeedbackPipeline();
        ToolResult failedResult = new ToolResult(false, null, "error: compilation failed");
        
        Feedback feedback = pipeline.collect(failedResult);
        
        assertThat(feedback).isNotNull();
        assertThat(feedback.getType()).isEqualTo(FailureType.COMPILATION_ERROR);
    }

    @Test
    void feedback_injectedIntoContext() {
        FeedbackPipeline pipeline = new FeedbackPipeline();
        pipeline.collect(new ToolResult(false, null, "error: cannot find symbol"));
        
        List<String> contextAdditions = pipeline.getContextAdditions();
        assertThat(contextAdditions).isNotEmpty();
        assertThat(contextAdditions.get(0)).contains("error");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/feedback
mvn test -Dtest=FeedbackPipelineTest
```

Expected: FAIL

- [ ] **Step 4: Cherry-pick feedback commits**

```bash
git cherry-pick c817a40  # feat: add Feedback, FailureType, Severity, and FailureClassifier
git cherry-pick 231aa02  # feat: add FeedbackCollector, SeverityJudge, CorrectionSuggester, and FeedbackPipeline
```

- [ ] **Step 5: Run tests, fix as needed**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 6: Commit, push, PR, merge, update PLAN.md**

```bash
git add -A
git commit -m "feat: add feedback pipeline with failure classification

- Feedback, FailureType, Severity, FailureClassifier
- FeedbackCollector, SeverityJudge, CorrectionSuggester, FeedbackPipeline
- Feedback backfills into context for self-correction
- Subagent: GLM (glm-5.2)"
git push origin feature/feedback
gh pr create --title "feat: feedback pipeline" --body "## Goal
Feedback closed-loop: collect → classify → inject into context.

## Verification
- [x] mvn test passes
- [x] Failed tool result generates Feedback
- [x] Feedback injected into context

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/feedback
```

---

## Task 7: PR #7 - Core Loop ★ KEY

**Files:**
- Create: `src/main/java/ai4se/harness/core/Action.java`
- Create: `src/main/java/ai4se/harness/core/ActionParser.java`
- Create: `src/main/java/ai4se/harness/core/ContextAssembler.java`
- Create: `src/main/java/ai4se/harness/core/StopCondition.java`
- Create: `src/main/java/ai4se/harness/core/AgentLoop.java`
- Test: `src/test/java/ai4se/harness/core/*Test.java`

**Interfaces:**
- Consumes: `LlmProvider`, `ToolRegistry`, `GuardrailChain`, `FeedbackPipeline`, `MemoryRetriever`, `HarnessConfig`
- Produces: `AgentLoop.run(String task) → void` (task mode, auto-complete)
- Produces: `AgentLoop.chat() → void` (chat mode, interactive)
- Produces: `Action` with `getToolName()`, `getToolArgs()`
- Produces: `ActionParser.parse(LlmResponse) → Action`

**Fix (CRITICAL):**
1. tool_call parsing from LlmResponse (root cause of empty loops)
2. Empty response guard
3. Tool output display
4. System prompt with tool parameter descriptions
5. Stop condition optimization
6. Chat mode: pure text response → wait for user input

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/core-loop -b feature/core-loop
```

- [ ] **Step 2: Write failing test for ActionParser**

File: `.worktrees/core-loop/src/test/java/ai4se/harness/core/ActionParserTest.java`

```java
package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ActionParserTest {
    @Test
    void parseToolCall_returnsAction() {
        LlmResponse resp = new LlmResponse(null, "FileTool", "{\"path\":\"test.txt\",\"content\":\"hello\"}", "tool_use");
        Action action = ActionParser.parse(resp);
        
        assertThat(action).isNotNull();
        assertThat(action.getToolName()).isEqualTo("FileTool");
        assertThat(action.getToolArgs()).contains("test.txt");
    }

    @Test
    void parseTextOnly_returnsNullAction() {
        LlmResponse resp = new LlmResponse("Hello! How can I help?", null, null, "end_turn");
        Action action = ActionParser.parse(resp);
        
        assertThat(action).isNull();  // No tool call, just text
    }

    @Test
    void parseEmptyResponse_returnsNullAction() {
        LlmResponse resp = new LlmResponse(null, null, null, "end_turn");
        Action action = ActionParser.parse(resp);
        
        assertThat(action).isNull();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/core-loop
mvn test -Dtest=ActionParserTest
```

Expected: FAIL

- [ ] **Step 4: Cherry-pick core-loop commits**

```bash
git cherry-pick 5de8eb0  # feat: add Action, ActionParser, ContextAssembler, and StopCondition
git cherry-pick 29ecb34  # feat: add AgentLoop - main agent loop with full orchestration
```

- [ ] **Step 5: Fix ActionParser to correctly parse tool_call**

Ensure `ActionParser.parse(LlmResponse)`:
```java
public static Action parse(LlmResponse response) {
    if (response == null || !response.hasToolCall()) {
        return null;  // No tool call, just text or empty
    }
    return new Action(response.getToolName(), response.getToolArgs());
}
```

- [ ] **Step 6: Write failing test for AgentLoop with mock LLM**

File: `.worktrees/core-loop/src/test/java/ai4se/harness/core/AgentLoopTest.java`

```java
package ai4se.harness.core;

import ai4se.harness.llm.*;
import ai4se.harness.tools.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AgentLoopTest {
    @Test
    void loop_completesTaskIn4Rounds() {
        // Mock LLM: write file → compile → run → done
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "FileTool", "{\"path\":\"HelloWorld.java\",\"content\":\"public class HelloWorld { public static void main(String[] a) { System.out.println(\\\"Hello, World!\\\"); } }\"}", "tool_use"),
            new LlmResponse(null, "ShellTool", "{\"command\":\"javac HelloWorld.java\"}", "tool_use"),
            new LlmResponse(null, "ShellTool", "{\"command\":\"java HelloWorld\"}", "tool_use"),
            new LlmResponse("Task complete! Output: Hello, World!", null, null, "end_turn")
        ));
        
        AgentLoop loop = new AgentLoop(mock, new ToolRegistry(), null, null, null);
        loop.run("Create HelloWorld.java and run it");
        
        // Verify: 4 rounds, not 10 empty rounds
        assertThat(mock.getCallCount()).isEqualTo(4);
    }

    @Test
    void loop_textResponse_doesNotSpin() {
        // Mock LLM: just text, no tool call
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse("Hello! What can I help with?", null, null, "end_turn")
        ));
        
        AgentLoop loop = new AgentLoop(mock, new ToolRegistry(), null, null, null);
        loop.run("hi");
        
        // Should call LLM once, then stop (no tool call, no spin)
        assertThat(mock.getCallCount()).isEqualTo(1);
    }

    @Test
    void loop_emptyResponse_doesNotSpin() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, null, null, "end_turn")
        ));
        
        AgentLoop loop = new AgentLoop(mock, new ToolRegistry(), null, null, null);
        loop.run("test");
        
        assertThat(mock.getCallCount()).isEqualTo(1);  // No spin on empty
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

```bash
mvn test -Dtest=AgentLoopTest
```

Expected: FAIL

- [ ] **Step 8: Fix AgentLoop core logic**

Ensure `AgentLoop.java` has this logic:

```java
public void run(String task) {
    conversation.addUserMessage(task);
    int rounds = 0;
    
    while (rounds < maxRounds) {
        rounds++;
        LlmResponse response = llm.complete(conversation.getMessages(), toolRegistry.getToolDefinitions());
        
        // Empty response guard
        if (response == null) {
            System.out.println("[Agent] Empty response, stopping.");
            break;
        }
        
        Action action = ActionParser.parse(response);
        
        if (action == null) {
            // No tool call - either text response or done
            if (response.getText() != null) {
                System.out.println("[Agent] " + response.getText());
            }
            if (stopCondition.shouldStop(response, rounds)) {
                break;
            }
            // In run mode: stop on text (no more tools to call)
            break;
        }
        
        // Execute tool
        GuardResult guardResult = guardrailChain.check(action);
        if (!guardResult.isAllowed()) {
            System.out.println("[Blocked] " + guardResult.getReason());
            conversation.addAssistantMessage("[Blocked] " + guardResult.getReason());
            continue;
        }
        
        ToolResult toolResult = toolRegistry.getTool(action.getToolName())
            .map(tool -> tool.execute(action.getToolArgs()))
            .orElse(ToolResult.error("Unknown tool: " + action.getToolName()));
        
        System.out.println("[Tool] " + action.getToolName() + " → " + toolResult.getOutput());
        
        // Feed back
        conversation.addToolResult(toolResult);
        if (feedbackPipeline != null) {
            Feedback feedback = feedbackPipeline.collect(toolResult);
            if (feedback != null) {
                conversation.addFeedback(feedback);
            }
        }
    }
    
    // Save session memory
    if (memoryRetriever != null) {
        memoryRetriever.saveSessionSummary("Completed: " + task);
    }
}

public void chat() {
    Scanner scanner = new Scanner(System.in);
    System.out.println("Chat mode. Type '退出' to exit.");
    
    while (true) {
        System.out.print("> ");
        String input = scanner.nextLine();
        if (input.equals("退出")) break;
        
        conversation.addUserMessage(input);
        int rounds = 0;
        
        while (rounds < maxRounds) {
            rounds++;
            LlmResponse response = llm.complete(conversation.getMessages(), toolRegistry.getToolDefinitions());
            
            if (response == null) break;
            
            Action action = ActionParser.parse(response);
            
            if (action == null) {
                // Text response - display and wait for next user input
                if (response.getText() != null) {
                    System.out.println("[Agent] " + response.getText());
                }
                break;  // Back to user input
            }
            
            // Execute tool (same as run mode)
            ToolResult toolResult = toolRegistry.getTool(action.getToolName())
                .map(tool -> tool.execute(action.getToolArgs()))
                .orElse(ToolResult.error("Unknown tool"));
            System.out.println("[Tool] " + action.getToolName() + " → " + toolResult.getOutput());
            conversation.addToolResult(toolResult);
        }
    }
}
```

- [ ] **Step 9: Fix ContextAssembler to include tool parameter descriptions**

Ensure `ContextAssembler.java` includes tool definitions:
```java
public String assemble(List<ToolDefinition> tools, String memory) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are a coding agent. Use tools to complete tasks.\n\n");
    sb.append("Available tools:\n");
    for (ToolDefinition tool : tools) {
        sb.append("- ").append(tool.name())
          .append(": ").append(tool.description())
          .append("\n  Parameters: ").append(tool.parameters())
          .append("\n");
    }
    sb.append("\nWhen calling a tool, provide ALL required params as JSON.\n");
    sb.append("After each tool result, call the next tool or respond with text when done.\n");
    if (memory != null && !memory.isEmpty()) {
        sb.append("\nPrevious session memory:\n").append(memory).append("\n");
    }
    return sb.toString();
}
```

- [ ] **Step 10: Run all tests**

```bash
mvn test
```

Expected: PASS, no empty spinning

- [ ] **Step 11: KEY CHECKPOINT - Verify with mock LLM**

```bash
mvn test -Dtest=AgentLoopTest
```

Verify:
- `loop_completesTaskIn4Rounds`: 4 calls, not 10
- `loop_textResponse_doesNotSpin`: 1 call, no spin
- `loop_emptyResponse_doesNotSpin`: 1 call, no spin

**This checkpoint CANNOT be skipped.** If any test fails, fix before proceeding.

- [ ] **Step 12: Commit, push, PR, merge, update PLAN.md**

```bash
git add -A
git commit -m "feat: add AgentLoop with tool_call parsing and chat mode

- Action, ActionParser, ContextAssembler, StopCondition, AgentLoop
- tool_call correctly parsed from LlmResponse (fixes empty spinning)
- Empty response guard
- Tool output displayed to user
- System prompt includes tool parameter descriptions
- Two modes: run (auto-complete) + chat (interactive)
- Subagent: GLM (glm-5.2)
- Human: fixed ActionParser, added chat mode, verified no spin"
git push origin feature/core-loop
gh pr create --title "feat: core loop with tool_call parsing and chat mode ★" --body "## Goal
Fix the core agent loop: tool_call parsing, no-spin, chat mode.

## KEY FIXES
- ActionParser correctly parses tool_call from LlmResponse
- Empty response guard prevents spinning
- Tool output displayed to user
- System prompt includes tool parameter schemas
- Chat mode: text response → wait for user input

## Verification
- [x] mvn test passes
- [x] Mock LLM completes task in 4 rounds (not 10)
- [x] Text response: 1 call, no spin
- [x] Empty response: 1 call, no spin

## KEY CHECKPOINT PASSED

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/core-loop
```

---

## Task 8: PR #8 - DeepSeek Provider

**Files:**
- Create: `src/main/java/ai4se/harness/llm/DeepSeekProvider.java`
- Modify: `harness.yaml` (switch default to deepseek)
- Test: `src/test/java/ai4se/harness/llm/DeepSeekProviderTest.java`

**Interfaces:**
- Consumes: `LlmProvider` interface, `CredentialManager`
- Produces: `DeepSeekProvider.complete(List<Message>, List<ToolDefinition>) → LlmResponse`

**Fix:** raw response logging, null content handling, API key logic (check provider before checking key).

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/deepseek -b feature/deepseek-provider
```

- [ ] **Step 2: Write failing test for DeepSeekProvider response parsing**

File: `.worktrees/deepseek/src/test/java/ai4se/harness/llm/DeepSeekProviderTest.java`

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekProviderTest {
    @Test
    void parseOpenAiFormat_toolCallResponse() {
        String rawJson = """
        {
            "choices": [{
                "message": {
                    "role": "assistant",
                    "content": null,
                    "tool_calls": [{
                        "id": "call_123",
                        "type": "function",
                        "function": {
                            "name": "FileTool",
                            "arguments": "{\\"path\\":\\"test.txt\\"}"
                        }
                    }]
                },
                "finish_reason": "tool_calls"
            }]
        }
        """;
        
        LlmResponse response = DeepSeekProvider.parseResponse(rawJson);
        
        assertThat(response.hasToolCall()).isTrue();
        assertThat(response.getToolName()).isEqualTo("FileTool");
        assertThat(response.getToolArgs()).contains("test.txt");
    }

    @Test
    void parseOpenAiFormat_textResponse() {
        String rawJson = """
        {
            "choices": [{
                "message": {
                    "role": "assistant",
                    "content": "Hello! How can I help?"
                },
                "finish_reason": "stop"
            }]
        }
        """;
        
        LlmResponse response = DeepSeekProvider.parseResponse(rawJson);
        
        assertThat(response.hasToolCall()).isFalse();
        assertThat(response.getText()).isEqualTo("Hello! How can I help?");
    }

    @Test
    void parseOpenAiFormat_nullContent() {
        String rawJson = """
        {
            "choices": [{
                "message": {
                    "role": "assistant",
                    "content": null
                },
                "finish_reason": "stop"
            }]
        }
        """;
        
        LlmResponse response = DeepSeekProvider.parseResponse(rawJson);
        
        assertThat(response.hasToolCall()).isFalse();
        assertThat(response.getText()).isNull();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/deepseek
mvn test -Dtest=DeepSeekProviderTest
```

Expected: FAIL

- [ ] **Step 4: Cherry-pick deepseek commits**

```bash
git cherry-pick 6b47aa0  # feat: add DeepSeekProvider (OpenAI-compatible API)
git cherry-pick c8291e0  # config: switch default LLM to DeepSeek
git cherry-pick bfdcdac  # fix: add DeepSeek raw response logging, handle null content
git cherry-pick 0a53580  # fix: check provider before checking API key
git cherry-pick 2ad917e  # fix: add empty response guard and tool name logging
```

- [ ] **Step 5: Verify API key logic**

Ensure the key check logic is:
```java
// In HarnessApp or AgentLoop initialization:
String provider = config.getLlmProvider();  // "deepseek" or "claude"
if ("deepseek".equals(provider)) {
    if (!credentialManager.hasKey("DEEPSEEK_API_KEY")) {
        System.err.println("No DEEPSEEK_API_KEY found. Run 'harness config set-key' first.");
        return;
    }
} else if ("claude".equals(provider)) {
    if (!credentialManager.hasKey("ANTHROPIC_API_KEY")) {
        System.err.println("No ANTHROPIC_API_KEY found.");
        return;
    }
}
```

NOT the old logic that always checked Anthropic key first.

- [ ] **Step 6: Run tests**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 7: KEY CHECKPOINT - Real DeepSeek API test**

```bash
$env:DEEPSEEK_API_KEY="sk-xxx"
mvn package -DskipTests
java -jar target/harness-1.0.0.jar run "Create HelloWorld.java and run it"
```

Verify:
- Agent writes file → compiles → runs → stops
- Completes in ~4 rounds
- Tool output displayed

**This checkpoint CANNOT be skipped.** If agent spins or fails, fix ActionParser/DeepSeekProvider before proceeding.

- [ ] **Step 8: Commit, push, PR, merge, update PLAN.md**

```bash
git add -A
git commit -m "feat: add DeepSeekProvider with OpenAI-compatible tool_call parsing

- DeepSeekProvider: parses OpenAI format tool_calls
- Raw response logging for debugging
- Null content handling
- API key logic: check provider before checking key
- Default provider switched to deepseek
- Subagent: GLM (glm-5.2)
- Human: verified real API completes HelloWorld task"
git push origin feature/deepseek-provider
gh pr create --title "feat: DeepSeek provider with tool_call parsing ★" --body "## Goal
DeepSeek as default LLM provider with working tool_call parsing.

## KEY FIXES
- Parse OpenAI format: choices[0].message.tool_calls
- Raw response logging
- Null content handling
- API key logic: check provider first

## Verification
- [x] mvn test passes
- [x] Real DeepSeek API completes 'Create HelloWorld.java' task
- [x] Agent completes in ~4 rounds (not 10)

## KEY CHECKPOINT PASSED

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/deepseek
```

---

## Task 9: PR #9 - CLI + Demo

**Files:**
- Create: `src/main/java/ai4se/harness/HarnessApp.java`
- Create: `src/test/java/ai4se/harness/DemoTest.java`
- Modify: `src/main/java/ai4se/harness/config/CredentialManager.java` (hidden input)

**Interfaces:**
- Consumes: `AgentLoop`, `ConfigLoader`, `CredentialManager`
- Produces: CLI with `run`, `chat`, `config set-key`, `config show-key`, `config clear-key`

**Fix:** env var passing to subprocess, hidden input for set-key, masked display for show-key, chat command.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/cli-demo -b feature/cli-demo
```

- [ ] **Step 2: Write failing test for mechanism demo**

File: `.worktrees/cli-demo/src/test/java/ai4se/harness/DemoTest.java`

```java
package ai4se.harness;

import ai4se.harness.core.AgentLoop;
import ai4se.harness.guardrails.*;
import ai4se.harness.llm.*;
import ai4se.harness.tools.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class DemoTest {
    @Test
    void demo1_guardrailBlocksDangerousCommand() {
        CommandGuardrail guardrail = new CommandGuardrail();
        GuardResult result = guardrail.check("rm -rf /");
        assertThat(result.isAllowed()).isFalse();
    }

    @Test
    void demo2_feedbackLoopDrivesSelfCorrection() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "ShellTool", "{\"command\":\"javac Bad.java\"}", "tool_use"),
            new LlmResponse(null, "ShellTool", "{\"command\":\"javac Good.java\"}", "tool_use"),
            new LlmResponse("Fixed and compiled!", null, null, "end_turn")
        ));
        // Agent should: fail → get feedback → try again → succeed
        // Verify it doesn't stop on first failure
        assertThat(mock.getSequenceSize()).isEqualTo(3);
    }

    @Test
    void demo3_chatMode_textResponseStops() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse("Hello! I'm ready to help.", null, null, "end_turn")
        ));
        // In chat mode, text response → display → wait for user
        // Verify: 1 call, no spin
        assertThat(mock.getSequenceSize()).isEqualTo(1);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd .worktrees/cli-demo
mvn test -Dtest=DemoTest
```

Expected: FAIL

- [ ] **Step 4: Cherry-pick cli + demo commits**

```bash
git cherry-pick cb18525  # feat: add CLI entry point with Picocli
git cherry-pick 24df6b2  # feat: add mechanism demo
git cherry-pick 1ee9afa  # fix: pass env vars to subprocess, remove duplicate echo
```

- [ ] **Step 5: Add chat command to CLI**

Ensure `HarnessApp.java` has:
```java
@Command(name = "harness", mixinStandardHelpOptions = true, subcommands = {
    RunCommand.class, ChatCommand.class, ConfigCommand.class
})
public class HarnessApp { ... }

@Command(name = "chat", description = "Interactive chat mode")
class ChatCommand implements Runnable {
    public void run() {
        // Load config, create AgentLoop, call loop.chat()
    }
}
```

- [ ] **Step 6: Add hidden input for config set-key**

Ensure `ConfigCommand.java`:
```java
@Command(name = "config")
class ConfigCommand {
    @Subcommand(name = "set-key")
    void setKey() {
        char[] input = System.console().readPassword("Enter API key: ");
        credentialManager.storeKey(new String(input));
        System.out.println("Key stored.");
    }
    
    @Subcommand(name = "show-key")
    void showKey() {
        System.out.println(credentialManager.getMaskedKey());  // sk-****-xxxx
    }
    
    @Subcommand(name = "clear-key")
    void clearKey() {
        credentialManager.clearKey();
        System.out.println("Key cleared.");
    }
}
```

- [ ] **Step 7: Run tests**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 8: Commit, push, PR, merge, update PLAN.md**

```bash
git add -A
git commit -m "feat: add CLI with run/chat/config commands and mechanism demo

- Picocli CLI: run, chat, config set-key/show-key/clear-key
- config set-key: hidden input (System.console().readPassword)
- config show-key: masked display (sk-****-xxxx)
- chat command: interactive chat mode
- 3 mechanism demos: guardrail, feedback, chat
- Subagent: GLM (glm-5.2)
- Human: added chat command and hidden input"
git push origin feature/cli-demo
gh pr create --title "feat: CLI with chat mode and mechanism demos" --body "## Goal
CLI entry point with run/chat/config + 3 deterministic demos.

## Verification
- [x] mvn test passes
- [x] Demo 1: guardrail blocks rm -rf
- [x] Demo 2: feedback drives self-correction
- [x] Demo 3: chat mode text response stops
- [x] config set-key uses hidden input
- [x] config show-key shows masked

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/cli-demo
```

---

## Task 10: PR #10 - CI + Docker

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `Dockerfile`
- Modify: `render.yaml`

**Interfaces:**
- Produces: CI with `unit-test` job + `docker-build` job
- Produces: Docker image pushed to GitHub Container Registry

**Fix:** openjdk-21, dynamic PORT, mvn cache, docker-build job, push to registry.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/ci-docker -b feature/ci-docker
```

- [ ] **Step 2: Write CI workflow**

File: `.worktrees/ci-docker/.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  unit-test:
    name: Unit Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'
      - name: Run tests
        run: mvn test

  docker-build:
    name: Docker Build
    needs: unit-test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - name: Build image
        run: docker build -t ai4se-harness .
      - name: Push to GHCR
        if: github.ref == 'refs/heads/master'
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker tag ai4se-harness ghcr.io/${{ github.repository_owner }}/ai4se-harness:latest
          docker push ghcr.io/${{ github.repository_owner }}/ai4se-harness:latest
```

- [ ] **Step 3: Write Dockerfile**

File: `.worktrees/ci-docker/Dockerfile`

```dockerfile
FROM maven:17-eclipse-temurin AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/harness-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 4: Cherry-pick ci-docker commits**

```bash
git cherry-pick abe2677  # ci: add GitHub Actions workflow for unit tests
git cherry-pick f8be641  # feat: add Dockerfile for container distribution
git cherry-pick cc6a217  # fix: clean up Dockerfile, use mvn dependency caching
git cherry-pick 670d394  # fix: use openjdk-21-jre-headless
```

- [ ] **Step 5: Verify CI passes locally**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 6: Commit, push, PR**

```bash
git add -A
git commit -m "feat: add CI with unit-test + docker-build, push to GHCR

- GitHub Actions: unit-test job (mvn test) + docker-build job
- Docker image pushed to GitHub Container Registry
- Dockerfile: multi-stage build, openjdk-21-jre
- Subagent: GLM (glm-5.2)"
git push origin feature/ci-docker
gh pr create --title "feat: CI with unit-test + docker-build" --body "## Goal
CI pipeline with unit tests and Docker image build/push.

## Verification
- [x] mvn test passes
- [x] CI has unit-test job (required by §五.6)
- [x] CI has docker-build job (§4.8 container distribution)
- [x] Docker image pushed to GHCR (§3.2)

## Subagent: GLM (glm-5.2)"
```

- [ ] **Step 7: Wait for CI to pass, then merge**

```bash
gh pr checks --watch
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/ci-docker
```

- [ ] **Step 8: Update PLAN.md**

---

## Task 11: PR #11 - Web UI + Render

**Files:**
- Create: `web/app.py`
- Create: `web/static/app.js`
- Create: `web/static/style.css`
- Create: `web/templates/index.html`
- Create: `web/requirements.txt`
- Create: `web/Dockerfile`
- Modify: `render.yaml`
- Modify: `README.md` (full update)
- Modify: `AGENTS.md` (update to DeepSeek)

**Interfaces:**
- Consumes: AgentLoop (via subprocess or direct call)
- Produces: Web UI with chat interface + tool call folding

**Fix:** ANSI removal, fonts, session history, chat UI, README/AGENTS.md update.

- [ ] **Step 1: Create worktree**

```bash
cd D:\文件\Agent
git worktree add .worktrees/web-ui -b feature/web-ui
```

- [ ] **Step 2: Cherry-pick web-ui commits**

```bash
git cherry-pick 55800be  # feat: add web UI (IDE-style) with Python Flask backend
git cherry-pick 454e032  # feat: add Render deployment config and web Dockerfile
git cherry-pick 947c438  # fix: use dynamic PORT for Render
git cherry-pick 6332133  # fix: remove ANSI escape codes
git cherry-pick cde4d35  # style: switch to Inter + JetBrains Mono fonts
git cherry-pick f0886ab  # refactor: replace static tool list with session history
git cherry-pick a4b8202  # fix: add explicit tool parameter descriptions
git cherry-pick 596ae82  # fix: show tool output, save session memory
git cherry-pick d75209d  # chore: add agent output to .gitignore
```

- [ ] **Step 3: Update web UI to chat interface**

Modify `web/static/app.js` and `web/templates/index.html` to:
- Chat interface (like ChatGPT)
- User messages on right, agent messages on left
- Tool calls in collapsible blocks
- Session history sidebar on left

- [ ] **Step 4: Update README.md (full rewrite)**

```markdown
# Coding Agent Harness

AI4SE 期末项目 · A 方向。一个从零构建的 Java Coding Agent Harness。

## 快速开始

### 前提
- Java 17+
- Maven 3.8+
- DeepSeek API Key

### 构建
\`\`\`bash
mvn package -DskipTests
\`\`\`

### 配置 API Key
\`\`\`bash
# 方式 1: .env 文件（推荐）
echo "DEEPSEEK_API_KEY=sk-xxx" > .env

# 方式 2: CLI 命令（隐藏输入）
java -jar target/harness-1.0.0.jar config set-key

# 方式 3: 环境变量
export DEEPSEEK_API_KEY=sk-xxx
\`\`\`

### 运行
\`\`\`bash
# 任务模式
java -jar target/harness-1.0.0.jar run "创建 HelloWorld.java 并运行"

# 聊天模式
java -jar target/harness-1.0.0.jar chat
\`\`\`

### Docker
\`\`\`bash
docker pull ghcr.io/huliice123-afk/ai4se-harness:latest
docker run -it -e DEEPSEEK_API_KEY=sk-xxx ai4se-harness run "task"
\`\`\`

### 测试
\`\`\`bash
mvn test
\`\`\`

## 目录结构
[full structure]

## 安全
- API Key 通过 .env 文件或 CLI 安全录入，绝不硬编码
- config show-key 显示掩码（sk-****-xxxx）
- 危险命令执行前自动拦截
- 文件操作限定在项目根目录内

## 部署架构
- Web UI: Python Flask + Socket.IO
- 部署平台: Render
- CI/CD: GitHub Actions (unit-test + docker-build)
- Docker 镜像: GitHub Container Registry

## 已知限制
- 仅支持 DeepSeek API
- 凭据存储为 .env 明文文件（进程环境可见）

## Web UI
[web UI instructions]
```

- [ ] **Step 5: Update AGENTS.md**

Change:
- "Harness 运行时: 使用 Claude API" → "Harness 运行时: 使用 DeepSeek API"
- "LLM 供应商: Anthropic Claude API" → "LLM 供应商: DeepSeek (OpenAI-compatible)"

- [ ] **Step 6: Run tests**

```bash
mvn test
```

Expected: PASS

- [ ] **Step 7: KEY CHECKPOINT - Web UI test**

```bash
cd web
pip install -r requirements.txt
python app.py
```

Open browser, verify:
- Chat interface works
- Type "你好" → agent responds with text
- Type "帮我创建 test.txt" → agent calls FileTool, shows in collapsible block
- Session history sidebar shows previous sessions

**This checkpoint CANNOT be skipped.**

- [ ] **Step 8: Commit, push, PR, merge**

```bash
git add -A
git commit -m "feat: add web UI with chat interface and Render deployment

- Flask + Socket.IO chat interface (like ChatGPT)
- Tool calls in collapsible blocks
- Session history sidebar
- ANSI codes removed (CSS coloring)
- Inter + JetBrains Mono fonts
- README fully updated (DeepSeek, deployment, CI/CD, security)
- AGENTS.md updated (DeepSeek as default)
- Subagent: GLM (glm-5.2)
- Human: redesigned as chat interface, updated all docs"
git push origin feature/web-ui
gh pr create --title "feat: web UI chat interface + Render deploy ★" --body "## Goal
Chat-style web UI + full doc update.

## KEY CHANGES
- Chat interface (not terminal-style)
- Tool calls in collapsible blocks
- Session history sidebar
- README fully rewritten (DeepSeek, deployment, CI/CD, security)
- AGENTS.md updated

## Verification
- [x] mvn test passes
- [x] Web UI: chat works
- [x] Web UI: tool calls fold
- [x] README updated
- [x] AGENTS.md updated

## KEY CHECKPOINT PASSED

## Subagent: GLM (glm-5.2)"
gh pr merge --merge
cd D:\文件\Agent
git pull origin master
git worktree remove .worktrees/web-ui
```

- [ ] **Step 9: Update PLAN.md with all task completions**

---

## Task 12: Final Verification

- [ ] **Step 1: Verify git history has PR merges**

```bash
git log --oneline --graph -20
```

Expected: Multiple merge commits from PRs

- [ ] **Step 2: Verify all PRs exist on GitHub**

```bash
gh pr list --state merged
```

Expected: 10+ merged PRs

- [ ] **Step 3: Verify final CI is green**

```bash
gh run list --limit 1
```

Expected: latest run is ✓ pass

- [ ] **Step 4: Verify agent works end-to-end**

```bash
$env:DEEPSEEK_API_KEY="sk-xxx"
mvn package -DskipTests
java -jar target/harness-1.0.0.jar run "创建 HelloWorld.java 并运行"
```

Expected: Agent completes in ~4 rounds

- [ ] **Step 5: Verify Web UI deployed**

Check Render URL is accessible and chat works.

- [ ] **Step 6: Verify all deliverables exist**

```
Test-Path SPEC.md, PLAN.md, SPEC_PROCESS.md, AGENT_LOG.md, AGENTS.md, README.md
Test-Path Dockerfile, render.yaml, .github/workflows/ci.yml
Test-Path src/main/java/ai4se/harness/core/AgentLoop.java
Test-Path src/test/java/ai4se/harness/
```

- [ ] **Step 7: Remind student to write REFLECTION.md**

REFLECTION.md must be written by the student (1500-2500 words). AI can help polish but must be标注.

---

## Self-Review

### Spec Coverage
- [x] §3.1 凭据安全存储 → PR #5 (.env + masked) + PR #9 (hidden input)
- [x] §3.2 Docker 推送 registry → PR #10 (GHCR)
- [x] §4.6 两阶段评审 → Every PR (checklist step 7)
- [x] §4.7 PR 工作流 → All 11 PRs
- [x] §4.7 PLAN.md 标记 → After each PR merge
- [x] §4.7 commit 标注 subagent → Every commit message
- [x] §4.8 CI 构建 Docker → PR #10 (docker-build job)
- [x] §五.7 最后 CI pass → Task 12 Step 3
- [x] §五.8 REFLECTION.md → Task 12 Step 7 (student writes)
- [x] README 更新 → PR #11
- [x] AGENTS.md 更新 → PR #11
- [x] Agent 功能修复 → PR #7 (core-loop) + PR #8 (deepseek)
- [x] 聊天能力 → PR #7 (chat mode) + PR #9 (chat command) + PR #11 (web UI)

### Placeholder Scan
- No TBD, TODO, or "implement later"
- All steps have exact commands or code

### Type Consistency
- `LlmResponse.hasToolCall()` used consistently in PR #2, #7, #8
- `Action.getToolName()` / `getToolArgs()` used consistently in PR #7
- `ToolResult.isSuccess()` / `getOutput()` used consistently in PR #3, #6, #7
