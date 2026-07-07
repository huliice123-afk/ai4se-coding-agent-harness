# AGENTS.md — Coding Agent Harness

## 项目概述

构建一个 Java 实现的 Coding Agent Harness，遵循 AI4SE 期末项目 A 方向全部要求。

## 技术栈

- **语言**: Java 17+
- **构建工具**: Maven
- **LLM 供应商**: Anthropic Claude API
- **测试框架**: JUnit 5 + Mockito + AssertJ
- **HTTP 客户端**: OkHttp 或 Java 11 HttpClient

## 代码规范

- 遵循 Java 标准命名规范（PascalCase 类名、camelCase 方法/变量、UPPER_SNAKE 常量）
- 包名遵循 `ai4se.harness.<module>` 约定
- 禁止提交包含真实凭据的代码（API key、token 等）
- 禁止硬编码凭据，使用安全存储方案

## 项目结构

```
harness/
├── src/main/java/ai4se/harness/
│   ├── core/          # 主循环、Agent 核心
│   ├── llm/           # LLM 抽象层（接口 + Claude 实现）
│   ├── tools/         # 工具定义与分发
│   ├── guardrails/    # 治理护栏
│   ├── feedback/      # 反馈闭环
│   ├── memory/        # 上下文与记忆
│   └── config/        # 配置管理
├── src/test/java/ai4se/harness/  # 测试（含 mock-LLM 单测）
├── pom.xml
└── README.md
```

## 工作流纪律

### 强制要求
- **TDD 强制**: 先写失败测试 → 最小实现使其通过 → 重构
- **Superpowers 七步工作流**: brainstorming → writing-plans → git-worktrees → subagent-driven → TDD → code-review → finishing-branch
- **先 SPEC 后实现**: 在 SPEC.md 和 PLAN.md 完成并通过冷启动验证之前，禁止编写实现代码
- **PR 工作流**: 每个独立功能/模块开一个 worktree，对应一个 PR
- **禁止单次 commit 提交全部代码**: 必须保留完整 commit 历史

### 代码质量
- 每个核心机制必须有 mock-LLM 驱动的确定性单元测试
- 测试不依赖网络与真实 LLM
- 移除真实 LLM 后，替代为 mock/stub 后，仍能用确定性测试验证核心机制

### 安全
- API Key 绝不硬编码、绝不提交到 Git、绝不写入日志
- 使用操作系统凭据存储（Windows Credential Manager / macOS Keychain）
- `.env` 文件仅用于本地开发，已加入 `.gitignore`

## 验收标准

- harness 主循环可运行、可闭环
- 六个维度（决策/工具/记忆/治理/反馈/配置）均有可运行的最低实现
- 选择一个维度深入实现（重点维度）
- 机制演示：mock LLM 下确定性复现：① 护栏拦截危险动作 ② 反馈闭环驱动自我修正 ③ 重点维度行为
- 可一键运行测试（`mvn test`）
- CI（GitHub Actions）配置，push 自动运行测试