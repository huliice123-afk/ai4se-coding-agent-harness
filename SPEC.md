# SPEC.md — Coding Agent Harness

> AI4SE 期末项目 · A 方向 · Coding Agent Harness

---

## 1. 问题陈述

### 1.1 要解决的问题

当前市场上的 Coding Agent（如 Claude Code、Codex、Cursor）大多是封闭的商业产品，其内部 harness 机制（主循环、工具分发、治理护栏、反馈闭环）对使用者是不透明的。开发者使用这些工具时，无法理解"一个 LLM 是如何被封装成稳定可靠的 coding agent 的"，也难以定制或审计其行为。

### 1.2 目标用户

- 对 AI4SE 方法论感兴趣的软件工程学生和研究者
- 希望理解 agent harness 内部机制的开发者
- 需要一个可审计、可定制、可单测的轻量 coding agent 原型的场景

### 1.3 为什么值得做

本项目通过从零构建一个 Coding Agent Harness，展示"Agent = LLM + Harness"这一核心等式。当 LLM 只负责"下一步做什么"时，工程师的价值体现在 harness 层的工程上：治理、反馈、上下文、安全。本项目让使用者对这套方法论形成第一手的批判性理解。

---

## 2. 用户故事

| # | 用户故事 | 验收标准 |
|---|---------|---------|
| US1 | 作为开发者，我希望通过 CLI 启动一个 coding agent 会话，输入自然语言任务，agent 能自主调用工具完成任务 | 输入任务后，agent 能自主循环决策、调用工具、输出结果 |
| US2 | 作为开发者，我希望 agent 在执行危险命令时被拦截，并让我手动确认 | 触发 `rm -rf` 等危险命令时，agent 暂停并等待输入 y/n |
| US3 | 作为开发者，我希望 agent 在执行失败后能自动收到反馈，并根据反馈修正行为 | 注入一次编译错误后，agent 能识别失败类型并尝试修正 |
| US4 | 作为开发者，我希望 agent 能记住之前的会话决策，并在新会话中利用这些信息 | 重启会话后，agent 能读取上次决策记录 |
| US5 | 作为开发者，我希望通过配置文件控制 agent 的行为（工具白名单、护栏开关、最大轮次等） | 修改 harness.yaml 后，agent 行为相应改变 |
| US6 | 作为开发者，我希望用 mock LLM 离线验证 harness 的所有核心机制，不依赖网络和真实 LLM | `mvn test` 在无网络环境下全部通过 |
| US7 | 作为开发者，我希望安全地管理 API Key，Key 不硬编码、不提交到 Git、不写入日志 | Key 存储在 Windows Credential Manager，通过 CLI 命令管理 |

---

## 3. 功能规约

### 3.1 Agent 主循环（core）

**输入**: 用户自然语言任务字符串
**行为**:
1. 加载配置（harness.yaml）
2. 加载记忆（AGENTS.md + 会话记忆）
3. 构建上下文（系统提示 + 记忆 + 可用工具列表 + 对话历史）
4. 调用 LLM（单次对话补全）
5. 解析 LLM 响应（动作名 + 参数）
6. 护栏检查（链式执行所有 Guardrail）
7. 执行工具（通过 ToolRegistry 分发）
8. 采集反馈（FeedbackCollector 判定结果）
9. 若成功 → 反馈信息回灌，进入下一轮
10. 若失败 → 失败分类 + 修正建议回灌，进入下一轮
11. 每轮判定是否停机（LLM 声明完成 / 达到最大轮次 / 达到最大修正轮次）

**输出**: 最终结果（成功/失败 + 摘要）
**边界条件**:
- 最大循环轮次：默认 10 轮
- 最大修正轮次：默认 3 轮
- 空输入 → 提示用户输入任务
- LLM 返回无法解析的响应 → 视为错误，回灌 LLM 要求重新输出

**错误处理**:
- LLM API 调用失败 → 重试 3 次，指数退避，仍失败则停机并报告
- 工具执行异常 → 捕获异常，作为反馈回灌，不中断主循环

### 3.2 LLM 抽象层（llm）

**接口**: `LlmProvider.complete(messages, tools) → LlmResponse`

**Claude 实现**: 调用 Anthropic Messages API（`/v1/messages`），使用 tool_use 格式

**Mock 实现**: 支持两种模式
- **脚本模式**: `given(input).thenReturn(response)` 输入→输出映射
- **序列模式**: `setSequence(r1, r2, r3)` 多轮对话预设

