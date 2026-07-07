# 重构设计：Git Worktrees + PR 流程 + Agent 功能修复

> 日期：2026-07-07
> 状态：已批准

---

## 1. 背景与目标

### 1.1 问题

当前仓库存在两类问题：

**流程问题**：
- 46 个 commit 全在 master 一条线上，没有 feature 分支、没有 PR、没有 merge commit
- 违反通用要求 §4.6（git worktrees 隔离工作区）和 §4.7（完整 commit 历史与 PR 工作流）
- PLAN.md 未标记 task 完成状态和 commit hash
- commit message 未标注 subagent

**功能问题**：
- Agent 无法完成基本任务（空转 10 轮）
- DeepSeek tool calling 解析不匹配
- 凭据管理仅内存模式，未实现安全存储
- README/AGENTS.md 过时（仍写 Claude API）

### 1.2 目标

通过全量 git 历史重构，同时修复 agent 功能，最终产出：
- 干净的 PR 工作流历史（11 个 feature 分支 → PR → merge）
- 真正能干活的 coding agent（支持任务执行 + 聊天）
- 满足作业全部硬性要求

### 1.3 约束

- Harness LLM provider：DeepSeek（已实现，保留）
- 开发工具：GLM（glm-5.2，作为编码智能体执行重构）
- 不使用 Claude Code 作为接口

---

## 2. 方案选择

**方案 A：顺序 feature 分支**（已选）

```
master (reset 到 scaffolding)
  │
  ├── PR #1: scaffolding (base，reset 点)
  ├── PR #2: llm-layer
  ├── PR #3: tools
  ├── PR #4: guardrails
  ├── PR #5: config-memory
  ├── PR #6: feedback
  ├── PR #7: core-loop ★重点
  ├── PR #8: deepseek-provider
  ├── PR #9: cli + demo
  ├── PR #10: ci-docker
  └── PR #11: web-ui + render
```

每个 PR 基于前一个 PR 合并后的 master，冲突可控，bugfix 折叠到对应 feature 分支。

---

## 3. Feature 分支详细设计

### PR #1: scaffolding（base）
- AGENTS.md, .gitignore, SPEC.md, PLAN.md, Maven 骨架
- 这是 reset 点，master 保留到此

### PR #2: llm-layer
- LlmProvider 接口, MockLlmProvider, Conversation, Message
- **修复**：统一 tool calling 响应格式，支持 DeepSeek 的 OpenAI-compatible 格式

### PR #3: tools
- Tool 接口, FileTool, ShellTool, GitTool, SearchTool, ToolRegistry
- **修复**：工具参数 schema 清晰化，让 LLM 能正确调用

### PR #4: guardrails
- Guardrail 链, Command/File/Network 护栏
- **修复**：InvalidPathException 捕获，通配符路径处理

### PR #5: config-memory
- HarnessConfig, ConfigLoader, CredentialManager, MemoryStore, MemoryRetriever
- **修复**：
  - 配置解析 ignoreUnknown
  - 跨会话记忆保存
  - **.env 文件加载**（§3.1 凭据安全）
  - **首次运行引导**（隐藏输入）
  - **查看/更新/清除**（查看时掩码显示）

### PR #6: feedback
- Feedback, FailureClassifier, SeverityJudge, FeedbackPipeline
- **修复**：反馈回灌到上下文，驱动自我修正

### PR #7: core-loop ★重点
- Action, ActionParser, ContextAssembler, StopCondition, AgentLoop
- **修复**：
  - tool calling 解析（当前空转的根因）
  - 空响应 guard
  - 工具输出回显
  - 系统提示词包含工具参数说明
  - 停机判断优化
  - **聊天能力**：纯文本响应触发等待用户输入
  - **两种模式**：run（自动执行到完成）+ chat（交互式对话）

### PR #8: deepseek-provider
- DeepSeekProvider, 配置切换
- **修复**：raw response 日志, null content 处理, API key 逻辑（先判断 provider 再检查对应 key）

### PR #9: cli + demo
- Picocli CLI, 机制演示（3 个确定性测试）
- **修复**：
  - 环境变量传递
  - **config set-key 隐藏输入**（System.console().readPassword()）
  - **config show-key 显示掩码**（sk-****-xxxx）
  - **chat 命令**（交互式聊天模式）

