const socket = io();
const output = document.getElementById("output");
const input = document.getElementById("task-input");
const sendBtn = document.getElementById("send-btn");
const statusDot = document.getElementById("status");
const sessionList = document.getElementById("session-list");

let executing = false;
let sessionCount = 0;

function appendLine(text, cls) {
  const div = document.createElement("div");
  div.className = "output-line " + (cls || "");
  div.textContent = text;
  output.appendChild(div);
  output.scrollTop = output.scrollHeight;
}

function classifyLine(line) {
  const lower = line.toLowerCase();
  if (lower.includes("error") || lower.includes("fail") || line.includes("✗")) return "error";
  if (lower.includes("success") || lower.includes("complete") || lower.includes("pass") || line.includes("✓")) return "ok";
  if (lower.includes("warn") || lower.includes("block") || lower.includes("danger") || line.includes("⚠")) return "warn";
  if (line.startsWith(">")) return "cmd";
  return "";
}

socket.on("connect", () => {
  statusDot.className = "status-dot connected";
  statusDot.title = "Connected";
  appendLine("✓ Connected to Coding Agent Harness", "ok");
});

socket.on("disconnect", () => {
  statusDot.className = "status-dot disconnected";
  statusDot.title = "Disconnected";
  executing = false;
  setInputEnabled(true);
  appendLine("✗ Disconnected from server", "error");
});

socket.on("stdout", (data) => {
  const cls = classifyLine(data.line);
  appendLine(data.line, cls);
});

socket.on("task_done", (data) => {
  executing = false;
  setInputEnabled(true);
  appendLine("✓ " + (data.summary || "Task completed"), "ok");
  addSession(data.summary);
});

socket.on("task_error", (data) => {
  executing = false;
  setInputEnabled(true);
  appendLine("✗ " + (data.error || "Error"), "error");
});

function setInputEnabled(enabled) {
  input.disabled = !enabled;
  sendBtn.disabled = !enabled;
  if (enabled) input.focus();
}

function sendTask() {
  const task = input.value.trim();
  if (!task || executing) return;

  executing = true;
  setInputEnabled(false);
  input.value = "";

  socket.emit("run_task", { task: task });
}

function addSession(summary) {
  sessionCount++;
  const div = document.createElement("div");
  div.style.cssText = "padding:2px 0;cursor:pointer;color:var(--text-dim);";
  div.textContent = `${sessionCount}. ${summary || "Task " + sessionCount}`;
  sessionList.appendChild(div);
}

input.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendTask();
  }
});

sendBtn.addEventListener("click", sendTask);

input.focus();