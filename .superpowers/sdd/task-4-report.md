# Task 4 Report: PR #4 - Guardrails

## Status: DONE

## Summary

Implemented guardrails (Command, File, Network) with chained execution for the Java Coding Agent Harness. Cherry-picked 4 commits from `master-backup` onto `feature/guardrails` (based on master @ `1279b7c`), added wildcard path tests, and opened PR #3 on GitHub.

## Commits Created

| SHA | Subject |
|-----|---------|
| `a71b4fc` | feat: add Guardrail interface, GuardResult, and CommandGuardrail |
| `a17cfca` | feat: add FileGuardrail and NetworkGuardrail |
| `3af4c7c` | feat: add GuardrailChain for chained guardrail execution |
| `d230a69` | fix: catch InvalidPathException in FileGuardrail for wildcards |
| `554ba68` | feat: add guardrails (Command, File, Network) with chain execution |

The first 4 commits were cherry-picked cleanly from `master-backup` (no conflicts). The 5th commit adds the missing wildcard path tests to `FileGuardrailTest.java`.

## Cherry-Pick

```
git cherry-pick 95d62dd 980bfcb d97c6a9 283e811
```

All 4 commits applied cleanly with **zero conflicts**.

## Key Fix: InvalidPathException Handling

Verified in `src/main/java/ai4se/harness/guardrails/FileGuardrail.java:23-31`:

```java
try {
    Path resolved = projectRoot.resolve(path).normalize();
    if (!resolved.startsWith(projectRoot)) {
        return GuardResult.block("File access outside project root: " + path);
    }
    return GuardResult.pass();
} catch (Exception e) {
    return GuardResult.block("Invalid file path: " + path + " (" + e.getMessage() + ")");
}
```

The catch block catches `Exception` (which includes `InvalidPathException` since it extends `IllegalArgumentException` extends `RuntimeException` extends `Exception`). This is broader than catching only `InvalidPathException` and is safer.

**Note on API:** The task description referenced `GuardResult.deny()`, but the actual `GuardResult` API uses `block()` (status `BLOCK`). The implementation correctly uses `block()`. The task description's code snippet was illustrative.

**Why wildcards crash without the catch:** On Windows, the `*` character is invalid in file paths. `Path.resolve("*.java")` throws `java.nio.file.InvalidPathException`. Without the try-catch, this would propagate up and crash the agent. With the catch, it returns a clean `GuardResult.block()`.

## Tests

### Added Tests (FileGuardrailTest.java)

Two new tests added to verify the InvalidPathException fix:

1. `shouldNotThrowOnWildcardPath` — uses `assertThatCode().doesNotThrowAnyException()` to verify `*.java` does not throw
2. `shouldBlockWildcardPath` — verifies `*.java` returns a `BLOCK` result

### Existing Tests (verified passing)

- `FileGuardrailTest`: path outside root blocked (`../../secret.txt`), path inside root passes (`src/main.java`), non-file action passes
- `CommandGuardrailTest`: dangerous command blocked (`rm -rf /`), safe command passes (`echo hello`), non-shell passes, DROP TABLE blocked
- `NetworkGuardrailTest`: curl blocked, wget blocked, normal command passes
- `GuardrailChainTest`: all pass, block at first guardrail, block at second guardrail

### Test Run

```
mvnw.cmd test
Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Guardrail test breakdown:
- CommandGuardrailTest: 4 tests
- FileGuardrailTest: 5 tests (3 original + 2 new wildcard tests)
- GuardrailChainTest: 3 tests
- NetworkGuardrailTest: 3 tests
- (plus 27 other tests from prior PRs: llm + tools packages)

## PR

- **URL:** https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/3
- **Title:** feat: guardrails with InvalidPathException handling
- **Base:** master ← **Head:** feature/guardrails
- **State:** OPEN

**Note on PR numbering:** The task is "Task 4 / PR #4" in the task series numbering. On GitHub, this is PR #3 because Task 1 (project setup) did not create a PR. GitHub PR numbering: #1 = LLM layer (Task 2), #2 = tools (Task 3), #3 = guardrails (Task 4).

## Files Changed

**Main sources (new):**
- `src/main/java/ai4se/harness/guardrails/Guardrail.java` — interface: `name()`, `check(actionName, actionParams)`
- `src/main/java/ai4se/harness/guardrails/GuardResult.java` — status enum (PASS/BLOCK/HITL) + reason
- `src/main/java/ai4se/harness/guardrails/CommandGuardrail.java` — denylist-based command blocking
- `src/main/java/ai4se/harness/guardrails/FileGuardrail.java` — path traversal protection + InvalidPathException catch
- `src/main/java/ai4se/harness/guardrails/NetworkGuardrail.java` — blocks curl/wget
- `src/main/java/ai4se/harness/guardrails/GuardrailChain.java` — sequential execution, stops on first non-pass

**Test sources (new):**
- `src/test/java/ai4se/harness/guardrails/CommandGuardrailTest.java`
- `src/test/java/ai4se/harness/guardrails/FileGuardrailTest.java` (extended with 2 wildcard tests)
- `src/test/java/ai4se/harness/guardrails/NetworkGuardrailTest.java`
- `src/test/java/ai4se/harness/guardrails/GuardrailChainTest.java`

## Subagent标注

- **Subagent:** GLM (glm-5.2)
- **Human:** verified InvalidPathException catch for wildcards

## Concerns

None. All cherry-picks applied cleanly, all 42 tests pass, the InvalidPathException catch is verified working on Windows (where `*` is an invalid path character), and the PR is open.
