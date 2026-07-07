import subprocess
import os
import sys
from flask import Flask, send_from_directory
from flask_socketio import SocketIO, emit

app = Flask(__name__, static_folder="static", static_url_path="")
app.config["SECRET_KEY"] = os.urandom(24).hex()
socketio = SocketIO(app, cors_allowed_origins="*", async_mode="gevent")

HARNESS_JAR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "target", "harness-1.0.0.jar")
HARNESS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

@app.route("/")
def index():
    return send_from_directory("static", "index.html")

@socketio.on("connect")
def handle_connect():
    emit("stdout", {"line": "\x1b[32m✓ Connected to Coding Agent Harness\x1b[0m"})

@socketio.on("run_task")
def handle_run_task(data):
    task = data.get("task", "").strip()
    if not task:
        emit("task_error", {"error": "Empty task"})
        return

    emit("stdout", {"line": f"\x1b[36m> {task}\x1b[0m"})

    if not os.path.exists(HARNESS_JAR):
        emit("task_error", {"error": f"JAR not found: {HARNESS_JAR}. Run mvn package first."})
        return

    try:
        proc = subprocess.Popen(
            ["java", "-jar", HARNESS_JAR, "run", task],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, bufsize=1, cwd=HARNESS_DIR
        )
        for line in proc.stdout:
            line = line.rstrip()
            if not line:
                continue
            formatted = format_line(line)
            emit("stdout", {"line": formatted})
        proc.wait()
        emit("task_done", {"summary": "Task completed"})
    except Exception as e:
        emit("task_error", {"error": str(e)})

def format_line(line):
    lower = line.lower()
    if "error" in lower or "fail" in lower or "✗" in line:
        return f"\x1b[31m{line}\x1b[0m"
    if "success" in lower or "complete" in lower or "✓" in line or "pass" in lower:
        return f"\x1b[32m{line}\x1b[0m"
    if "warn" in lower or "block" in lower or "danger" in lower or "⚠" in line:
        return f"\x1b[33m{line}\x1b[0m"
    if "round" in lower or "[" in line:
        return f"\x1b[36m{line}\x1b[0m"
    return line

if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    socketio.run(app, host="0.0.0.0", port=port, debug=False)