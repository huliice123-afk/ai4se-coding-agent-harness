# WEB_PLAN.md — Web UI 实现计划

## Task 1: Python 后端 (app.py + requirements.txt)

**Files:**
- Create: `web/requirements.txt`
- Create: `web/app.py`

### Step 1: 创建 requirements.txt

```txt
flask==3.1.0
flask-socketio==5.5.1
python-dotenv==1.1.0
gunicorn==23.0.0
gevent==24.11.1
```

### Step 2: 创建 app.py

```python
import subprocess
import os
import sys
from flask import Flask, send_from_directory
from flask_socketio import SocketIO, emit

app = Flask(__name__, static_folder="static", static_url_path="")
app.config["SECRET_KEY"] = os.urandom(24).hex()
socketio = SocketIO(app, cors_allowed_origins="*", async_mode="gevent")

HARNESS_JAR = os.path.join(os.path.dirname(__file__), "..", "target", "harness-1.0.0.jar")

@app.route("/")
def index():
    return send_from_directory("static", "index.html")

@socketio.on("connect")
def handle_connect():
    emit("stdout", {"line": "Connected to Coding Agent Harness"})

@socketio.on("run_task")
def handle_run_task(data):
    task = data.get("task", "")
    if not task:
        emit("task_error", {"error": "Empty task"})
        return

    emit("stdout", {"line": f"> {task}"})
    try:
        proc = subprocess.Popen(
            ["java", "-jar", HARNESS_JAR, "run", task],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, bufsize=1, cwd=os.path.dirname(HARNESS_JAR)
        )
        for line in proc.stdout:
            emit("stdout", {"line": line.rstrip()})
        proc.wait()
        emit("task_done", {"summary": "Task completed"})
    except Exception as e:
        emit("task_error", {"error": str(e)})

if __name__ == "__main__":
    socketio.run(app, host="0.0.0.0", port=int(os.getenv("PORT", 5000)), debug=True)
```

## Task 2: 前端 HTML (index.html)

**Files:**
- Create: `web/static/index.html`

Single-page IDE-style chat UI with VS Code Dark+ theme.

## Task 3: 前端 CSS (style.css)

**Files:**
- Create: `web/static/style.css`

- VS Code Dark+ color palette
- Left sidebar (240px) + right panel layout
- Terminal-style monospace output
- Color-coded message types
- Smooth scroll and transitions
- Responsive input area

## Task 4: 前端 JS (app.js)

**Files:**
- Create: `web/static/app.js`

Socket.IO client:
- Connect/disconnect handling
- Send task on Enter
- Append stdout lines to terminal
- Auto-scroll to bottom
- Color-code message types
- Disable input during execution

## Task 5: JAR 构建配置

- 确保 `mvn package` 产出 fat JAR（含依赖）
- 更新 pom.xml 添加 maven-assembly-plugin 或 maven-shade-plugin

## Task 6: 部署到 Render

- 创建 `render.yaml` 或手动配置
- 设置环境变量 `DEEPSEEK_API_KEY`
- 验证部署