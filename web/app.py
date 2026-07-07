import subprocess
import os
import sys
from flask import Flask, render_template
from flask_socketio import SocketIO, emit

app = Flask(__name__, template_folder="templates", static_folder="static", static_url_path="")
app.config["SECRET_KEY"] = os.urandom(24).hex()
socketio = SocketIO(app, cors_allowed_origins="*", async_mode="threading")

HARNESS_JAR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "target", "harness-1.0.0.jar")
HARNESS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

@app.route("/")
def index():
    return render_template("index.html")

@socketio.on("connect")
def handle_connect():
    pass

@socketio.on("run_task")
def handle_run_task(data):
    task = data.get("task", "").strip()
    if not task:
        emit("task_error", {"error": "Empty task"})
        return

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
            emit("stdout", {"line": line})
        proc.wait()
        emit("task_done", {"summary": "Task completed"})
    except Exception as e:
        emit("task_error", {"error": str(e)})

if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    socketio.run(app, host="0.0.0.0", port=port, debug=False)