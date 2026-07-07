const socket = io();
const messages = document.getElementById("messages");
const input = document.getElementById("task-input");
const sendBtn = document.getElementById("send-btn");
const statusDot = document.getElementById("status");
const sessionList = document.getElementById("session-list");
const newSessionBtn = document.getElementById("new-session-btn");

let executing = false;
let sessionCount = 0;
let currentAgentBubble = null;
let currentToolBlock = null;

function scrollToBottom() {
  messages.scrollTop = messages.scrollHeight;
}

function addUserMessage(text) {
  const row = document.createElement("div");
  row.className = "msg-row user";
  const bubble = document.createElement("div");
  bubble.className = "bubble user-bubble";
  bubble.textContent = text;
  row.appendChild(bubble);
  messages.appendChild(row);
  scrollToBottom();
}

function startAgentBubble() {
  const row = document.createElement("div");
  row.className = "msg-row agent";
  const bubble = document.createElement("div");
  bubble.className = "bubble agent-bubble";
  row.appendChild(bubble);
  messages.appendChild(row);
  currentAgentBubble = bubble;
  currentToolBlock = null;
  scrollToBottom();
  return bubble;
}

function appendAgentText(text) {
  if (!currentAgentBubble) startAgentBubble();
  if (currentAgentBubble.textContent.length > 0) currentAgentBubble.textContent += "\n";
  currentAgentBubble.textContent += text;
  scrollToBottom();
}

function startToolCall(toolName) {
  const row = document.createElement("div");
  row.className = "msg-row agent";

  const block = document.createElement("div");
  block.className = "tool-block";

  const header = document.createElement("div");
  header.className = "tool-header";
  header.innerHTML = '<span class="tool-caret">&#9654;</span>' +
    '<span class="tool-name">' + escapeHtml(toolName) + '</span>' +
    '<span class="tool-status">running</span>';

  const body = document.createElement("div");
  body.className = "tool-body";

  block.appendChild(header);
  block.appendChild(body);

  header.addEventListener("click", function () {
    block.classList.toggle("open");
    body.style.display = block.classList.contains("open") ? "block" : "none";
  });

  row.appendChild(block);
  messages.appendChild(row);

  currentToolBlock = { block: block, body: body, header: header, name: toolName };
  currentAgentBubble = null;
  scrollToBottom();
}

function appendToolResult(toolName, output) {
  if (!currentToolBlock || currentToolBlock.name !== toolName) {
    startToolCall(toolName);
  }
  const tb = currentToolBlock;
  const line = document.createElement("div");
  line.className = "tool-output";
  line.textContent = output;
  tb.body.appendChild(line);
  const status = tb.header.querySelector(".tool-status");
  if (status) status.textContent = "done";
  tb.body.style.display = tb.block.classList.contains("open") ? "block" : "none";
  scrollToBottom();
}

function appendBlocked(reason) {
  const row = document.createElement("div");
  row.className = "msg-row agent";
  const bubble = document.createElement("div");
  bubble.className = "bubble blocked-bubble";
  bubble.textContent = "Blocked: " + reason;
  row.appendChild(bubble);
  messages.appendChild(row);
  currentAgentBubble = null;
  currentToolBlock = null;
  scrollToBottom();
}

function appendInfo(text) {
  const row = document.createElement("div");
  row.className = "msg-row agent";
  const bubble = document.createElement("div");
  bubble.className = "bubble info-bubble";
  bubble.textContent = text;
  row.appendChild(bubble);
  messages.appendChild(row);
  currentAgentBubble = null;
  currentToolBlock = null;
  scrollToBottom();
}

function appendContinuation(text) {
  if (currentToolBlock) {
    const line = document.createElement("div");
    line.className = "tool-output";
    line.textContent = text;
    currentToolBlock.body.appendChild(line);
  } else if (currentAgentBubble) {
    currentAgentBubble.textContent += "\n" + text;
  } else {
    appendInfo(text);
  }
  scrollToBottom();
}

function escapeHtml(s) {
  const d = document.createElement("div");
  d.textContent = s;
  return d.innerHTML;
}

function handleLine(line) {
  let m;
  if ((m = line.match(/^\[Tool\] (.+?) \u2192 (.*)$/))) {
    appendToolResult(m[1], m[2]);
    return;
  }
  if ((m = line.match(/^\[Tool\] (.+)$/))) {
    startToolCall(m[1]);
    return;
  }
  if ((m = line.match(/^\[Agent\] (.*)$/))) {
    appendAgentText(m[1]);
    return;
  }
  if ((m = line.match(/^\[Blocked\] (.*)$/))) {
    appendBlocked(m[1]);
    return;
  }
  if ((m = line.match(/^\[WARNING\] (.*)$/))) {
    appendInfo("Warning: " + m[1]);
    return;
  }
  if (/^\[Round \d+\]/.test(line)) return;
  if (line === "=== Result ===") return;
  if (line.startsWith("Previous session:")) {
    appendInfo(line);
    return;
  }
  if (line.startsWith("Chat mode.") || line === ">") return;
  if (line.trim() === "") return;
  appendContinuation(line);
}

socket.on("connect", function () {
  statusDot.className = "status-dot connected";
  statusDot.title = "Connected";
});

socket.on("disconnect", function () {
  statusDot.className = "status-dot disconnected";
  statusDot.title = "Disconnected";
  executing = false;
  setInputEnabled(true);
});

socket.on("stdout", function (data) {
  handleLine(data.line);
});

socket.on("task_done", function (data) {
  executing = false;
  setInputEnabled(true);
  addSession(data.summary || "Task completed");
});

socket.on("task_error", function (data) {
  executing = false;
  setInputEnabled(true);
  const row = document.createElement("div");
  row.className = "msg-row agent";
  const bubble = document.createElement("div");
  bubble.className = "bubble error-bubble";
  bubble.textContent = data.error || "Error";
  row.appendChild(bubble);
  messages.appendChild(row);
  currentAgentBubble = null;
  currentToolBlock = null;
  scrollToBottom();
});

function setInputEnabled(enabled) {
  input.disabled = !enabled;
  sendBtn.disabled = !enabled;
  if (enabled) input.focus();
}

function sendTask() {
  const task = input.value.trim();
  if (!task || executing) return;

  addUserMessage(task);
  currentAgentBubble = null;
  currentToolBlock = null;

  executing = true;
  setInputEnabled(false);
  input.value = "";
  autoResize();

  socket.emit("run_task", { task: task });
}

function addSession(summary) {
  sessionCount++;
  if (sessionCount === 1) sessionList.innerHTML = "";
  const div = document.createElement("div");
  div.className = "session-item";
  div.innerHTML = '<span class="session-num">' + sessionCount + '</span>' +
    '<span class="session-text">' + escapeHtml(summary.substring(0, 60)) + '</span>';
  sessionList.appendChild(div);
}

function autoResize() {
  input.style.height = "auto";
  input.style.height = Math.min(input.scrollHeight, 160) + "px";
}

input.addEventListener("keydown", function (e) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendTask();
  }
});

input.addEventListener("input", autoResize);
sendBtn.addEventListener("click", sendTask);

newSessionBtn.addEventListener("click", function () {
  messages.innerHTML = "";
  currentAgentBubble = null;
  currentToolBlock = null;
  input.focus();
});

input.focus();
