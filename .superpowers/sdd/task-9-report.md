# Task 9 Report — PR #9: CLI + Demo

**Status:** DONE

**Branch:** `feature/cli-demo` (worktree `D:\文件\Agent\.worktrees\cli-demo`, based on `master` @ `da639e7`)
**GitHub PR:** https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/8
**Commit:** `acf78b1` — `feat: add CLI with run/chat/config commands and mechanism demo`

---

## Summary

Added a Picocli-based CLI (`run`, `chat`, `config`) and the three deterministic mechanism demos required by §A.6. All 115 tests pass (3 new demos + 112 pre-existing).

## Commits

| SHA | Subject |
|-----|---------|
| `acf78b1` | feat: add CLI with run/chat/config commands and mechanism demo |

## Test summary

```
.\mvnw.cmd test
Tests run: 115, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS  (5.8s)
```

New `DemoTest` (3/3 pass):
- `demo1_guardrailBlocksDangerousCommand` — `CommandGuardrail` blocks `rm -rf /`.
- `demo2_feedbackLoopDrivesSelfCorrection` — mock LLM: fail (compile error) → succeed → `end_turn`; 3 LLM calls.
- `demo3_chatMode_textResponseStops` — chat mode text response stops after 1 LLM call (no spin).

## What was delivered

### 1. CLI (`src/main/java/ai4se/harness/HarnessApp.java`)
- `@Command(name="harness", mixinStandardHelpOptions=true, subcommands={RunCommand, ChatCommand, ConfigCommand})`
- **`run <task>`** — loads `harness.yaml`, builds `AgentLoop`, `loop.run(task)`. DeepSeek + Claude providers.
- **`chat`** — `loop.chat()` interactive mode; `退出` exits (handled in `AgentLoop.chat()`).
- **`config`** subcommands:
  - `set-key` — `System.console().readPassword()` (hidden input) → `CredentialManager.storeKey()`; zeroes char array.
  - `show-key` — `CredentialManager.getMaskedKey()` → `sk-****xxxx` (masked, never plaintext).
  - `clear-key` — `CredentialManager.clearKey()`.
- Extracted `buildLoop(HarnessConfig)` helper (DRY between `run` and `chat`).

### 2. Mechanism demos (`src/test/java/ai4se/harness/demo/DemoTest.java`)
Deterministic, mock-LLM, no network. See test summary above.

## Cherry-pick handling

Task listed three commits from `master-backup`:

| Commit | Action | Reason |
|--------|--------|--------|
| `cb18525` (CLI entry point) | **skipped** | Add/add conflict with existing `HarnessApp.java` (PR #8 already ships a superset with DeepSeek). Resolving to "ours" = empty commit → `git cherry-pick --skip`. |
| `24df6b2` (mechanism demo) | applied as base, then **rewritten** | Original does not compile vs current API: `LlmResponse` 3rd arg is `String toolArgs` (not `Map`); `FailureType.COMPILE_ERROR` doesn't exist (actual `COMPILATION_ERROR`); `Tool.execute(String)` not `execute(Map)`. Its demo3 was `FailureClassifier`, not the required chat-mode demo. demo1 kept as-is. |
| `1ee9afa` (typo; real `1ee6afa`) | **skipped** | Modifies `web/app.py` + `web/static/app.js` — frontend files absent from this Java worktree (modify/delete conflict). Irrelevant to CLI/demo deliverables. |

## Concerns

- **Cherry-picks not applied verbatim.** All three listed cherry-picks were either superseded, non-compiling, or frontend-only (see table). The deliverables (chat command, hidden-input config, 3 demos) were implemented fresh in one clean commit. Documented in the PR body.
- **`System.console()` may be null** in non-interactive environments (piped stdin, some IDEs). `set-key` guards against this with a clear error message and exit code 1. Hidden input only works in a real terminal.
- **`show-key` masked format** is `sk-****xxxx` (first 3 + `****` + last 4), produced by `CredentialManager.mask()`. The task's `sk-****-xxxx` was illustrative; the actual masking never exposes plaintext (verified by `CredentialManagerTest.shouldMaskKeyNotShowPlainText`).
- **GitHub PR number is #8**, while the task calls this "PR #9" (project-internal sequence numbering). The previous merge commit was labelled "Merge PR #8" in its message; GitHub's own PR counter is offset by one. No action needed.

## Subagent标注

- **Subagent**: GLM (`glm-5.2`) — implemented CLI commands, demo tests, ran verification (`mvnw test`), pushed branch, created PR.
- **Human**: added `chat` command and hidden-input `config set-key` / masked `show-key` / `clear-key` subcommands; resolved cherry-pick conflicts.
