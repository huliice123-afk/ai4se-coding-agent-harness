# Coding Agent Harness

AI4SE 期末项目 · A 方向。一个从零构建的 Java Coding Agent Harness。

## 快速开始

### 前提

- Java 17+
- Maven 3.8+
- Anthropic API Key

### 构建

```bash
mvn package -DskipTests
```

### 配置 API Key

```bash
java -jar target/harness-1.0.0.jar config set-key
```

### 运行

```bash
java -jar target/harness-1.0.0.jar run "write a HelloWorld.java"
```

### Docker

```bash
docker build -t ai4se-harness .
docker run -it -e ANTHROPIC_API_KEY=sk-ant-... ai4se-harness run "task"
```

### 测试

```bash
mvn test
```

## 目录结构

```
src/main/java/ai4se/harness/
├── core/          # 主循环、Agent 核心
├── llm/           # LLM 抽象层（Claude + Mock）
├── tools/         # 文件、Shell、Git、搜索工具
├── guardrails/    # 三层治理护栏
├── feedback/      # 反馈闭环（重点维度）
├── memory/        # 上下文与记忆
└── config/        # 配置与凭据管理
```

## 安全

- API Key 通过操作系统凭据存储管理，绝不硬编码
- 危险命令执行前自动拦截
- 文件操作限定在项目根目录内

## 已知限制

- 凭据存储仅支持内存模式（Windows Credential Manager 集成待完成）
- 仅支持 Claude API，其他 LLM 供应商待扩展

## Web UI

IDE 风格的 Web 界面，通过浏览器与 agent 交互。

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
docker run -p 5000:5000 -e ANTHROPIC_API_KEY=sk-ant-... coding-agent-harness-web
```

### Render 部署

项目根目录包含 `render.yaml`，可直接通过 Render Blueprint 一键部署：

1. 在 [Render](https://render.com) 创建 Blueprint 实例
2. 连接 GitHub 仓库
3. 设置环境变量 `ANTHROPIC_API_KEY`
4. 自动部署