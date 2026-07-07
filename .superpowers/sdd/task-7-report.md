# Task 7: PR #7 - Core Loop ★ KEY

## Status: DONE

## Summary

Implemented the core agent loop — the most critical component that fixes the agent's empty spinning issue. The agent previously spun 10 rounds doing nothing because ActionParser couldn't parse tool calls from LlmResponse (it called non-existent methods `hasAction()`, `getActionName()`, `getActionParams()` instead of `hasToolCall()`, `getToolName()`, `getToolArgs()`).

## Commits Created

| SHA | Subject |
|-----|---------|
| b4c231b | feat: add Action, ActionParser, ContextAssembler, and StopCondition |
| 9d04662 | feat: add AgentLoop - main agent loop with full orchestration |
| 4bda6d6 | fix: improve test quality - ClaudeProvider mock server, robust ContextAssembler, full SeverityJudge coverage |
| 830e89a | fix: implement HITL flow, fix MemoryRetriever keys, add AgentLoop tests |
| 6cf2a16 | fix: add explicit tool parameter descriptions to system prompt |
| 2fffa05 | fix: add empty response guard and tool name logging to AgentLoop |
| 396099b | fix: show tool output, save session memory between runs |
| 70f88d6 | feat: add AgentLoop with tool_call parsing and chat mode (critical fixes) |

## Cherry-Pick Conflict Resolution

1. **ClaudeProvider.java / ClaudeProviderTest.java** (modify/delete): Files were deleted in master (replaced by different provider). Resolved by keeping deletion (`git rm`).
2. **.superpowers/sdd/progress.md** (modify/delete): Tracking file not in master. Resolved by keeping deletion.
3. **.mvn/wrapper/maven-wrapper.properties / mvnw.cmd** (add/add): Identical content on both sides. Resolved by taking HEAD version.
4. **MemoryRetriever.java** (content): HEAD had hardcoded key list, cherry-pick had configurable `keys` field. Took cherry-pick version (better design).
5. **MemoryRetrieverTest.java** (content): Cherry-pick added tests for custom keys feature. Took cherry-pick version.
6. **Unwanted new files**: Removed project description markdown files and duplicated `.mvn/.mvn/` path that came from master-backup.

## Critical Fixes Applied (commit 70f88d6)

### 1. ActionParser — tool_call parsing fixed
**Before (broken):**
```java
public Action parse(LlmResponse response) {
    if (response.hasAction()) {  // method doesn't exist!
        return new Action(response.getActionName(), response.getActionParams());  // methods don't exist!
    }
    return null;
}
```

**After (fixed):**
```java
public Action parse(LlmResponse response) {
    if (response == null || !response.hasToolCall()) {
        return null;
    }
    return new Action(response.getToolName(), response.getToolArgs());
}
```

### 2. AgentLoop — no-spin on text/empty responses
**Before (spinning):** On text response, the loop did `continue` (spinning back to LLM).
**After (fixed):** On text/empty response → `break` (stop in run mode).

Key logic:
```java
Action action = parser.parse(response);
if (action == null) {
    // No tool call - text or empty response
    if (text != null && !text.isBlank()) {
        System.out.println("[Agent] " + text);
    }
    break;  // KEY FIX: stop, don't continue
}
```

### 3. Chat mode added
Interactive `chat()` method with `Scanner` input. Type `退出` to exit. Text response returns to user input; tool calls execute inline.

### 4. ContextAssembler — tool parameter descriptions
**Before:** Hardcoded tool descriptions.
**After:** Uses `tool.getName()`, `tool.getDescription()`, `tool.getParameters()` from the Tool interface.

### 5. Action — String toolArgs (not Map)
Changed `Action` from `Map<String, Object> params` to `String toolArgs` to align with:
- `LlmResponse.getToolArgs()` returns `String`
- `Tool.execute(String args)` takes `String`
JSON is parsed to Map only for guardrail checks in AgentLoop.

### 6. Interface alignment fixes
- `tools.get()` → `tools.getTool()` (ToolRegistry API)
- `Severity.FATAL` → `Severity.CRITICAL` (enum value)
- `feedback.process()` → `feedback.collect()` (populates contextAdditions)
- Added `MockLlmProvider.getCallCount()` for test verification
- Added `ToolResult.error()` and `ToolResult.success()` static helpers

## Test Results

