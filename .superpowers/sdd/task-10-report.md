# Task 10 Report: PR #10 - CI + Docker

## Status: DONE_WITH_CONCERNS

## Summary

Added CI pipeline (`.github/workflows/ci.yml`) with `unit-test` + `docker-build` jobs and a multi-stage `Dockerfile` using openjdk-21 runtime. Docker image is pushed to GitHub Container Registry (GHCR) on master pushes.

## Commits Created

| SHA | Subject |
|-----|---------|
| `1d4b2fa` | ci: add GitHub Actions workflow for unit tests (cherry-pick of `abe2677`) |
| `c625445` | feat: add Dockerfile for container distribution (cherry-pick of `f8be641`) |
| `1ccac9f` | feat: add CI with unit-test + docker-build, push to GHCR (KEY FIX commit) |

## Pull Request

- **URL**: https://github.com/huliice123-afk/ai4se-coding-agent-harness/pull/9
- **GitHub PR #**: 9 (project scheme: "PR #10 of 11")
- **Title**: feat: CI with unit-test + docker-build
- **Base**: `master` ← **Head**: `feature/ci-docker`
- **State**: OPEN

## Test Summary

```
.\mvnw.cmd test
Tests run: 115, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 115 tests pass.

## Changes

### `.github/workflows/ci.yml` (KEY FIX #2)

Two jobs:

1. **`unit-test`** (name: `Unit Tests`, required by §五.6):
   - `actions/checkout@v4`
   - `actions/setup-java@v4` — Java 17, temurin, `cache: maven`
   - `mvn test`

2. **`docker-build`** (name: `Docker Build`, required by §4.8):
   - `needs: unit-test` (gated on tests passing)
   - `permissions: contents: read, packages: write`
   - `docker build -t ai4se-harness .`
   - On `master` push only: login to `ghcr.io` with `GITHUB_TOKEN`, tag as `ghcr.io/<owner>/ai4se-harness:latest`, push (§3.2 GHCR requirement)

Triggers: push/PR to `master` only.

### `Dockerfile` (KEY FIX #3)

Multi-stage build:
- **Build**: `maven:17-eclipse-temurin` — `COPY pom.xml` → `mvn dependency:go-offline` (layer cache) → `COPY src` → `mvn package -DskipTests`
- **Runtime**: `eclipse-temurin:21-jre` — `COPY --from=build /app/target/harness-*.jar app.jar` → `ENTRYPOINT ["java", "-jar", "app.jar"]`

Java 21 runtime used because openjdk-17 is unavailable on Debian Trixie (per commit `670d394`).

## Cherry-pick Notes

| Commit | Subject | Result |
|--------|---------|--------|
| `abe2677` | ci: add GitHub Actions workflow | ✅ Applied as base, then extended with docker-build job |
| `f8be641` | feat: add Dockerfile | ✅ Applied as base, then rewritten per KEY FIX |
| `cc6a217` | fix: clean up Dockerfile, mvn caching | ⏭️ Skipped — modifies `web/Dockerfile` (absent in this branch; modify/delete conflict). Equivalent caching fix applied to root `Dockerfile`. |
| `670d394` | fix: use openjdk-21-jre-headless | ⏭️ Skipped — modifies `web/Dockerfile` (absent in this branch). Equivalent openjdk-21 fix applied to root `Dockerfile`. |

`cc6a217` and `670d394` target `web/Dockerfile`, a frontend Dockerfile that exists in `master-backup` but was never merged into `master` (the `web/` directory does not exist in this worktree). The fixes they represent (dependency caching, openjdk-21) were applied directly to the root `Dockerfile` per the KEY FIX specification.

## Verification Evidence

- **Tests**: `.\mvnw.cmd test` → `Tests run: 115, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` (run 2026-07-07)
- **ci.yml**: Contains `unit-test` job (§五.6) and `docker-build` job (§4.8) with GHCR push (§3.2). Verified by reading file back.
- **Dockerfile**: Uses `eclipse-temurin:21-jre` runtime (openjdk-21). Verified by reading file back.
- **PR**: Created and confirmed OPEN at the correct base/head.

## Concerns

1. **`harness.yaml` not copied into Docker image**: The KEY FIX #3 Dockerfile specification does not include `COPY harness.yaml`. The harness loads `harness.yaml` as its config at runtime (`ConfigLoader`), so the image will need `harness.yaml` volume-mounted (e.g. `-v harness.yaml:/app/harness.yaml`) or the config provided via environment/another path. I followed the task's exact Dockerfile content. If standalone execution is expected, this should be revisited.

2. **Cherry-pick skips**: `cc6a217` and `670d394` could not be cherry-picked cleanly (they modify `web/Dockerfile`, absent from this branch). The equivalent fixes were applied to the root `Dockerfile` manually. This is documented in the PR body.

3. **PR numbering**: This is GitHub PR #9, but the project's internal scheme calls it "PR #10" (off-by-one, consistent with prior PRs — e.g. the CLI PR was GitHub #8 but labeled "PR #9").

## Subagent标注

- **Subagent**: GLM (`glm-5.2`) — cherry-picked base commits, applied KEY FIXES (docker-build job, openjdk-21 Dockerfile), ran `mvn test` verification, created PR.
- **Human**: specified required CI/Dockerfile structure, cherry-pick SHAs, and KEY FIX specifications.
