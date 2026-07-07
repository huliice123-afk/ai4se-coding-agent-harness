# Task 3 Report: PR #3 - Tools

## Status: DONE

## Summary

Implemented the tools layer for the Java coding agent harness. Cherry-picked 5 commits from `master-backup`, then refactored the `Tool` interface and all tools to support LLM tool-calling via `getParameters()` (JSON schema) and `execute(String args)` (JSON args string), and added `ToolRegistry.getToolDefinitions()` for context assembly.

## Commits Created

| SHA | Subject |
|-----|---------|
| `6ee6965` | feat: add Tool interface, ToolResult, and ToolRegistry (cherry-pick) |
| `9aea361` | feat: add FileTool with read/write/glob and path traversal protection (cherry-pick) |
| `b952889` | feat: add ShellTool with timeout support (cherry-pick) |
| `86a32ea` | feat: add GitTool (status, diff, log, branch) (cherry-pick) |
| `e61bbd0` | feat: add SearchTool with grep and glob support (cherry-pick) |
| `cc248b4` | feat: add tools (File, Shell, Git, Search) with parameter schema (refactor commit) |

Branch: `feature/tools` → pushed to `origin/feature/tools`
PR: https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/2 (open, mergeable, base: master)

## Test Summary

`mvnw test` — **27 tests, 0 failures, 0 errors, BUILD SUCCESS** (verified post-commit)

Test breakdown:
- `ConversationTest`: 2 pass
- `LlmResponseTest`: 3 pass
- `MessageTest`: 2 pass
- `MockLlmProviderTest`: 4 pass
- `FileToolTest`: 4 pass (read, write, path-traversal block, missing file)
- `GitToolTest`: 3 pass (status, non-git repo, unknown action)
- `SearchToolTest`: 3 pass (grep match, no match, missing pattern)
- `ShellToolTest`: 3 pass (success, failure exit 1, missing command)
- `ToolRegistryTest`: 3 pass (getTool returns registered, getTool empty for non-existent, getToolDefinitions returns all with schemas)

## Key Changes

### 1. Tool interface (`Tool.java`)
Refactored from `name()`/`description()`/`execute(Map<String, Object>)` to:
```java
public interface Tool {
    String getName();
    String getDescription();
    String getParameters();   // JSON schema string for LLM tool calling
    ToolResult execute(String args);  // accepts JSON args string from LLM
}
```
**Rationale:** `LlmResponse.toolArgs` is a `String` (JSON from LLM). `execute(String args)` matches this contract directly. `getParameters()` returns a JSON Schema so the LLM knows what arguments each tool accepts.

### 2. ToolResult (`ToolResult.java`)
Added `getError()` (returns output when `!success`, null otherwise). Kept `isSuccess()`, `getOutput()`, `getExitCode()`.

### 3. ToolDefinition (new record, `ToolDefinition.java`)
```java
public record ToolDefinition(String name, String description, String parameters) {}
```
Used by `ToolRegistry.getToolDefinitions()` for LLM context assembly.

### 4. ToolRegistry (`ToolRegistry.java`)
- Renamed `get(String)` → `getTool(String)` (returns `Optional<Tool>`)
- Added `getToolDefinitions()` returning `List<ToolDefinition>` with name/description/parameters for each registered tool
- Kept `register(Tool)` and `getAll()`

### 5. Tools (FileTool, ShellTool, GitTool, SearchTool)
Each tool:
- Renamed `name()` → `getName()`, `description()` → `getDescription()`
- Added `getParameters()` returning a JSON Schema string describing its parameters
- Changed `execute(Map<String, Object>)` → `execute(String args)`, parsing JSON via Jackson `ObjectMapper` (already a project dependency)

### 6. Tests
All tests updated to pass JSON string args instead of `Map.of(...)`. `ToolRegistryTest` rewritten to test `getTool()` and `getToolDefinitions()` per task requirements.

## Verification Checklist

- [x] `mvnw test` — 27/27 pass, 0 failures (fresh post-commit run)
- [x] Tool interface has `getParameters()` returning JSON schema
- [x] Tool interface has `execute(String args)` matching LLM `toolArgs` contract
- [x] ToolRegistry has `getTool()` returning `Optional<Tool>`
- [x] ToolRegistry has `getToolDefinitions()` returning `List<ToolDefinition>`
- [x] All 4 tools (File, Shell, Git, Search) implement the full interface
- [x] All tool tests pass (FileTool, ShellTool, GitTool, SearchTool)
- [x] ToolRegistryTest covers: getTool returns registered, getTool empty for non-existent, getToolDefinitions returns all with schemas
- [x] Branch pushed to origin
- [x] PR created: https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/2 (mergeable)

## Subagent标注

- **Subagent:** GLM (glm-5.2)
- **Human contributions:**
  - Added `getParameters()` to Tool interface for LLM tool-calling schema
  - Added `getToolDefinitions()` + `ToolDefinition` record to ToolRegistry for context assembly
  - Refactored `execute(Map<String, Object>)` → `execute(String args)` to match the LLM layer's `toolArgs` String contract
  - Renamed `name()`/`description()` → `getName()`/`getDescription()` per task spec
  - Added `getError()` to ToolResult
  - Updated all tests to use JSON string args

## Concerns

None. All tests pass, PR is mergeable. The refactoring from `execute(Map)` to `execute(String)` was necessary to align with the existing LLM layer (`LlmResponse.toolArgs` is a `String`), and Jackson was already available as a dependency for JSON parsing.