```
mvnw test → Tests run: 105, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### KEY CHECKPOINT TESTS (all passing)

| Test | Expectation | Result |
|------|------------|--------|
| `loop_completesTaskIn4Rounds` | 3 tool_calls + 1 text done → 4 calls, NOT 10 | ✅ 4 calls |
| `loop_textResponse_doesNotSpin` | text only → 1 call, no spin | ✅ 1 call no spin |
| `loop_emptyResponse_doesNotSpin` | empty → 1 call, no spin | ✅ 1 call no spin |
| `parseToolCall` | returns Action with correct name and args | ✅ |
| `parseTextOnly` | returns null Action | ✅ |
| `parseEmptyResponse` | returns null Action | ✅ |

### All Test Suites

| Suite | Tests | Status |
|-------|-------|--------|
| ActionParserTest | 4 | ✅ |
| AgentLoopTest | 8 | ✅ |
| ContextAssemblerTest | 2 | ✅ |
| StopConditionTest | 3 | ✅ |
| CorrectionSuggesterTest | 3 | ✅ |
| FailureClassifierTest | 7 | ✅ |
| FeedbackCollectorTest | 2 | ✅ |
| FeedbackPipelineTest | 5 | ✅ |
| SeverityJudgeTest | 8 | ✅ |
| CommandGuardrailTest | 4 | ✅ |
| FileGuardrailTest | 5 | ✅ |
| GuardrailChainTest | 5 | ✅ |
| NetworkGuardrailTest | 3 | ✅ |
| ConversationTest | 2 | ✅ |
| LlmResponseTest | 3 | ✅ |
| MessageTest | 2 | ✅ |
| MockLlmProviderTest | 4 | ✅ |
| FileMemoryStoreTest | 3 | ✅ |
| MemoryRetrieverTest | 7 | ✅ |
| FileToolTest | 4 | ✅ |
| GitToolTest | 3 | ✅ |
| SearchToolTest | 3 | ✅ |
| ShellToolTest | 3 | ✅ |
| ToolRegistryTest | 3 | ✅ |
| ConfigLoaderTest | + | ✅ |
| CredentialManagerTest | + | ✅ |
| **Total** | **105** | **All pass** |

## PR

- URL: https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/6
- Title: "feat: core loop with tool_call parsing and chat mode ★"
- Branch: `feature/core-loop` → `master`

## Concerns

1. **PR number mismatch**: Task says "PR #7" but GitHub assigned PR #6 (repo-level sequential numbering). This is expected — the task numbering doesn't match GitHub's PR numbering.
2. **packfile warnings**: Git operations showed `packfile index unavailable` warnings. These are non-fatal — all commits were applied correctly and the push succeeded.
3. **Chat mode not tested**: The `chat()` method uses `System.in` (Scanner) and is not unit-tested. It would require input stream mocking. The run mode is fully tested.
4. **FeedbackPipeline.getContextAdditions()**: Wired into conversation as `[CONTEXT]` messages after each tool execution. Since `collect()` is now used (instead of `process()`), contextAdditions is populated on failures.

## Subagent标注

- Subagent: GLM (glm-5.2)
- Human: fixed ActionParser, added chat mode, verified no spin

---

## Follow-up: Chat Mode Fixes (task-7b)

### Status: DONE

### Summary

Fixed four chat-mode / context-ordering issues in AgentLoop and ContextAssembler. The agent now retains conversation memory across chat turns, records its own tool calls in history (so the LLM can see what it previously invoked), wires the feedback pipeline and session-memory save into chat mode, and places the current task before the result/feedback history.

### Issues Fixed

1. **Chat mode resets history per turn** (`AgentLoop.chat()`): `Conversation history = new Conversation()` was instantiated inside the outer `while (true)` loop, wiping memory each user turn. FIX: moved the `Conversation` (and a `StringBuilder chatSummary`) outside the loop so a single instance persists across all turns.

2. **Assistant tool calls not recorded in history** (run + chat modes): Only `[RESULT]`/`[FEEDBACK]` user messages were appended — the LLM never saw which tool it had just called. FIX: after `parser.parse(response)` returns a non-null `Action`, add `history.add(new Message("assistant", "Calling " + action.getToolName() + " with " + action.getToolArgs()))` in both `run()` and `chat()`. Chat mode also now records assistant text responses (previously dropped).

3. **Chat mode: wire feedback pipeline and save session memory**: After tool execution in chat, call `feedback.collect(toolResult)` when `feedback != null`, branching on success (`[RESULT]`) vs failure (`[FEEDBACK] …`) and appending `feedback.getContextAdditions()` as `[CONTEXT]` messages — mirroring `run()`. On chat exit (`退出`), call `memory.saveSessionSummary("Chat session.\n" + chatSummary)`.

4. **Task placement in ContextAssembler**: Messages were ordered `[system, history…, task]`, pushing the current task to the end after all prior results/feedback. FIX: reordered to `[system, task, history…]` so the current task precedes the result/feedback history.

### Test Results

```
mvnw test → Tests run: 105, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 8 `AgentLoopTest` cases pass (incl. `shouldStopAfterMaxRounds`, `loop_completesTaskIn4Rounds`, `shouldBlockDangerousCommandAndReturnFailure` — verified the assistant-message addition does not alter round counts or stop behavior since it occurs after the `shouldStop` check). Both `ContextAssemblerTest` cases pass (empty-history assembly still yields `[system, task]` with task as the final message).

### Files Changed

| File | Change |
|------|--------|
| `src/main/java/ai4se/harness/core/AgentLoop.java` | +28/-3 — chat history persistence, assistant tool-call recording (run+chat), feedback wiring + memory save in chat |
| `src/main/java/ai4se/harness/core/ContextAssembler.java` | +1/-1 — task placed before history |

### Commit

| SHA | Subject |
|-----|---------|
| 47ef2e4 | fix: chat mode history, tool call recording, context ordering |

Pushed to `origin/feature/core-loop` (70f88d6..47ef2e4).