**边界条件**:
- 超时：30 秒
- Token 超限：截断最早的消息，保留系统提示 + 最近 3 轮
- 网络错误：重试 3 次

### 3.3 工具系统（tools）

**工具注册**: `ToolRegistry` 管理所有工具，按名称分发

| 工具 | 操作 | 输入 | 输出 | 权限 |
|------|------|------|------|------|
| FileTool | read/write/glob | 路径 + 内容 | 文件内容 / 状态 | 限定项目根目录内 |
| ShellTool | 执行 shell 命令 | 命令字符串 | 退出码 + stdout + stderr | 需护栏检查 |
| GitTool | status/diff/commit/branch/log | 子命令 + 参数 | 命令输出 | 危险操作需确认 |
| SearchTool | grep/glob | 模式 + 路径 | 匹配结果列表 | 只读，无限制 |

**边界条件**:
- 文件路径必须规范化，禁止 `../` 越界
- Shell 命令超时 30 秒
- 工具执行结果截断至 4000 字符

### 3.4 治理护栏（guardrails）

**三层链式护栏**，每个 Guardrail 返回 `PASS / BLOCK / HITL`：

| 护栏 | 拦截内容 | 判定方式 |
|------|---------|---------|
| CommandGuardrail | 危险 Shell 命令 | 正则匹配：`rm\s+-rf`、`sudo`、`DROP\s+TABLE`、`chmod\s+777`、`>\s*/dev/`、`mkfs`、`dd\s+if=`、`shutdown`、`:(){` |
| FileGuardrail | 文件越界 | 路径规范化后检查是否以项目根目录开头，禁止操作 `.git` 目录内部 |
| NetworkGuardrail | 对外网络请求 | 正则匹配：`curl`、`wget`、`nc\s`、`telnet`，除非在 `allowed_hosts` 白名单中 |

**HITL 流程**:
1. 拦截 → 打印：[危险动作类型] 详情：[命令/路径]
2. 等待用户输入 `y`（放行）/ `n`（拒绝）/ `a`（本次会话全部放行）
3. 用户选择 `n` → 构造 `FEEDBACK: 操作被护栏拦截，请换一种方案` 回灌 LLM

**边界条件**:
- 护栏可配置开关（harness.yaml 中 `guardrails.hitl: true/false`）
- 关闭 HITL 时，危险动作直接 BLOCK（不询问，直接拒绝）
- 危险命令正则列表可配置扩展

### 3.5 反馈闭环（feedback）— 重点维度

**反馈流水线**:

```
工具执行结果 → FeedbackCollector → FailureClassifier → SeverityJudge → 修正建议 → 回灌 LLM
```

#### 3.5.1 FeedbackCollector（反馈采集器）

对每种工具执行结果，自动采集客观信号：

| 工具 | 采集信号 | 判定方式 |
|------|---------|---------|
| Shell | 退出码、stdout、stderr、耗时 | 退出码 != 0 → 失败 |
| File | 读：文件是否存在、内容；写：写入后校验 | 读缺失/写校验失败 → 失败 |
| Git | 退出码、状态变化 | 退出码 != 0 → 失败 |
| Search | 结果数量 | 结果数 > 0 → 成功 |

**输出**: `Feedback` 对象，包含 `success: boolean`、`signals: Map<String, Object>`、`rawOutput: String`

#### 3.5.2 FailureClassifier（失败分类器）

```java
enum FailureType {
    COMPILE_ERROR,      // 编译/语法错误（javac 错误信息）
    RUNTIME_ERROR,       // 运行时错误（exception stack trace）
    TEST_FAILURE,        // 测试失败（JUnit 输出）
    COMMAND_REJECTED,    // 被护栏拦截
    FILE_NOT_FOUND,      // 文件不存在
    PERMISSION_DENIED,   // 权限不足
    TIMEOUT,             // 超时
    UNKNOWN              // 兜底
}
```

**分类逻辑**: 基于确定性规则（正则匹配 stderr 关键字、退出码范围），不依赖 LLM。

#### 3.5.3 SeverityJudge（严重性判定）

| 级别 | 含义 | 触发条件 |
|------|------|---------|
| FATAL | 不可恢复 | 护栏拦截（用户拒绝）、权限不足 |
| ERROR | 需要修正 | 编译错误、测试失败 |
| WARNING | 可继续 | 超时（可重试）、文件不存在（可创建） |
| INFO | 仅供参考 | 搜索无结果 |

