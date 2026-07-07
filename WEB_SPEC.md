# WEB_SPEC.md — Coding Agent Harness Web UI

## 1. 概述

为 Coding Agent Harness 添加 Web 界面，用户通过浏览器与 agent 交互。

## 2. 架构

```
Browser (ChatGPT-style Chat UI)
    │  WebSocket (Socket.IO)
    ▼
Python Flask + Flask-SocketIO (Render)
    │  subprocess
    ▼
harness.jar (AgentLoop → DeepSeek API)
```

## 3. 后端 (Python)

### 3.1 技术栈
- Flask 3.x + Flask-SocketIO 5.x
- 依赖：flask, flask-socketio, python-dotenv, gunicorn (生产)

### 3.2 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/` | GET | 静态文件 index.html |
| `/static/<path>` | GET | CSS/JS 静态资源 |

### 3.3 WebSocket 事件

| 事件 | 方向 | 载荷 | 说明 |
|------|------|------|------|
| `connect` | 客户端→服务端 | — | 连接建立 |
| `run_task` | 客户端→服务端 | `{task: "..."}` | 发起任务 |
| `stdout` | 服务端→客户端 | `{line: "..."}` | 逐行输出 |
| `task_done` | 服务端→客户端 | `{summary: "..."}` | 任务完成 |
| `task_error` | 服务端→客户端 | `{error: "..."}` | 任务失败 |
| `disconnect` | 客户端→服务端 | — | 断开连接 |

### 3.4 子进程管理

```python
# 启动 harness
proc = subprocess.Popen(
    ["java", "-jar", "harness.jar", "run", task],
    stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    text=True, bufsize=1
)
# 逐行推送
for line in proc.stdout:
    socketio.emit("stdout", {"line": line.rstrip()})
```

## 4. 前端 (HTML/CSS/JS)

### 4.1 设计系统

基于 VS Code Dark+ 配色：

| 用途 | 颜色 | 色值 |
|------|------|------|
| 背景 | 主背景 | `#1e1e1e` |
| 侧边栏 | 侧边栏背景 | `#252526` |
| 输入区 | 输入区背景 | `#2d2d30` |
| 文字 | 主文字 | `#cccccc` |
| 强调 | 蓝色 | `#569cd6` |
| 成功 | 绿色 | `#4ec9b0` |
| 错误 | 红色 | `#f44747` |
| 警告 | 黄色 | `#dcdcaa` |
| 边框 | 分隔线 | `#3e3e42` |

### 4.2 布局

```
┌──────────────────────────────────────────────────┐
│  标题栏 (拖拽区域)                      [状态]    │
├────────────┬─────────────────────────────────────┤
│  工具面板   │  终端输出区                          │
│  📁 file   │  $ echo hello                        │
│  💻 shell  │  hello                               │
│  📦 git    │                                      │
│  🔍 search │  ✅ 编译成功                          │
│ ────────── │                                      │
│  会话列表   │                                      │
│  > 任务1   │  ────────────────────────            │
│  > 任务2   │  > _                         [发送]  │
└────────────┴─────────────────────────────────────┘
```

### 4.3 组件

| 组件 | 说明 |
|------|------|
| 标题栏 | 项目名称 + 连接状态指示器 |
| 工具面板 | 显示可用工具列表（图标+名称） |
| 会话列表 | 历史会话，点击切换 |
| 终端区 | 命令输出，等宽字体，自动滚动 |
| 输入区 | 底部固定，单行输入 + 发送按钮 |

### 4.4 交互细节

- 连接时状态指示器变绿
- 任务执行时输入框禁用，显示"执行中..."
- 终端输出逐行追加，自动滚动到底部
- 工具调用行以蓝色前缀 `📄` 标记
- 错误行以红色前缀 `✗` 标记
- 成功行以绿色前缀 `✓` 标记
- 护栏拦截以黄色前缀 `⚠` 标记

## 5. 部署

- 平台：Render
- 构建：`pip install -r requirements.txt`
- 运行：`gunicorn -k gevent -w 1 app:app`
- 环境变量：`DEEPSEEK_API_KEY`（从 Render 环境变量注入）
- 前端 + 后端同进程，无需 CDN

## 6. 验收标准

- 浏览器打开后显示 Chat 风格界面（用户消息在右、agent 在左）
- 输入消息后 agent 开始执行，消息流实时显示输出
- 工具调用以可折叠块展示，错误、护栏拦截以不同颜色区分
- 连接断开时状态指示器变红
- `python app.py` 一键启动
- 可在 Render 上部署运行