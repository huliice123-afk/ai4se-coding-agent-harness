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
let resultStarted = false;

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

function friendlyToolName(toolName) {
  const map = {
    "file": "正在操作文件",
    "shell": "正在执行命令",
    "git": "正在执行 Git 操作",
    "search": "正在搜索"
  };
  return map[toolName] || ("正在执行 " + toolName);
}

function friendlyAction(action, params) {
  try {
    const p = JSON.parse(params);
    if (action === "read") return "读取 " + (p.path || "");
    if (action === "write") return "写入 " + (p.path || "");
    if (action === "glob") return "查找 " + (p.pattern || "");
    if (action === "execute") return "运行: " + (p.command || "");
    if (action === "status") return "查看 Git 状态";
    if (action === "diff") return "查看 Git 差异";
    if (action === "log") return "查看 Git 日志";
    if (action === "grep") return "搜索: " + (p.pattern || "");
  } catch (e) {}
  return null;
}

function startToolCall(toolName, detail) {
  const row = document.createElement("div");
  row.className = "msg-row agent";

  const block = document.createElement("div");
  block.className = "tool-block";

  const header = document.createElement("div");
  header.className = "tool-header";
  const label = friendlyToolName(toolName);
  const sub = detail ? '<span class="tool-detail">' + escapeHtml(detail) + '</span>' : '';
  header.innerHTML = '<span class="tool-caret">&#9654;</span>' +
    '<span class="tool-name">' + escapeHtml(label) + '</span>' +
    sub +
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
  bubble.textContent = "该操作被安全拦截";
  row.appendChild(bubble);
  messages.appendChild(row);
  currentAgentBubble = null;
  currentToolBlock = null;
  scrollToBottom();
}

function appendError(text) {
  const friendly = friendlyError(text);
  const row = document.createElement("div");
  row.className = "msg-row agent";
  const bubble = document.createElement("div");
  bubble.className = "bubble error-bubble";
  bubble.textContent = friendly;
  row.appendChild(bubble);
  messages.appendChild(row);
  currentAgentBubble = null;
  currentToolBlock = null;
  scrollToBottom();
}

function friendlyError(text) {
  if (/no files matched/i.test(text)) return "未找到匹配的文件";
  if (/unknown tool/i.test(text)) return "操作不支持";
  if (/missing required parameter/i.test(text)) return "参数不完整";
  if (/file not found/i.test(text)) return "文件不存在";
  if (/timeout/i.test(text)) return "操作超时";
  if (/empty response/i.test(text)) return "未收到响应";
  return text;
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

  // [Round N] → hidden
  if (/^\[Round \d+\]/.test(line)) return;

  // === Result === → stop processing (everything after is duplicate)
  if (line === "=== Result ===") {
    resultStarted = true;
    return;
  }
  if (resultStarted) return;

  // [Agent] text → show
  if ((m = line.match(/^\[Agent\] (.*)$/))) {
    appendAgentText(m[1]);
    return;
  }

  // [Tool] toolName → output → fold into tool block
  if ((m = line.match(/^\[Tool\] (.+?) \u2192 (.*)$/))) {
    appendToolResult(m[1], m[2]);
    return;
  }

  // [Tool] toolName → start tool call (simplified display)
  if ((m = line.match(/^\[Tool\] (.+)$/))) {
    startToolCall(m[1]);
    return;
  }

  // Calling X with {"action":"read",...} → fold into tool block, show friendly action
  if ((m = line.match(/^Calling (\w+) with (.+)$/))) {
    const toolName = m[1];
    const detail = friendlyAction(null, m[2]) || m[2];
    startToolCall(toolName, detail);
    // Also fold the raw params into the body
    const raw = document.createElement("div");
    raw.className = "tool-output";
    raw.textContent = m[2];
    if (currentToolBlock) currentToolBlock.body.appendChild(raw);
    return;
  }

  // [Blocked] → simplified friendly message
  if ((m = line.match(/^\[Blocked\] (.*)$/))) {
    appendBlocked(m[1]);
    return;
  }

  // [WARNING] → simplified
  if ((m = line.match(/^\[WARNING\] (.*)$/))) {
    appendInfo(m[1]);
    return;
  }

  // Error-like lines → friendly
  if (/^(No files matched|Unknown tool|Missing required|File not found|Timeout|Empty response)/i.test(line)) {
    appendError(line);
    return;
  }

  // Previous session → info
  if (line.startsWith("Previous session:")) {
    appendInfo(line);
    return;
  }

  // Chat mode prompts → hidden
  if (line.startsWith("Chat mode.") || line === ">") return;

  // Empty → skip
  if (line.trim() === "") return;

  // Everything else → fold into current tool block or agent bubble
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
  appendError(data.error || "Error");
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
  resultStarted = false;

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