#### 3.5.4 修正建议生成

确定性规则生成建议，不依赖 LLM：

| 失败类型 | 建议 |
|---------|------|
| COMPILE_ERROR | "编译错误，请根据以下错误信息修正代码：{stderr}" |
| TEST_FAILURE | "测试失败，请检查测试输出并修正实现：{stderr}" |
| COMMAND_REJECTED | "操作被护栏拦截，请换一种安全的方案" |
| TIMEOUT | "命令超时，请尝试拆分任务或优化执行方式" |

#### 3.5.5 多轮自我修正

1. 第 N 轮 LLM 输出 → 执行 → 失败
2. 分类 + 严重性判定 + 修正建议
3. 将反馈注入对话上下文，LLM 进入第 N+1 轮
4. 最多 3 轮修正，超过则停机并报告失败摘要

**每轮反馈格式**:
```
[FEEDBACK] 失败类型: COMPILE_ERROR
错误详情: Main.java:5: error: ';' expected
修正建议: 编译错误，请根据以下错误信息修正代码
```

### 3.6 记忆系统（memory）

**Codex 风格**：基于 Markdown 文件存储，按需检索注入上下文。

| 记忆类型 | 存储路径 | 加载时机 | 内容 |
|---------|---------|---------|------|
| AGENTS.md | 项目根目录 | 每次会话启动 | 项目约定、技术栈、规范 |
| 会话记忆 | `.harness/memory/session_*.md` | 下次会话启动时加载最近一次 | 上次做了什么、为什么 |
| 决策记忆 | `.harness/memory/decisions.md` | 按查询检索 | 关键决策及原因 |

**检索策略**:
1. 从当前任务提取关键词（分词 + 去停用词）
2. 在决策记忆中搜索匹配行
3. 取 top-3 匹配片段，注入上下文
4. 不使用全量加载，避免上下文爆炸

**边界条件**:
- 记忆文件不存在 → 静默跳过
- 单条记忆上限 2000 字符
- 检索无结果 → 返回空，不注入额外内容

### 3.7 配置系统（config）

**配置文件**: `harness.yaml`（YAML 格式）

```yaml
llm:
  provider: claude
  model: claude-sonnet-4-20250514
  max_tokens: 4096
tools:
  allowed: [file, shell, git, search]
  shell_timeout: 30s
guardrails:
  hitl: true
  command_denylist: [rm -rf, sudo, chmod 777, mkfs, shutdown]
  network_blocked: true
  network_allowed_hosts: []
feedback:
  max_rounds: 3
loop:
  max_rounds: 10
memory:
  store_path: .harness/memory
  search_top_k: 3
```

**凭据管理**: API Key 不写入配置文件，通过 CLI 命令管理：
- `harness config set-key` → 隐藏输入，写入 Windows Credential Manager
- `harness config show-key` → 显示掩码（如 `sk-ant-****-xxxx`）
- `harness config clear-key` → 清除存储的 Key

---

## 4. 非功能性需求

### 4.1 性能

- harness 启动时间 < 2 秒
- 单轮循环（不含 LLM 调用）< 100ms
- 工具执行超时可配置，默认 30 秒

### 4.2 安全（含凭据威胁模型）

**威胁模型**:
- 攻击者获取仓库代码 → 不会泄露 API Key（Key 不在代码中）
- 攻击者获取机器访问权限 → 可通过 Credential Manager 读取 Key（操作系统级防护）
- 日志泄露 → Key 从不写入日志
- Shell history 泄露 → 使用隐藏输入，不通过命令行参数传递 Key

**对策**:
- API Key 存储于 Windows Credential Manager（`keyring` 库）
- `.env` 文件已加入 `.gitignore`，仅作备选加载源
- 所有日志输出前过滤 Key 模式（`sk-ant-` 前缀）
- 配置文件中不含凭据字段

### 4.3 可用性

- CLI 命令设计直观：`harness run "任务描述"`
- 错误信息清晰，包含失败原因和建议
- 颜色输出区分信息级别（INFO / WARN / ERROR）

### 4.4 可观测性

- 每轮循环输出：轮次编号、动作、结果摘要
- 工具执行输出：命令、退出码、耗时
- 日志级别可配置（DEBUG / INFO / WARN / ERROR）

---

## 5. 系统架构

