# Coding Agent Harness

AI4SE 期末项目 · A 方向。一个从零构建的 Java Coding Agent Harness。

## 快速开始

### 前提

- Java 17+
- Maven 3.8+
- DeepSeek API Key

### 构建

```bash
mvn package -DskipTests
```

### 配置 API Key

```bash
# 方式 1: .env 文件（推荐）
echo "DEEPSEEK_API_KEY=sk-xxx" > .env

# 方式 2: CLI 命令（隐藏输入）
java -jar target/harness-1.0.0.jar config set-key

# 方式 3: 环境变量
export DEEPSEEK_API_KEY=sk-xxx
```

### 运行

```bash
# 任务模式
java -jar target/harness-1.0.0.jar run "创建 HelloWorld.java 并运行"

# 聊天模式
java -jar target/harness-1.0.0.jar chat
```

### Docker

```bash
docker pull ghcr.io/huliice123-afk/ai4se-harness:latest
docker run -it -e DEEPSEEK_API_KEY=sk-xxx ai4se-harness run "task"
```

### 测试

```bash
mvn test
```

## 目录结构

```
src/main/java/ai4se/harness/
├── HarnessApp.java          # CLI 入口（run / chat / config 子命令）
├── core/                    # 主循环与编排
│   ├── AgentLoop.java       #   agent 主循环（run + chat 双模式）
│   ├── Action.java          #   工具调用动作
│   ├── ActionParser.java    #   解析 LLM 响应为 Action
│   ├── ContextAssembler.java#   组装上下文（系统提示 + 记忆 + 历史）
│   └── StopCondition.java   #   停止条件判定
├── llm/                     # LLM 抽象层
│   ├── LlmProvider.java     #   供应商接口
│   ├── DeepSeekProvider.java#   DeepSeek（OpenAI 兼容，tool_call 解析）
│   ├── ClaudeProvider.java  #   Claude（备选）
│   ├── MockLlmProvider.java #   测试用 Mock
│   ├── Conversation.java    #   对话历史容器
│   ├── Message.java         #   消息结构
│   └── LlmResponse.java     #   LLM 响应封装
├── tools/                   # 工具层
│   ├── Tool.java            #   工具接口
│   ├── ToolRegistry.java    #   工具注册表
│   ├── ToolDefinition.java  #   工具定义（JSON Schema）
│   ├── ToolResult.java      #   工具执行结果
│   ├── FileTool.java        #   文件读写
│   ├── ShellTool.java       #   Shell 命令执行
│   ├── GitTool.java         #   Git 操作
│   └── SearchTool.java      #   代码搜索
├── guardrails/              # 三层治理护栏
│   ├── Guardrail.java       #   护栏接口
│   ├── GuardrailChain.java  #   护栏链
│   ├── GuardResult.java     #   护栏判定结果（pass/block/hitl）
│   ├── CommandGuardrail.java#   命令黑名单拦截
│   ├── FileGuardrail.java   #   文件路径越界拦截
│   └── NetworkGuardrail.java#   网络访问拦截
├── feedback/                # 反馈闭环
│   ├── FeedbackPipeline.java#   反馈流水线
│   ├── FeedbackCollector.java#  反馈采集
│   ├── Feedback.java        #   反馈结构
│   ├── FailureClassifier.java#  失败分类
│   ├── FailureType.java     #   失败类型枚举
│   ├── SeverityJudge.java   #   严重度判定
│   ├── Severity.java        #   严重度枚举
│   └── CorrectionSuggester.java# 修正建议
├── memory/                  # 上下文与记忆
│   ├── MemoryStore.java     #   记忆存储接口
│   ├── FileMemoryStore.java #   文件持久化实现
│   └── MemoryRetriever.java #   记忆检索
└── config/                  # 配置与凭据
    ├── HarnessConfig.java   #   配置模型
    ├── ConfigLoader.java    #   YAML 配置加载
    └── CredentialManager.java#  API Key 安全录入（.env）

web/                         # Web UI（Python Flask + Socket.IO）
├── app.py                   #   Flask 后端，调用 harness JAR
├── templates/index.html     #   聊天界面
├── static/app.js            #   前端逻辑（消息解析、工具块折叠）
├── static/style.css         #   样式（Inter + JetBrains Mono）
├── requirements.txt         #   Python 依赖
└── Dockerfile               #   Web 部署镜像
```

## 安全

- API Key 通过 .env 文件或 CLI 安全录入，绝不硬编码
- `config show-key` 显示掩码（`sk-****-xxxx`）
- 危险命令执行前自动拦截（rm -rf、sudo、chmod 777 等）
- 文件操作限定在项目根目录内
- 网络访问默认拦截

## 部署架构

- **Web UI**: Python Flask + Socket.IO（实时流式输出）
- **部署平台**: Render（Blueprint 一键部署）
- **CI/CD**: GitHub Actions（unit-test + docker-build）
- **Docker 镜像**: GitHub Container Registry（ghcr.io）

## 已知限制

- 仅支持 DeepSeek API（Claude 为备选实现）
- 凭据存储为 .env 明文文件（进程环境可见）

## Web UI

ChatGPT 风格的聊天界面，通过浏览器与 agent 交互。LLM 自动决定回复文本或调用工具，工具调用以可折叠块展示。

### 本地启动

```bash
# 1. 构建 JAR
mvn package -DskipTests

# 2. 安装 Python 依赖
cd web
pip install -r requirements.txt

# 3. 启动
python app.py
# 浏览器打开 http://localhost:5000
```

### Docker 部署

```bash
docker build -f web/Dockerfile -t coding-agent-harness-web .
docker run -p 5000:5000 -e DEEPSEEK_API_KEY=sk-xxx coding-agent-harness-web
```

### Render 部署

项目根目录包含 `render.yaml`，可直接通过 Render Blueprint 一键部署：

1. 在 [Render](https://render.com) 创建 Blueprint 实例
2. 连接 GitHub 仓库
3. 设置环境变量 `DEEPSEEK_API_KEY`
4. 自动部署
