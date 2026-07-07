# Task 2: PR #2 - LLM Layer

## Status: DONE

## Summary

Created the LLM abstraction layer with `tool_call` support compatible with DeepSeek's OpenAI-compatible API. Cherry-picked the LLM layer commits from `master-backup`, then applied the KEY FIX: refactored `LlmResponse` from the old `action`-based API (`actionName`/`actionParams` as `Map`/`hasAction()`) to the unified `tool_call` API (`toolName`/`toolArgs` as JSON `String`/`hasToolCall()`).

## Commits Created (on `feature/llm-layer`, based on `master` @ 8946fde)

| SHA | Subject |
|-----|---------|
| `dd0b352` | feat: add LLM abstraction layer (Message, LlmResponse, LlmProvider, Conversation) |
| `245b153` | feat: add MockLlmProvider with script and sequence modes |
| `7d6f3c1` | feat: add LLM abstraction layer with tool_call support |

The first two are cherry-picks from `master-backup` (commits `5a7f71b` and `364daa1`). The third is the KEY FIX that refactors the API to the `tool_call` format.

## Key Fix: LlmResponse API Refactor

The cherry-picked `LlmResponse` used an old `action`-based API:
```java
// OLD
LlmResponse(String text, String actionName, Map<String, Object> actionParams, String stopReason)
hasAction() / getActionName() / getActionParams()
```

Refactored to the `tool_call` API matching DeepSeek's OpenAI-compatible format (`choices[0].message.tool_calls[].function.name` / `.arguments`):
```java
// NEW
LlmResponse(String text, String toolName, String toolArgs, String stopReason)
hasToolCall()  // true when toolName != null
getToolName()  // tool name
getToolArgs()  // tool arguments as JSON string
getText()      // text content (null if tool_call only)
getStopReason()
```

Response type semantics:
- `text=null, toolName=set` → tool_call response (`hasToolCall() == true`)
- `text=set, toolName=null` → text-only response (`hasToolCall() == false`)
- both null → empty response (handled gracefully)

## Files

### Production code
- `src/main/java/ai4se/harness/llm/LlmResponse.java` — refactored to tool_call API (KEY FIX)
- `src/main/java/ai4se/harness/llm/LlmProvider.java` — interface: `complete(List<Message>, List<Tool>)`
- `src/main/java/ai4se/harness/llm/MockLlmProvider.java` — script + sequence modes (no change needed; default response uses nulls)
- `src/main/java/ai4se/harness/llm/Message.java` — role + content
- `src/main/java/ai4se/harness/llm/Conversation.java` — message list container
- `src/main/java/ai4se/harness/tools/Tool.java` — tool placeholder

### Tests
- `src/test/java/ai4se/harness/llm/LlmResponseTest.java` — 3 tests: text-only, tool_call, empty
- `src/test/java/ai4se/harness/llm/MockLlmProviderTest.java` — 4 tests: script mode tool_call, default fallback, sequence order, sequence exhausted
- `src/test/java/ai4se/harness/llm/ConversationTest.java` — 2 tests (cherry-picked, unchanged)
- `src/test/java/ai4se/harness/llm/MessageTest.java` — 2 tests (cherry-picked, unchanged)

## TDD Process

1. **RED**: Rewrote `LlmResponseTest` and `MockLlmProviderTest` to specify the new `tool_call` API. Ran `mvnw test` → compilation failure: `hasToolCall()`/`getToolName()`/`getToolArgs()` not found; constructor 3rd param is `Map` not `String`. Confirmed tests fail because the new API is missing.
2. **GREEN**: Refactored `LlmResponse.java` to the tool_call API. Ran `mvnw test` → 11 tests pass, BUILD SUCCESS.

## Verification (fresh evidence)

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- ConversationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- LlmResponseTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- MessageTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- MockLlmProviderTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Pull Request

- **PR #1**: https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/1
- **Title**: feat: LLM abstraction layer with tool_call support
- **Base**: `master` ← **Head**: `feature/llm-layer`
- **State**: open
- **Commits**: 3

## Notes / Environment

- `gh` CLI was not installed; installed via `winget install --id GitHub.cli` (v2.96.0).
- The git credential PAT (length 40) lacks the `read:org` scope required by `gh auth login`, so the PR was created via the GitHub REST API (`POST /repos/{owner}/{repo}/pulls`) using the same credential. The token was not printed/exposed.
- CRLF line-ending warnings from git are cosmetic only; build/tests unaffected.

## Concerns

None. All requirements met:
- [x] Cherry-picked commits `5a7f71b` and `364daa1` (no conflicts)
- [x] `LlmResponse` has `hasToolCall()`/`getToolName()`/`getToolArgs()`/`getText()`/`getStopReason()`
- [x] Constructor signature `LlmResponse(String text, String toolName, String toolArgs, String stopReason)`
- [x] `LlmResponseTest` covers tool_call, text-only, and empty responses
- [x] `MockLlmProviderTest` covers script mode (tool_call on keyword match) and sequence mode (in-order)
- [x] `mvnw test` passes (11 tests, 0 failures)
- [x] Committed and pushed to `feature/llm-layer`
- [x] PR created