### 5.1 组件图

```
┌─────────────────────────────────────────────────┐
│                    CLI Entry                      │
│              harness run "task"                  │
└─────────────────────┬───────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────┐
│                  AgentLoop (core)                │
│  ┌──────────────────────────────────────────┐   │
│  │  context → LLM → parse → guardrail →     │   │
│  │  execute → feedback → loop               │   │
│  └──────────────────────────────────────────┘   │
└──┬──────────┬──────────┬──────────┬─────────────┘
   │          │          │          │
┌──▼──┐ ┌────▼────┐ ┌───▼───┐ ┌───▼──────┐
│ LLM  │ │Tools    │ │Guard  │ │Feedback  │
│      │ │Registry │ │rails  │ │Pipeline  │
└──────┘ └─────────┘ └───────┘ └──────────┘
                                          │
┌──────────┐                        ┌────▼─────┐
│  Memory  │                        │ Config   │
│  Store   │                        │ Manager  │
└──────────┘                        └──────────┘
```

### 5.2 数据流

```
User Input
  │
  ▼
Context Assembler ◄── Memory Store ◄── AGENTS.md / session / decisions
  │
  ▼
LLM Provider (Claude API / Mock)
  │
  ▼
Action Parser ──► { tool: "shell", params: { command: "mvn test" } }
  │
  ▼
Guardrail Chain ──► PASS / BLOCK / HITL
  │
  ▼
Tool Registry ──► Tool.execute()
  │
  ▼
Feedback Pipeline ──► Feedback { success, type, severity, suggestion }
  │
  ▼
Loop: feedback → context → next round (or stop)
```

### 5.3 外部依赖

| 依赖 | 用途 | 备选 |
|------|------|------|
| Anthropic Messages API | 真实 LLM 调用 | 可替换为 OpenAI 等兼容 API |
| Windows Credential Manager | 凭据安全存储 | macOS Keychain / Linux Secret Service |
| Jackson | YAML 解析 | SnakeYAML |
| JUnit 5 + Mockito | 测试 | — |
| Maven | 构建 | — |

---

## 6. 数据模型

### 6.1 核心实体

```
Message { role: SYSTEM|USER|ASSISTANT, content: String }
Conversation { messages: List<Message> }
Action { tool: String, params: Map<String, Object> }
ToolResult { success: bool, output: String, exitCode: int }
GuardResult { status: PASS|BLOCK|HITL, reason: String }
Feedback { success: bool, type: FailureType, severity: Severity, suggestion: String }
LlmResponse { action: Action?, text: String?, stopReason: String }
```

### 6.2 配置实体

```
HarnessConfig { llm: LlmConfig, tools: ToolsConfig, guardrails: GuardrailsConfig, feedback: FeedbackConfig, loop: LoopConfig, memory: MemoryConfig }
```

### 6.3 记忆实体

```
MemoryEntry { key: String, content: String, timestamp: Instant, tags: List<String> }
```

---

## 7. 凭据与分发设计

### 7.1 凭据存储方案

- **主方案**: Windows Credential Manager，通过 Java `keyring` 库或 JNA 直接调用 Win32 API
- **备选方案**: `.env` 文件（仅开发用，已加入 `.gitignore`）
- **录入**: `harness config set-key` → 隐藏输入（`System.console().readPassword()`）
- **查看**: `harness config show-key` → 显示掩码 `sk-ant-****-xxxx`
- **更新**: 重新执行 `set-key` 覆盖
- **清除**: `harness config clear-key`

### 7.2 分发形态

**双分发**:

1. **Docker 容器**:
   ```bash
   docker build -t ai4se-harness .
   docker run -it ai4se-harness run "your task"
   ```
   容器内 Key 通过 volume 挂载或环境变量注入。

2. **Maven Fat JAR**:
   ```bash
   mvn package -Pfat-jar
   java -jar target/harness.jar run "your task"
   ```
   目标平台：Windows / macOS / Linux（Java 17+ 前置）

### 7.3 目标机器 Key 配置

- Docker: 启动时 `-e ANTHROPIC_API_KEY=xxx` 或挂载 `.env` 文件
- JAR: 首次运行引导执行 `harness config set-key`，写入 OS Credential Manager

---

## 8. 技术选型与理由

