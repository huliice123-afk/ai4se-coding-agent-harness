# Task 8 Report: PR #8 - DeepSeek Provider

## Status: DONE_WITH_CONCERNS

## Summary

Implemented DeepSeekProvider with OpenAI-compatible tool_call parsing, adapted to the current master branch's `LlmResponse` API (which diverged from master-backup). All 112 tests pass. PR created.

## Commits Created

| SHA | Subject |
|-----|---------|
| `7b41837` | feat: add DeepSeekProvider with OpenAI-compatible tool_call parsing |

## PR

- URL: https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/7
- Title: "feat: DeepSeek provider with tool_call parsing ★"
- Base: master ← Head: feature/deepseek-provider

## Test Summary

```
mvnw.cmd test
Tests run: 112, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

DeepSeekProviderTest: 7 tests, all passing:
- `parseOpenAiFormat_toolCallResponse` - parses tool_calls correctly (name, args, stopReason)
- `parseOpenAiFormat_textResponse` - parses text content correctly
- `parseOpenAiFormat_nullContent` - handles null content gracefully (returns "")
- `shouldConstructWithApiKey` - construction
- `completeShouldParseToolUseResponse` - full flow via MockWebServer
- `completeShouldParseTextResponse` - full flow via MockWebServer
- `completeShouldHandleErrorResponse` - error handling via MockWebServer

## What Was Done

### 1. DeepSeekProvider.java (KEY DELIVERABLE)
- `public static LlmResponse parseResponse(String rawJson)` - static method as required
- Parses OpenAI format:
  - `tool_calls[0].function.name` → `LlmResponse.toolName`
  - `tool_calls[0].function.arguments` → `LlmResponse.toolArgs` (raw JSON string)
  - `message.content` → `LlmResponse.text` (when no tool_calls)
  - `finish_reason` → `LlmResponse.stopReason`
- Null content handling: returns empty string ""
- Raw response logging to stderr when no tool call and blank text
- Uses `tool.getName()`/`tool.getDescription()`/`tool.getParameters()` (current Tool API)

### 2. HarnessApp.java (CRITICAL FIX #3)
- CLI entry point using Picocli (pom.xml already declared mainClass)
- **Provider-before-key logic**: checks `config.getLlm().getProvider()` FIRST, then checks the appropriate API key within each branch
  - `"deepseek"` → checks DEEPSEEK_API_KEY via CredentialManager
  - `"claude"` → checks ANTHROPIC_API_KEY via System.getenv
  - unknown → error
- This fixes the bug where Anthropic key was checked unconditionally first, blocking DeepSeek

### 3. ClaudeProvider.java (dependency)
- Brought in from master-backup, adapted to current API
- `toolArgs` serialized to JSON String (not Map) to match current LlmResponse
- Necessary dependency for HarnessApp to compile (claude branch)

### 4. harness.yaml
- Default provider switched: `claude` → `deepseek`
- Default model: `claude-sonnet-4-20250514` → `deepseek-chat`

### 5. render.yaml
- Created with DEEPSEEK_API_KEY (and ANTHROPIC_API_KEY) env vars

### 6. DeepSeekProviderTest.java
- 7 tests: 3 required (parseOpenAiFormat_*) + 4 MockWebServer-based

## Concerns

### 1. Codebase Divergence (MAIN CONCERN)
The current master branch (db3a94e) and master-backup diverged significantly on the `LlmResponse` API:
- **Current master**: `tool` terminology — `hasToolCall()`, `getToolName()`, `getToolArgs()` (String), `getText()`, `getStopReason()`
- **master-backup**: `action` terminology — `hasAction()`, `getActionName()`, `getActionParams()` (Map)

The cherry-picked commits were written against master-backup's API. Blindly cherry-picking would produce non-compiling code (Map passed where String expected; non-existent methods called). I attempted the cherry-pick, confirmed the conflict, aborted, and applied all changes manually adapted to the current master's API.

### 2. Missing Files on Current Branch
The following files did not exist on the current master branch:
- `HarnessApp.java` — created in `cb18525` on master-backup, never merged to master
- `ClaudeProvider.java` — exists on master-backup, not on master
- `render.yaml` — exists on master-backup, not on master

I created all three. The pom.xml already declared `mainClass=ai4se.harness.HarnessApp`, confirming HarnessApp was expected.

### 3. ClaudeProvider Brought In (SCOPE CONCERN)
ClaudeProvider was not in the task's commit list but is a necessary dependency for HarnessApp to compile (the claude branch references it). I adapted it to the current API (toolArgs as JSON String instead of Map). This may overlap with a future PR in the 11-PR series. If a later PR adds ClaudeProvider, merge conflicts will need resolution.

### 4. Cherry-Pick Not Used
Due to the API divergence and missing files, the cherry-pick approach failed (conflict on HarnessApp.java which didn't exist). I applied the intent of all 6 commits manually in a single commit, adapted to the current codebase. The `2ad917e` commit was already applied (as `2fffa05` from PR #7), so it was correctly skipped.

### 5. CredentialManager is DeepSeek-Specific
The current `CredentialManager` hardcodes `DEEPSEEK_API_KEY` as its env var. For the claude branch in HarnessApp, I used `System.getenv("ANTHROPIC_API_KEY")` directly instead of `cm.getKey()` (which would incorrectly check DEEPSEEK_API_KEY). This is a pre-existing limitation, not introduced by this PR.

### 6. Real API Test Pending (KEY CHECKPOINT)
The real DeepSeek API integration test (HelloWorld task) is to be verified by the controller after merge, as specified in the task.

## Files Changed

| File | Status | Purpose |
|------|--------|---------|
| `src/main/java/ai4se/harness/llm/DeepSeekProvider.java` | NEW | DeepSeek provider with static parseResponse |
| `src/main/java/ai4se/harness/llm/ClaudeProvider.java` | NEW | Claude provider (dependency, adapted) |
| `src/main/java/ai4se/harness/HarnessApp.java` | NEW | CLI entry point with provider-before-key logic |
| `src/test/java/ai4se/harness/llm/DeepSeekProviderTest.java` | NEW | 7 tests |
| `render.yaml` | NEW | Render deployment config |
| `harness.yaml` | MODIFIED | Default provider → deepseek |