### PR #10: ci-docker
- GitHub Actions, Dockerfile
- **修复**：
  - openjdk-21-jre-headless
  - 动态 PORT
  - mvn 缓存
  - **CI 增加 docker-build job**（§4.8 容器分发要求）
  - **Docker 镜像推送到公开 registry**（§3.2）

### PR #11: web-ui + render
- Flask + Socket.IO, Render 部署
- **修复**：
  - ANSI 移除（前端 CSS 着色）
  - 字体（Inter + JetBrains Mono）
  - 会话历史侧边栏
  - session memory 跨运行保存
  - **聊天界面**（像 ChatGPT，工具调用可折叠展示）
  - **README 全面更新**（DeepSeek、部署架构、CI/CD、安全配置、已知限制）
  - **AGENTS.md 更新**（DeepSeek 为默认）

---

## 4. Agent 最终行为设计

### 4.1 AgentLoop 逻辑

```
loop:
  response = LLM.call(user_message + history + context)
  
  if response.has_tool_call():
    result = execute_tool(response.tool_call)
    feed_back(result)
    continue loop          # 继续循环，可能再调工具
  
  else:  # 纯文本
    display(response.text)
    user_input = wait_for_user()   # 等待用户下一条
    if user_input == "退出":
      stop
    else:
      add_to_history(user_input)
      continue loop          # 带着新输入继续
```

### 4.2 CLI 命令

```bash
# 任务模式（自动执行到完成）
java -jar harness.jar run "创建 HelloWorld.java"

# 聊天模式（交互式对话）
java -jar harness.jar chat

# 凭据管理
java -jar harness.jar config set-key    # 隐藏输入
java -jar harness.jar config show-key   # 显示掩码 sk-****-xxxx
java -jar harness.jar config clear-key
```

### 4.3 Web UI

- 左侧：会话历史列表
- 右侧：聊天界面（像 ChatGPT）
- 用户输入消息 → agent 回复（可能带工具调用过程展示）
- 工具调用过程可折叠显示
- LLM 自动判断是聊天还是执行任务

---

## 5. 缺口修复对照表

| 要求 | 修复 PR | 状态 |
|------|---------|------|
| §3.1 凭据安全存储（.env + 引导 + 查看/更新/清除） | PR #5, #9 | 计划中 |
| §3.2 Docker 推送公开 registry | PR #10 | 计划中 |
| §4.6 两阶段评审 | 全程 | 计划中 |
| §4.7 PLAN.md 标记完成+commit hash | 全程 | 计划中 |
| §4.7 commit/PR 标注 subagent | 全程 | 计划中 |
| §4.8 CI 构建 Docker 镜像 | PR #10 | 计划中 |
| §五.7 最后一次 CI pass | 最终 | 计划中 |
| §五.8 REFLECTION.md | 学生本人写 | 待办 |
| README 过时 | PR #11 | 计划中 |
| AGENTS.md 过时 | PR #11 | 计划中 |
| Agent 功能修复 | PR #7, #8 | 计划中 |
| 聊天能力 | PR #7, #9, #11 | 计划中 |

---

## 6. Skill 设计

### 名称：`refactor-with-worktrees`

### 位置：`~/.config/opencode/superpowers/skills/refactor-with-worktrees/SKILL.md`

### 作用：约束重构过程，确保每个 PR 符合标准，agent 功能真正修复

### 检查清单（每个 feature 分支）：

1. Worktree 创建（.worktrees/ 已 gitignored）
2. TDD 强制（红→绿→重构）
3. 功能验收（按分支不同）
4. 测试通过（mvn test 全绿）
5. PR 规范（commit message 标注 subagent、PR 描述、PLAN.md 更新）
6. CI/CD 验证（PR merge 前 CI 必须 pass）
7. 关键检查点（PR #7/#8/#11 合并后跑通完整流程）

---

## 7. 风险

| 风险 | 对策 |
|------|------|
| cherry-pick 冲突 | 每个 PR 基于前一个合并后的 master，顺序进行 |
| force push 数据丢失 | 重置前备份 master 到 master-backup 分支 |
| agent 功能修复不彻底 | PR #7/#8 关键检查点不可跳过 |
| CI 不通过 | 每个 PR merge 前必须 CI pass |