| 选择 | 理由 |
|------|------|
| Java 17 | 开发者熟悉；强类型适合工程化项目；Maven 生态成熟 |
| Maven | 依赖管理简单，fat JAR 打包方便，CI 集成成熟 |
| Anthropic Claude API | 编码能力业界最强，tool_use API 设计成熟，适合 agentic 场景 |
| JUnit 5 + Mockito | Java 测试标准，Mockito 支持 LLM 抽象层的 mock 注入 |
| Jackson | YAML 解析首选，性能好，注解驱动 |
| Windows Credential Manager | 满足凭据安全存储要求，系统级防护 |
| 纯代码护栏 + 反馈 | 满足"机制必须是代码，不能是提示词"的硬性要求 |

---

## 9. 领域与机制设计（A 方向额外要求）

### 9.1 该领域的反馈信号

Coding 领域的反馈信号是**客观、确定、可回灌的**：

- **编译结果**: 编译器的退出码和错误信息，是明确的"代码有没有语法错误"的信号
- **测试结果**: JUnit 的 pass/fail 输出，是明确的"代码行为是否正确"的信号
- **Shell 退出码**: 0 表示成功，非 0 表示失败，Unix 惯例
- **文件操作结果**: 文件是否存在、写入后内容是否匹配

### 9.2 危险动作

- 危险 Shell 命令（`rm -rf`、`sudo`、`mkfs`、`shutdown`）
- 文件越界操作（读写项目目录外的文件）
- Git 危险操作（`force push`、`reset --hard`）
- 对外网络请求（`curl`、`wget` 发送数据）

### 9.3 所需工具

文件读写、Shell 执行、Git 操作、代码搜索（grep/glob）

### 9.4 记忆需求

项目约定（AGENTS.md）、会话历史、关键决策——按需检索，非全量注入

### 9.5 重点维度：反馈闭环

选择反馈闭环作为重点维度，理由：
- 它是 coding agent 自我修正的**核心驱动力**
- 所有机制都是**确定性代码**（分类器、严重性判定、建议生成），没有任何依赖 LLM 智能的部分
- 天然满足"移除 LLM 后仍可单测"的评分标准
- 反馈闭环的质量直接决定 agent 的自主修正能力

### 9.6 编码实现策略

- 反馈采集器：对每种工具返回结果做确定性解析（退出码、正则匹配 stderr）
- 失败分类器：基于规则引擎（关键字匹配 + 退出码范围），不调用 LLM
- 严重性判定：基于失败类型的固定映射表
- 修正建议：基于失败类型的固定模板，填入具体错误信息
- 所有机制在 mock LLM 下 100% 可单测

---

## 10. 验收标准

| # | 标准 | 验证方式 |
|---|------|---------|
| AC1 | harness 可接收自然语言任务并完成闭环 | 手动运行 `harness run "create a HelloWorld.java"` |
| AC2 | 危险命令被护栏拦截并等待人工确认 | 手动运行含 `rm -rf` 的任务，观察拦截 |
| AC3 | 反馈闭环在失败后驱动修正 | 注入 mock 失败，观察 agent 修正行为 |
| AC4 | 记忆可在跨会话间持久化 | 运行任务 → 退出 → 重启 → 查看记忆加载 |
| AC5 | 配置修改后 agent 行为改变 | 修改 max_rounds 后观察行为变化 |
| AC6 | `mvn test` 在无网络下全部通过 | 断网执行 `mvn test` |
| AC7 | 机制演示脚本可运行 | 运行演示脚本，观察三项演示 |
| AC8 | API Key 不落盘、不提交、不泄露 | 检查代码、Git 历史、日志 |
| AC9 | Docker 和 JAR 均可分发运行 | `docker run` 和 `java -jar` 分别验证 |

---

## 11. 风险与未决问题

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Claude API 调用成本 | 开发阶段频繁调用费用高 | 开发阶段用 Mock LLM，仅在集成测试和 demo 时调用真实 API |
| Java 凭据管理跨平台 | Windows Credential Manager API 在 macOS/Linux 不可用 | 抽象接口，按平台加载不同实现，或使用 `keyring` 库 |
| 工具执行安全 | Shell 命令可能有副作用 | 护栏 + 沙箱（建议在 Docker 内运行 harness） |
| 上下文窗口管理 | 多轮对话可能超出 token 限制 | 截断策略：保留系统提示 + 最近 3 轮 |
| 护栏正则覆盖不全 | 危险命令可能绕过正则 | 白名单模式优于黑名单，但初期先做黑名单 |