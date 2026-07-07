# Coding Agent Harness 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个 Java 实现的 Coding Agent Harness，从零实现 agent 主循环、工具分发、治理护栏、反馈闭环、记忆和配置六个维度，反馈闭环为重点深入维度。

**Architecture:** CLI 应用，用户输入自然语言任务 → 主循环组织上下文 → 调用 LLM → 解析动作 → 护栏检查 → 执行工具 → 反馈采集 → 回灌 LLM 循环。LLM 通过抽象层注入，支持 mock 离线测试。

**Tech Stack:** Java 17, Maven, JUnit 5 + Mockito + AssertJ, OkHttp (HTTP 客户端), Jackson (YAML), Anthropic Claude API, Picocli (CLI 框架)

## Global Constraints

- 所有机制必须是代码实现，不能是提示词
- 每个核心机制必须有用 mock LLM 驱动的确定性单元测试，测试不依赖网络与真实 LLM
- TDD 强制：先写失败测试 → 最小实现 → 重构
- 禁止硬编码凭据，API Key 绝不提交到 Git
- 定期 commit（每个 task 至少一次 commit），禁止单次 commit 提交全部代码
- 每个 task 对应一个 worktree，产生一个 PR（如使用 subagent-driven）

---

## File Structure

```
harness/
├── pom.xml
├── harness.yaml                          # 默认配置模板
├── src/main/java/ai4se/harness/
│   ├── HarnessApp.java                   # CLI 入口，Picocli
│   ├── core/
│   │   ├── AgentLoop.java                # 主循环
│   │   ├── Action.java                   # 动作模型
│   │   ├── ActionParser.java             # 解析 LLM 响应为 Action
│   │   ├── ContextAssembler.java         # 组装上下文
│   │   └── StopCondition.java            # 停机判断
│   ├── llm/
│   │   ├── LlmProvider.java              # LLM 抽象接口
│   │   ├── LlmResponse.java              # LLM 响应模型
│   │   ├── Message.java                  # 消息模型
│   │   ├── Conversation.java             # 对话历史
│   │   ├── ClaudeProvider.java           # Claude API 实现
│   │   └── MockLlmProvider.java          # Mock 实现（脚本+序列模式）
│   ├── tools/
│   │   ├── Tool.java                     # 工具接口
│   │   ├── ToolResult.java               # 工具执行结果
│   │   ├── ToolRegistry.java             # 工具注册与分发
│   │   ├── FileTool.java                 # 文件读写/glob
│   │   ├── ShellTool.java                # Shell 命令执行
│   │   ├── GitTool.java                  # Git 操作
│   │   └── SearchTool.java               # grep/glob 搜索
│   ├── guardrails/
│   │   ├── Guardrail.java                # 护栏接口
│   │   ├── GuardResult.java              # 拦截结果
│   │   ├── GuardrailChain.java           # 链式护栏执行
│   │   ├── CommandGuardrail.java         # 危险命令拦截
│   │   ├── FileGuardrail.java            # 文件路径边界
│   │   └── NetworkGuardrail.java         # 网络请求拦截
│   ├── feedback/
│   │   ├── Feedback.java                 # 反馈模型
│   │   ├── FeedbackCollector.java        # 反馈采集器
│   │   ├── FailureClassifier.java        # 失败分类器 (核心)
│   │   ├── FailureType.java              # 失败类型枚举
│   │   ├── Severity.java                 # 严重性枚举
│   │   ├── SeverityJudge.java            # 严重性判定
│   │   ├── CorrectionSuggester.java      # 修正建议生成
│   │   └── FeedbackPipeline.java         # 反馈流水线
│   ├── memory/
│   │   ├── MemoryStore.java              # 记忆接口
│   │   ├── FileMemoryStore.java          # 文件存储实现
│   │   └── MemoryRetriever.java          # 关键词检索
│   └── config/
│       ├── HarnessConfig.java            # 配置模型
│       ├── ConfigLoader.java             # YAML 配置加载
│       └── CredentialManager.java        # 凭据管理（Windows Credential Manager）
├── src/test/java/ai4se/harness/
│   ├── core/
│   │   ├── AgentLoopTest.java
│   │   ├── ActionParserTest.java
│   │   ├── ContextAssemblerTest.java
│   │   └── StopConditionTest.java
│   ├── llm/
│   │   ├── MockLlmProviderTest.java
│   │   └── ClaudeProviderTest.java
│   ├── tools/
│   │   ├── ToolRegistryTest.java
│   │   ├── FileToolTest.java
│   │   ├── ShellToolTest.java
│   │   ├── GitToolTest.java
│   │   └── SearchToolTest.java
│   ├── guardrails/
│   │   ├── CommandGuardrailTest.java
│   │   ├── FileGuardrailTest.java
│   │   ├── NetworkGuardrailTest.java
│   │   └── GuardrailChainTest.java
│   ├── feedback/
│   │   ├── FeedbackCollectorTest.java
│   │   ├── FailureClassifierTest.java
│   │   ├── SeverityJudgeTest.java
│   │   ├── CorrectionSuggesterTest.java
│   │   └── FeedbackPipelineTest.java
│   ├── memory/
│   │   ├── FileMemoryStoreTest.java
│   │   └── MemoryRetrieverTest.java
│   └── config/
│       ├── ConfigLoaderTest.java
│       └── CredentialManagerTest.java
├── demo/
│   └── DemoTest.java                     # 机制演示（三项确定性演示）
├── Dockerfile
├── .github/workflows/ci.yml
└── README.md
```

---

## Task 1: Maven 项目脚手架

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/ai4se/harness/` (directory)
- Create: `src/test/java/ai4se/harness/` (directory)
- Create: `harness.yaml`

**Interfaces:**
- Produces: Maven 项目结构，含所有依赖（JUnit 5, Mockito, AssertJ, OkHttp, Jackson, Picocli）

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>ai4se</groupId>
    <artifactId>harness</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Coding Agent Harness</name>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.11.4</junit.version>
        <mockito.version>5.14.2</mockito.version>
        <assertj.version>3.27.3</assertj.version>
        <jackson.version>2.18.2</jackson.version>
        <okhttp.version>4.12.0</okhttp.version>
        <picocli.version>4.7.6</picocli.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>${okhttp.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-yaml</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
            <version>${picocli.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.4.2</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>ai4se.harness.HarnessApp</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建默认配置模板 harness.yaml**

```yaml
llm:
  provider: claude
  model: claude-sonnet-4-20250514
  max_tokens: 4096
tools:
  allowed: [file, shell, git, search]
  shell_timeout: 30
guardrails:
  hitl: true
  command_denylist: [rm -rf, sudo, chmod 777, mkfs, shutdown]
  network_blocked: true
  network_allowed_hosts: []
feedback:
  max_rounds: 3
loop:
  max_rounds: 10
memory:
  store_path: .harness/memory
  search_top_k: 3
```

- [ ] **Step 3: 验证 Maven 构建**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add pom.xml harness.yaml
git commit -m "chore: Maven project scaffolding with dependencies"
```

---

## Task 2: LLM 抽象层 — 模型与接口

**Files:**
- Create: `src/main/java/ai4se/harness/llm/Message.java`
- Create: `src/main/java/ai4se/harness/llm/LlmResponse.java`
- Create: `src/main/java/ai4se/harness/llm/LlmProvider.java`
- Create: `src/main/java/ai4se/harness/llm/Conversation.java`
- Create: `src/test/java/ai4se/harness/llm/MockLlmProviderTest.java`

**Interfaces:**
- Produces: `Message(String role, String content)` — role 为 "system"/"user"/"assistant"
- Produces: `LlmResponse(String text, String actionName, Map<String, Object> actionParams, String stopReason)` — LLM 响应
- Produces: `LlmProvider.complete(List<Message> messages, List<Tool> tools) → LlmResponse`
- Produces: `Conversation()` — 对话历史容器，add(Message), getMessages() → List<Message>

- [ ] **Step 1: 写 Message 的失败测试**

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MessageTest {
    @Test
    void shouldCreateMessageWithRoleAndContent() {
        Message msg = new Message("user", "Hello");
        assertThat(msg.getRole()).isEqualTo("user");
        assertThat(msg.getContent()).isEqualTo("Hello");
    }

    @Test
    void shouldRejectNullRole() {
        assertThatThrownBy(() -> new Message(null, "content"))
            .isInstanceOf(NullPointerException.class);
    }
}
```

Run: `mvn test -pl . -Dtest=MessageTest`
Expected: 2 FAIL (Message class not found)

- [ ] **Step 2: 实现 Message**

```java
package ai4se.harness.llm;

import java.util.Objects;

public class Message {
    private final String role;
    private final String content;

    public Message(String role, String content) {
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.content = content;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
}
```

Run: `mvn test -pl . -Dtest=MessageTest`
Expected: 2 PASS

- [ ] **Step 3: 写 LlmResponse 的测试**

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class LlmResponseTest {
    @Test
    void shouldCreateTextResponse() {
        LlmResponse resp = new LlmResponse("done", null, null, "end_turn");
        assertThat(resp.getText()).isEqualTo("done");
        assertThat(resp.hasAction()).isFalse();
    }

    @Test
    void shouldCreateActionResponse() {
        LlmResponse resp = new LlmResponse(null, "shell", Map.of("command", "ls"), "tool_use");
        assertThat(resp.hasAction()).isTrue();
        assertThat(resp.getActionName()).isEqualTo("shell");
        assertThat(resp.getActionParams()).containsEntry("command", "ls");
    }
}
```

Run: `mvn test -pl . -Dtest=LlmResponseTest`
Expected: 2 FAIL

- [ ] **Step 4: 实现 LlmResponse**

```java
package ai4se.harness.llm;

import java.util.Map;

public class LlmResponse {
    private final String text;
    private final String actionName;
    private final Map<String, Object> actionParams;
    private final String stopReason;

    public LlmResponse(String text, String actionName, Map<String, Object> actionParams, String stopReason) {
        this.text = text;
        this.actionName = actionName;
        this.actionParams = actionParams;
        this.stopReason = stopReason;
    }

    public String getText() { return text; }
    public String getActionName() { return actionName; }
    public Map<String, Object> getActionParams() { return actionParams; }
    public String getStopReason() { return stopReason; }
    public boolean hasAction() { return actionName != null; }
}
```

Run: `mvn test -pl . -Dtest=LlmResponseTest`
Expected: 2 PASS

- [ ] **Step 5: 写 LlmProvider 接口**

```java
package ai4se.harness.llm;

import java.util.List;

public interface LlmProvider {
    LlmResponse complete(List<Message> messages, List<ai4se.harness.tools.Tool> tools);
}
```

Note: 后续在 Task 4 实现 Tool 接口后，此接口的 `tools` 参数才能编译通过。先声明接口，编译时允许暂时 import 缺失。

- [ ] **Step 6: 写 Conversation 的测试**

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ConversationTest {
    @Test
    void shouldAddAndRetrieveMessages() {
        Conversation conv = new Conversation();
        conv.add(new Message("user", "hello"));
        conv.add(new Message("assistant", "hi"));
        assertThat(conv.getMessages()).hasSize(2);
        assertThat(conv.getLastMessage().getContent()).isEqualTo("hi");
    }

    @Test
    void shouldReturnNullForEmptyConversation() {
        Conversation conv = new Conversation();
        assertThat(conv.getLastMessage()).isNull();
    }
}
```

Run: `mvn test -pl . -Dtest=ConversationTest`
Expected: 2 FAIL

- [ ] **Step 7: 实现 Conversation**

```java
package ai4se.harness.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Conversation {
    private final List<Message> messages = new ArrayList<>();

    public void add(Message message) { messages.add(message); }
    public List<Message> getMessages() { return Collections.unmodifiableList(messages); }
    public Message getLastMessage() { return messages.isEmpty() ? null : messages.get(messages.size() - 1); }
}
```

Run: `mvn test -pl . -Dtest=ConversationTest`
Expected: 2 PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ai4se/harness/llm/ src/test/java/ai4se/harness/llm/
git commit -m "feat: add LLM abstraction layer (Message, LlmResponse, LlmProvider, Conversation)"
```

---

## Task 3: MockLlmProvider — 脚本模式和序列模式

**Files:**
- Create: `src/main/java/ai4se/harness/llm/MockLlmProvider.java`

**Interfaces:**
- Consumes: `LlmProvider`, `LlmResponse`, `Message` (from Task 2); `Tool` (from Task 4, 先声明接口)
- Produces: `MockLlmProvider()` — 构造函数
- Produces: `MockLlmProvider.whenInputContains(String keyword).thenReturn(LlmResponse response)` — 脚本模式，链式调用
- Produces: `MockLlmProvider.setSequence(List<LlmResponse> responses)` — 序列模式
- Produces: `MockLlmProvider.complete(messages, tools) → LlmResponse` — 按脚本或序列返回

- [ ] **Step 1: 写 MockLlmProvider 的失败测试**

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class MockLlmProviderTest {
    @Test
    void shouldReturnScriptedResponse() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.whenInputContains("write test").thenReturn(
            new LlmResponse(null, "shell", Map.of("command", "mvn test"), "tool_use")
        );

        LlmResponse resp = mock.complete(
            List.of(new Message("user", "please write test")),
            List.of()
        );

        assertThat(resp.hasAction()).isTrue();
        assertThat(resp.getActionName()).isEqualTo("shell");
        assertThat(resp.getActionParams()).containsEntry("command", "mvn test");
    }

    @Test
    void shouldReturnDefaultResponseWhenNoMatch() {
        MockLlmProvider mock = new MockLlmProvider();
        LlmResponse resp = mock.complete(
            List.of(new Message("user", "unknown task")),
            List.of()
        );

        assertThat(resp.getText()).isEqualTo("Task completed.");
        assertThat(resp.getStopReason()).isEqualTo("end_turn");
    }

    @Test
    void shouldReturnSequenceResponses() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "step1"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "step2"), "tool_use"),
            new LlmResponse("all done", null, null, "end_turn")
        ));

        assertThat(mock.complete(List.of(), List.of()).getActionParams().get("command")).isEqualTo("step1");
        assertThat(mock.complete(List.of(), List.of()).getActionParams().get("command")).isEqualTo("step2");
        assertThat(mock.complete(List.of(), List.of()).getText()).isEqualTo("all done");
    }

    @Test
    void shouldFallbackToDefaultAfterSequenceExhausted() {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse("only one", null, null, "end_turn")
        ));

        mock.complete(List.of(), List.of());
        LlmResponse resp = mock.complete(List.of(), List.of());
        assertThat(resp.getText()).isEqualTo("Task completed.");
    }
}
```

Run: `mvn test -pl . -Dtest=MockLlmProviderTest`
Expected: 4 FAIL

- [ ] **Step 2: 实现 MockLlmProvider**

```java
package ai4se.harness.llm;

import java.util.*;

public class MockLlmProvider implements LlmProvider {
    private final Map<String, LlmResponse> script = new LinkedHashMap<>();
    private final List<LlmResponse> sequence = new ArrayList<>();
    private int sequenceIndex = 0;

    public MockLlmProvider whenInputContains(String keyword) {
        return new ScriptBuilder(this, keyword);
    }

    void addScript(String keyword, LlmResponse response) {
        script.put(keyword, response);
    }

    public void setSequence(List<LlmResponse> responses) {
        sequence.clear();
        sequence.addAll(responses);
        sequenceIndex = 0;
    }

    @Override
    public LlmResponse complete(List<Message> messages, List<ai4se.harness.tools.Tool> tools) {
        if (sequenceIndex < sequence.size()) {
            return sequence.get(sequenceIndex++);
        }
        String combined = messages.stream().map(Message::getContent).reduce("", (a, b) -> a + " " + b);
        for (Map.Entry<String, LlmResponse> entry : script.entrySet()) {
            if (combined.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new LlmResponse("Task completed.", null, null, "end_turn");
    }

    public static class ScriptBuilder {
        private final MockLlmProvider mock;
        private final String keyword;

        ScriptBuilder(MockLlmProvider mock, String keyword) {
            this.mock = mock;
            this.keyword = keyword;
        }

        public void thenReturn(LlmResponse response) {
            mock.addScript(keyword, response);
        }
    }
}
```

Run: `mvn test -pl . -Dtest=MockLlmProviderTest`
Expected: 4 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/llm/MockLlmProvider.java src/test/java/ai4se/harness/llm/
git commit -m "feat: add MockLlmProvider with script and sequence modes"
```

---

## Task 4: 工具接口与 ToolRegistry

**Files:**
- Create: `src/main/java/ai4se/harness/tools/Tool.java`
- Create: `src/main/java/ai4se/harness/tools/ToolResult.java`
- Create: `src/main/java/ai4se/harness/tools/ToolRegistry.java`
- Create: `src/test/java/ai4se/harness/tools/ToolRegistryTest.java`

**Interfaces:**
- Produces: `Tool.name() → String`, `Tool.description() → String`, `Tool.execute(Map<String, Object> params) → ToolResult`
- Produces: `ToolResult` — `ToolResult(boolean success, String output`, `ToolResult(boolean success, String output, int exitCode)`
- Produces: `ToolRegistry.register(Tool tool)`, `ToolRegistry.get(String name) → Tool`, `ToolRegistry.getAll() → List<Tool>`

- [ ] **Step 1: 写 Tool 接口和 ToolResult 的失败测试**

```java
package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ToolRegistryTest {
    @Test
    void shouldRegisterAndRetrieveTool() {
        ToolRegistry registry = new ToolRegistry();
        Tool tool = new Tool() {
            public String name() { return "test"; }
            public String description() { return "test tool"; }
            public ToolResult execute(Map<String, Object> params) {
                return new ToolResult(true, "ok");
            }
        };
        registry.register(tool);
        assertThat(registry.get("test")).isPresent();
        assertThat(registry.get("test").get().name()).isEqualTo("test");
    }

    @Test
    void shouldReturnEmptyForUnknownTool() {
        ToolRegistry registry = new ToolRegistry();
        assertThat(registry.get("unknown")).isEmpty();
    }

    @Test
    void shouldListAllTools() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new Tool() {
            public String name() { return "a"; }
            public String description() { return "a"; }
            public ToolResult execute(Map<String, Object> p) { return new ToolResult(true, ""); }
        });
        registry.register(new Tool() {
            public String name() { return "b"; }
            public String description() { return "b"; }
            public ToolResult execute(Map<String, Object> p) { return new ToolResult(true, ""); }
        });
        assertThat(registry.getAll()).hasSize(2);
    }
}
```

Run: `mvn test -pl . -Dtest=ToolRegistryTest`
Expected: 3 FAIL

- [ ] **Step 2: 实现 Tool 接口和 ToolResult**

```java
package ai4se.harness.tools;

import java.util.Map;

public interface Tool {
    String name();
    String description();
    ToolResult execute(Map<String, Object> params);
}
```

```java
package ai4se.harness.tools;

public class ToolResult {
    private final boolean success;
    private final String output;
    private final int exitCode;

    public ToolResult(boolean success, String output) {
        this(success, output, success ? 0 : 1);
    }

    public ToolResult(boolean success, String output, int exitCode) {
        this.success = success;
        this.output = output;
        this.exitCode = exitCode;
    }

    public boolean isSuccess() { return success; }
    public String getOutput() { return output; }
    public int getExitCode() { return exitCode; }
}
```

- [ ] **Step 3: 实现 ToolRegistry**

```java
package ai4se.harness.tools;

import java.util.*;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public Optional<Tool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<Tool> getAll() {
        return new ArrayList<>(tools.values());
    }
}
```

Run: `mvn test -pl . -Dtest=ToolRegistryTest`
Expected: 3 PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ai4se/harness/tools/Tool.java src/main/java/ai4se/harness/tools/ToolResult.java src/main/java/ai4se/harness/tools/ToolRegistry.java src/test/java/ai4se/harness/tools/
git commit -m "feat: add Tool interface, ToolResult, and ToolRegistry"
```

---

## Task 5: FileTool — 文件读写与 glob

**Files:**
- Create: `src/main/java/ai4se/harness/tools/FileTool.java`
- Create: `src/test/java/ai4se/harness/tools/FileToolTest.java`

**Interfaces:**
- Consumes: `Tool` (from Task 4)
- Produces: `FileTool(String projectRoot)` — 限定在项目根目录内操作
- Produces: `FileTool.execute(params)` — 支持 action: "read"/"write"/"glob"

- [ ] **Step 1: 写 FileTool 的失败测试**

```java
package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class FileToolTest {
    @Test
    void shouldReadFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "read", "path", "test.txt"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("hello world");
    }

    @Test
    void shouldWriteFile(@TempDir Path tempDir) {
        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "write", "path", "out.txt", "content", "hello"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(Files.exists(tempDir.resolve("out.txt"))).isTrue();
    }

    @Test
    void shouldBlockPathTraversal(@TempDir Path tempDir) {
        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "read", "path", "../../etc/passwd"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOutput()).contains("outside project root");
    }

    @Test
    void shouldReturnErrorForMissingFile(@TempDir Path tempDir) {
        FileTool tool = new FileTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "read", "path", "nonexistent.txt"));

        assertThat(result.isSuccess()).isFalse();
    }
}
```

Run: `mvn test -pl . -Dtest=FileToolTest`
Expected: 4 FAIL

- [ ] **Step 2: 实现 FileTool**

```java
package ai4se.harness.tools;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileTool implements Tool {
    private final Path projectRoot;

    public FileTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String name() { return "file"; }

    @Override
    public String description() { return "Read, write, or glob files within the project"; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "read");
        String path = (String) params.get("path");
        if (path == null) return new ToolResult(false, "Missing required parameter: path");

        Path resolved = projectRoot.resolve(path).normalize();
        if (!resolved.startsWith(projectRoot)) {
            return new ToolResult(false, "Access denied: path outside project root");
        }

        try {
            switch (action) {
                case "read": return readFile(resolved);
                case "write": return writeFile(resolved, (String) params.get("content"));
                case "glob": return globFiles((String) params.getOrDefault("pattern", "*"));
                default: return new ToolResult(false, "Unknown action: " + action);
            }
        } catch (IOException e) {
            return new ToolResult(false, "IO error: " + e.getMessage());
        }
    }

    private ToolResult readFile(Path path) throws IOException {
        if (!Files.exists(path)) return new ToolResult(false, "File not found: " + path);
        String content = Files.readString(path);
        return new ToolResult(true, content);
    }

    private ToolResult writeFile(Path path, String content) throws IOException {
        if (content == null) return new ToolResult(false, "Missing required parameter: content");
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return new ToolResult(true, "Written to " + path);
    }

    private ToolResult globFiles(String pattern) throws IOException {
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            String result = stream
                .filter(p -> p.getFileName().toString().contains(pattern.replace("*", "")))
                .map(p -> projectRoot.relativize(p).toString())
                .limit(50)
                .collect(Collectors.joining("\n"));
            return new ToolResult(true, result.isEmpty() ? "No files matched" : result);
        }
    }
}
```

Run: `mvn test -pl . -Dtest=FileToolTest`
Expected: 4 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/tools/FileTool.java src/test/java/ai4se/harness/tools/FileToolTest.java
git commit -m "feat: add FileTool with read/write/glob and path traversal protection"
```

---

## Task 6: ShellTool — Shell 命令执行

**Files:**
- Create: `src/main/java/ai4se/harness/tools/ShellTool.java`
- Create: `src/test/java/ai4se/harness/tools/ShellToolTest.java`

**Interfaces:**
- Consumes: `Tool` (from Task 4)
- Produces: `ShellTool(long timeoutSeconds)` — 默认超时 30 秒
- Produces: `ShellTool.execute(params)` — params: "command" (String)

- [ ] **Step 1: 写 ShellTool 的失败测试**

```java
package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ShellToolTest {
    @Test
    void shouldExecuteSuccessfulCommand() {
        ShellTool tool = new ShellTool(10);
        ToolResult result = tool.execute(Map.of("command", "echo hello"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("hello");
        assertThat(result.getExitCode()).isEqualTo(0);
    }

    @Test
    void shouldReturnFailureForFailedCommand() {
        ShellTool tool = new ShellTool(10);
        ToolResult result = tool.execute(Map.of("command", "exit 1"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getExitCode()).isEqualTo(1);
    }

    @Test
    void shouldHandleMissingCommand() {
        ShellTool tool = new ShellTool(10);
        ToolResult result = tool.execute(Map.of());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOutput()).contains("Missing required parameter: command");
    }
}
```

Run: `mvn test -pl . -Dtest=ShellToolTest`
Expected: 3 FAIL

- [ ] **Step 2: 实现 ShellTool**

```java
package ai4se.harness.tools;

import java.io.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellTool implements Tool {
    private final long timeoutSeconds;

    public ShellTool(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String name() { return "shell"; }

    @Override
    public String description() { return "Execute shell commands"; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String command = (String) params.get("command");
        if (command == null) return new ToolResult(false, "Missing required parameter: command");

        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolResult(false, "Command timed out after " + timeoutSeconds + "s\n" + output.toString());
            }

            int exitCode = process.exitValue();
            return new ToolResult(exitCode == 0, output.toString(), exitCode);
        } catch (Exception e) {
            return new ToolResult(false, "Execution error: " + e.getMessage());
        }
    }
}
```

Run: `mvn test -pl . -Dtest=ShellToolTest`
Expected: 3 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/tools/ShellTool.java src/test/java/ai4se/harness/tools/ShellToolTest.java
git commit -m "feat: add ShellTool with timeout support"
```

---

## Task 7: GitTool — Git 操作

**Files:**
- Create: `src/main/java/ai4se/harness/tools/GitTool.java`
- Create: `src/test/java/ai4se/harness/tools/GitToolTest.java`

**Interfaces:**
- Consumes: `Tool` (from Task 4)
- Produces: `GitTool(Path projectRoot)` — 在项目根目录执行 git 命令
- Produces: `GitTool.execute(params)` — params: "action" (status/diff/commit/branch/log)

- [ ] **Step 1: 写 GitTool 的失败测试**

```java
package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class GitToolTest {
    @Test
    void shouldReturnGitStatus(@TempDir Path tempDir) throws Exception {
        initGitRepo(tempDir);
        GitTool tool = new GitTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "status"));
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldReturnErrorForNonGitRepo(@TempDir Path tempDir) {
        GitTool tool = new GitTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "status"));
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void shouldHandleUnknownAction(@TempDir Path tempDir) throws Exception {
        initGitRepo(tempDir);
        GitTool tool = new GitTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "unknown"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getOutput()).contains("Unknown action");
    }

    private void initGitRepo(Path dir) throws Exception {
        new ProcessBuilder("git", "init").directory(dir.toFile()).start().waitFor();
    }
}
```

Run: `mvn test -pl . -Dtest=GitToolTest`
Expected: 3 FAIL

- [ ] **Step 2: 实现 GitTool**

```java
package ai4se.harness.tools;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GitTool implements Tool {
    private final Path projectRoot;

    public GitTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String name() { return "git"; }

    @Override
    public String description() { return "Git operations: status, diff, commit, branch, log"; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String action = (String) params.get("action");
        if (action == null) return new ToolResult(false, "Missing required parameter: action");

        try {
            switch (action) {
                case "status": return runGit("status", "--short");
                case "diff": return runGit("diff");
                case "diff-staged": return runGit("diff", "--staged");
                case "log": return runGit("log", "--oneline", "-10");
                case "branch": return runGit("branch");
                default: return new ToolResult(false, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return new ToolResult(false, "Git error: " + e.getMessage());
        }
    }

    private ToolResult runGit(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "git";
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd).directory(projectRoot.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
        }

        process.waitFor(10, TimeUnit.SECONDS);
        int exitCode = process.exitValue();
        return new ToolResult(exitCode == 0, output.toString(), exitCode);
    }
}
```

Run: `mvn test -pl . -Dtest=GitToolTest`
Expected: 3 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/tools/GitTool.java src/test/java/ai4se/harness/tools/GitToolTest.java
git commit -m "feat: add GitTool (status, diff, log, branch)"
```

---

## Task 8: SearchTool — grep 和 glob 搜索

**Files:**
- Create: `src/main/java/ai4se/harness/tools/SearchTool.java`
- Create: `src/test/java/ai4se/harness/tools/SearchToolTest.java`

**Interfaces:**
- Consumes: `Tool` (from Task 4)
- Produces: `SearchTool(Path projectRoot)` — 在项目根目录内搜索
- Produces: `SearchTool.execute(params)` — params: "action" ("grep"/"glob"), "pattern"

- [ ] **Step 1: 写 SearchTool 的失败测试**

```java
package ai4se.harness.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class SearchToolTest {
    @Test
    void shouldGrepContent(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "hello world\nfoo bar");
        SearchTool tool = new SearchTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "grep", "pattern", "hello"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("a.txt");
        assertThat(result.getOutput()).contains("hello world");
    }

    @Test
    void shouldReturnEmptyForNoMatch(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "nothing here");
        SearchTool tool = new SearchTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "grep", "pattern", "nonexistent"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo("No matches found");
    }

    @Test
    void shouldReturnErrorForMissingPattern(@TempDir Path tempDir) {
        SearchTool tool = new SearchTool(tempDir);
        ToolResult result = tool.execute(Map.of("action", "grep"));
        assertThat(result.isSuccess()).isFalse();
    }
}
```

Run: `mvn test -pl . -Dtest=SearchToolTest`
Expected: 3 FAIL

- [ ] **Step 2: 实现 SearchTool**

```java
package ai4se.harness.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class SearchTool implements Tool {
    private final Path projectRoot;

    public SearchTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String name() { return "search"; }

    @Override
    public String description() { return "Search files: grep for content, glob for filenames"; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "grep");
        String pattern = (String) params.get("pattern");
        if (pattern == null) return new ToolResult(false, "Missing required parameter: pattern");

        try {
            switch (action) {
                case "grep": return grep(pattern);
                case "glob": return glob(pattern);
                default: return new ToolResult(false, "Unknown action: " + action);
            }
        } catch (IOException e) {
            return new ToolResult(false, "Search error: " + e.getMessage());
        }
    }

    private ToolResult grep(String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            stream.filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains(File.separator + "."))
                .forEach(p -> {
                    try {
                        for (String line : Files.readAllLines(p)) {
                            if (line.contains(pattern)) {
                                sb.append(projectRoot.relativize(p)).append(": ").append(line).append("\n");
                            }
                        }
                    } catch (IOException ignored) {}
                });
        }
        String result = sb.toString();
        return new ToolResult(true, result.isEmpty() ? "No matches found" : result);
    }

    private ToolResult glob(String pattern) throws IOException {
        String result;
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            result = stream
                .filter(p -> !p.toString().contains(File.separator + "."))
                .filter(p -> p.getFileName().toString().matches(globToRegex(pattern)))
                .map(p -> projectRoot.relativize(p).toString())
                .limit(50)
                .collect(Collectors.joining("\n"));
        }
        return new ToolResult(true, result.isEmpty() ? "No files matched" : result);
    }

    private String globToRegex(String glob) {
        return glob.replace(".", "\\.").replace("*", ".*").replace("?", ".");
    }
}
```

Run: `mvn test -pl . -Dtest=SearchToolTest`
Expected: 3 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/tools/SearchTool.java src/test/java/ai4se/harness/tools/SearchToolTest.java
git commit -m "feat: add SearchTool with grep and glob support"
```

---

## Task 9: 治理护栏 — 接口与 CommandGuardrail

**Files:**
- Create: `src/main/java/ai4se/harness/guardrails/Guardrail.java`
- Create: `src/main/java/ai4se/harness/guardrails/GuardResult.java`
- Create: `src/main/java/ai4se/harness/guardrails/CommandGuardrail.java`
- Create: `src/test/java/ai4se/harness/guardrails/CommandGuardrailTest.java`

**Interfaces:**
- Produces: `Guardrail.check(Map<String, Object> actionParams) → GuardResult`
- Produces: `GuardResult` — `GuardResult(Status status, String reason)`, Status: `PASS`, `BLOCK`, `HITL`
- Produces: `CommandGuardrail(List<String> denylist)` — 支持可配置的危险命令列表

- [ ] **Step 1: 写 Guardrail 接口和 GuardResult**

```java
package ai4se.harness.guardrails;

public enum GuardResult { PASS, BLOCK, HITL }

public interface Guardrail {
    String name();
    GuardResult check(String actionName, java.util.Map<String, Object> actionParams);
}
```

Wait, let me use a class for GuardResult so it carries a reason string. Let me redefine.

```java
package ai4se.harness.guardrails;

public class GuardResult {
    public enum Status { PASS, BLOCK, HITL }

    private final Status status;
    private final String reason;

    public GuardResult(Status status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public static GuardResult pass() { return new GuardResult(Status.PASS, ""); }
    public static GuardResult block(String reason) { return new GuardResult(Status.BLOCK, reason); }
    public static GuardResult hitl(String reason) { return new GuardResult(Status.HITL, reason); }

    public Status getStatus() { return status; }
    public String getReason() { return reason; }
    public boolean isPass() { return status == Status.PASS; }
    public boolean isBlock() { return status == Status.BLOCK; }
    public boolean isHitl() { return status == Status.HITL; }
}
```

```java
package ai4se.harness.guardrails;

import java.util.Map;

public interface Guardrail {
    String name();
    GuardResult check(String actionName, Map<String, Object> actionParams);
}
```

- [ ] **Step 2: 写 CommandGuardrail 的失败测试**

```java
package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class CommandGuardrailTest {
    @Test
    void shouldBlockDangerousCommand() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf", "sudo", "chmod 777"));
        GuardResult result = guard.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldPassSafeCommand() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf", "sudo"));
        GuardResult result = guard.check("shell", Map.of("command", "echo hello"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldPassNonShellAction() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf"));
        GuardResult result = guard.check("file", Map.of("action", "read", "path", "test.txt"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldBlockDropTable() {
        CommandGuardrail guard = new CommandGuardrail(List.of("DROP TABLE", "DELETE FROM"));
        GuardResult result = guard.check("shell", Map.of("command", "echo 'DROP TABLE users' | mysql"));
        assertThat(result.isBlock()).isTrue();
    }
}
```

Run: `mvn test -pl . -Dtest=CommandGuardrailTest`
Expected: 4 FAIL

- [ ] **Step 3: 实现 CommandGuardrail**

```java
package ai4se.harness.guardrails;

import java.util.List;
import java.util.Map;

public class CommandGuardrail implements Guardrail {
    private final List<String> denylist;

    public CommandGuardrail(List<String> denylist) {
        this.denylist = denylist;
    }

    @Override
    public String name() { return "command-guardrail"; }

    @Override
    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        if (!"shell".equals(actionName)) return GuardResult.pass();

        String command = (String) actionParams.get("command");
        if (command == null) return GuardResult.pass();

        String lower = command.toLowerCase();
        for (String dangerous : denylist) {
            if (lower.contains(dangerous.toLowerCase())) {
                return GuardResult.block("Dangerous command detected: " + dangerous);
            }
        }
        return GuardResult.pass();
    }
}
```

Run: `mvn test -pl . -Dtest=CommandGuardrailTest`
Expected: 4 PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ai4se/harness/guardrails/ src/test/java/ai4se/harness/guardrails/
git commit -m "feat: add Guardrail interface, GuardResult, and CommandGuardrail"
```

---

## Task 10: FileGuardrail 和 NetworkGuardrail

**Files:**
- Create: `src/main/java/ai4se/harness/guardrails/FileGuardrail.java`
- Create: `src/main/java/ai4se/harness/guardrails/NetworkGuardrail.java`
- Create: `src/test/java/ai4se/harness/guardrails/FileGuardrailTest.java`
- Create: `src/test/java/ai4se/harness/guardrails/NetworkGuardrailTest.java`

**Interfaces:**
- Consumes: `Guardrail` (from Task 9)
- Produces: `FileGuardrail(Path projectRoot)` — 检查 file 工具的路径是否在项目根目录内
- Produces: `NetworkGuardrail()` — 检查 shell 命令是否包含 curl/wget 等网络请求

- [ ] **Step 1: 写 FileGuardrail 的失败测试**

```java
package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class FileGuardrailTest {
    @Test
    void shouldBlockPathOutsideRoot(@TempDir Path tempDir) {
        FileGuardrail guard = new FileGuardrail(tempDir);
        GuardResult result = guard.check("file", Map.of("action", "read", "path", "../../secret.txt"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldPassPathInsideRoot(@TempDir Path tempDir) {
        FileGuardrail guard = new FileGuardrail(tempDir);
        GuardResult result = guard.check("file", Map.of("action", "read", "path", "src/main.java"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldPassNonFileAction() {
        FileGuardrail guard = new FileGuardrail(Path.of("/tmp"));
        GuardResult result = guard.check("shell", Map.of("command", "ls"));
        assertThat(result.isPass()).isTrue();
    }
}
```

Run: `mvn test -pl . -Dtest=FileGuardrailTest`
Expected: 3 FAIL

- [ ] **Step 2: 实现 FileGuardrail**

```java
package ai4se.harness.guardrails;

import java.nio.file.Path;
import java.util.Map;

public class FileGuardrail implements Guardrail {
    private final Path projectRoot;

    public FileGuardrail(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String name() { return "file-guardrail"; }

    @Override
    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        if (!"file".equals(actionName)) return GuardResult.pass();

        String path = (String) actionParams.get("path");
        if (path == null) return GuardResult.pass();

        Path resolved = projectRoot.resolve(path).normalize();
        if (!resolved.startsWith(projectRoot)) {
            return GuardResult.block("File access outside project root: " + path);
        }
        return GuardResult.pass();
    }
}
```

Run: `mvn test -pl . -Dtest=FileGuardrailTest`
Expected: 3 PASS

- [ ] **Step 3: 写 NetworkGuardrail 的失败测试**

```java
package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class NetworkGuardrailTest {
    @Test
    void shouldBlockCurl() {
        NetworkGuardrail guard = new NetworkGuardrail();
        GuardResult result = guard.check("shell", Map.of("command", "curl http://evil.com"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldBlockWget() {
        NetworkGuardrail guard = new NetworkGuardrail();
        GuardResult result = guard.check("shell", Map.of("command", "wget http://evil.com/file"));
        assertThat(result.isBlock()).isTrue();
    }

    @Test
    void shouldPassNormalCommand() {
        NetworkGuardrail guard = new NetworkGuardrail();
        GuardResult result = guard.check("shell", Map.of("command", "mvn test"));
        assertThat(result.isPass()).isTrue();
    }
}
```

Run: `mvn test -pl . -Dtest=NetworkGuardrailTest`
Expected: 3 FAIL

- [ ] **Step 4: 实现 NetworkGuardrail**

```java
package ai4se.harness.guardrails;

import java.util.List;
import java.util.Map;

public class NetworkGuardrail implements Guardrail {
    private static final List<String> BLOCKED = List.of("curl", "wget", "nc ", "telnet");

    @Override
    public String name() { return "network-guardrail"; }

    @Override
    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        if (!"shell".equals(actionName)) return GuardResult.pass();

        String command = (String) actionParams.get("command");
        if (command == null) return GuardResult.pass();

        String lower = command.toLowerCase();
        for (String blocked : BLOCKED) {
            if (lower.contains(blocked)) {
                return GuardResult.block("Network request blocked: " + blocked);
            }
        }
        return GuardResult.pass();
    }
}
```

Run: `mvn test -pl . -Dtest=NetworkGuardrailTest`
Expected: 3 PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ai4se/harness/guardrails/FileGuardrail.java src/main/java/ai4se/harness/guardrails/NetworkGuardrail.java src/test/java/ai4se/harness/guardrails/
git commit -m "feat: add FileGuardrail and NetworkGuardrail"
```

---

## Task 11: GuardrailChain — 链式护栏执行

**Files:**
- Create: `src/main/java/ai4se/harness/guardrails/GuardrailChain.java`
- Create: `src/test/java/ai4se/harness/guardrails/GuardrailChainTest.java`

**Interfaces:**
- Consumes: `Guardrail` (from Task 9)
- Produces: `GuardrailChain(List<Guardrail> guardrails)` — 链式执行
- Produces: `GuardrailChain.check(actionName, actionParams) → GuardResult` — 短路：第一个非 PASS 即返回

- [ ] **Step 1: 写 GuardrailChain 的失败测试**

```java
package ai4se.harness.guardrails;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class GuardrailChainTest {
    @Test
    void shouldPassWhenAllGuardrailsPass() {
        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf")),
            new NetworkGuardrail()
        ));
        GuardResult result = chain.check("shell", Map.of("command", "echo hello"));
        assertThat(result.isPass()).isTrue();
    }

    @Test
    void shouldBlockAtFirstGuardrail() {
        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf")),
            new NetworkGuardrail()
        ));
        GuardResult result = chain.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isBlock()).isTrue();
        assertThat(result.getReason()).contains("rm -rf");
    }

    @Test
    void shouldBlockAtSecondGuardrail() {
        GuardrailChain chain = new GuardrailChain(List.of(
            new CommandGuardrail(List.of("rm -rf")),
            new NetworkGuardrail()
        ));
        GuardResult result = chain.check("shell", Map.of("command", "curl http://evil.com"));
        assertThat(result.isBlock()).isTrue();
        assertThat(result.getReason()).contains("curl");
    }
}
```

Run: `mvn test -pl . -Dtest=GuardrailChainTest`
Expected: 3 FAIL

- [ ] **Step 2: 实现 GuardrailChain**

```java
package ai4se.harness.guardrails;

import java.util.List;
import java.util.Map;

public class GuardrailChain {
    private final List<Guardrail> guardrails;

    public GuardrailChain(List<Guardrail> guardrails) {
        this.guardrails = guardrails;
    }

    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        for (Guardrail guardrail : guardrails) {
            GuardResult result = guardrail.check(actionName, actionParams);
            if (!result.isPass()) {
                return result;
            }
        }
        return GuardResult.pass();
    }
}
```

Run: `mvn test -pl . -Dtest=GuardrailChainTest`
Expected: 3 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/guardrails/GuardrailChain.java src/test/java/ai4se/harness/guardrails/GuardrailChainTest.java
git commit -m "feat: add GuardrailChain for chained guardrail execution"
```

---

## Task 12: 配置系统 — ConfigLoader 和 CredentialManager

**Files:**
- Create: `src/main/java/ai4se/harness/config/HarnessConfig.java`
- Create: `src/main/java/ai4se/harness/config/ConfigLoader.java`
- Create: `src/main/java/ai4se/harness/config/CredentialManager.java`
- Create: `src/test/java/ai4se/harness/config/ConfigLoaderTest.java`
- Create: `src/test/java/ai4se/harness/config/CredentialManagerTest.java`

**Interfaces:**
- Produces: `HarnessConfig` — 配置 POJO，含 `LlmConfig`, `ToolsConfig`, `GuardrailsConfig`, `FeedbackConfig`, `LoopConfig`, `MemoryConfig`
- Produces: `ConfigLoader.load(Path yamlPath) → HarnessConfig`
- Produces: `CredentialManager` — `storeKey(String key)`, `getKey() → Optional<String>`, `clearKey()`

- [ ] **Step 1: 写 HarnessConfig 和 ConfigLoader 的失败测试**

```java
package ai4se.harness.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class ConfigLoaderTest {
    @Test
    void shouldLoadValidConfig(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("harness.yaml");
        Files.writeString(configPath, """
            llm:
              provider: claude
              model: claude-sonnet-4-20250514
              max_tokens: 4096
            tools:
              allowed: [file, shell, git, search]
              shell_timeout: 30
            guardrails:
              hitl: true
              command_denylist: [rm -rf, sudo]
              network_blocked: true
            feedback:
              max_rounds: 3
            loop:
              max_rounds: 10
            memory:
              store_path: .harness/memory
              search_top_k: 3
            """);

        HarnessConfig config = ConfigLoader.load(configPath);
        assertThat(config.getLlm().getProvider()).isEqualTo("claude");
        assertThat(config.getTools().getAllowed()).contains("file", "shell");
        assertThat(config.getGuardrails().isHitl()).isTrue();
        assertThat(config.getLoop().getMaxRounds()).isEqualTo(10);
    }
}
```

Run: `mvn test -pl . -Dtest=ConfigLoaderTest`
Expected: 1 FAIL

- [ ] **Step 2: 实现 HarnessConfig 和子配置类**

```java
package ai4se.harness.config;

import java.util.List;

public class HarnessConfig {
    private LlmConfig llm;
    private ToolsConfig tools;
    private GuardrailsConfig guardrails;
    private FeedbackConfig feedback;
    private LoopConfig loop;
    private MemoryConfig memory;

    public LlmConfig getLlm() { return llm; }
    public void setLlm(LlmConfig llm) { this.llm = llm; }
    public ToolsConfig getTools() { return tools; }
    public void setTools(ToolsConfig tools) { this.tools = tools; }
    public GuardrailsConfig getGuardrails() { return guardrails; }
    public void setGuardrails(GuardrailsConfig guardrails) { this.guardrails = guardrails; }
    public FeedbackConfig getFeedback() { return feedback; }
    public void setFeedback(FeedbackConfig feedback) { this.feedback = feedback; }
    public LoopConfig getLoop() { return loop; }
    public void setLoop(LoopConfig loop) { this.loop = loop; }
    public MemoryConfig getMemory() { return memory; }
    public void setMemory(MemoryConfig memory) { this.memory = memory; }

    public static class LlmConfig {
        private String provider;
        private String model;
        private int maxTokens;
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class ToolsConfig {
        private List<String> allowed;
        private int shellTimeout;
        public List<String> getAllowed() { return allowed; }
        public void setAllowed(List<String> allowed) { this.allowed = allowed; }
        public int getShellTimeout() { return shellTimeout; }
        public void setShellTimeout(int shellTimeout) { this.shellTimeout = shellTimeout; }
    }

    public static class GuardrailsConfig {
        private boolean hitl;
        private List<String> commandDenylist;
        private boolean networkBlocked;
        public boolean isHitl() { return hitl; }
        public void setHitl(boolean hitl) { this.hitl = hitl; }
        public List<String> getCommandDenylist() { return commandDenylist; }
        public void setCommandDenylist(List<String> commandDenylist) { this.commandDenylist = commandDenylist; }
        public boolean isNetworkBlocked() { return networkBlocked; }
        public void setNetworkBlocked(boolean networkBlocked) { this.networkBlocked = networkBlocked; }
    }

    public static class FeedbackConfig {
        private int maxRounds;
        public int getMaxRounds() { return maxRounds; }
        public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
    }

    public static class LoopConfig {
        private int maxRounds;
        public int getMaxRounds() { return maxRounds; }
        public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
    }

    public static class MemoryConfig {
        private String storePath;
        private int searchTopK;
        public String getStorePath() { return storePath; }
        public void setStorePath(String storePath) { this.storePath = storePath; }
        public int getSearchTopK() { return searchTopK; }
        public void setSearchTopK(int searchTopK) { this.searchTopK = searchTopK; }
    }
}
```

- [ ] **Step 3: 实现 ConfigLoader**

```java
package ai4se.harness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigLoader {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static HarnessConfig load(Path yamlPath) throws IOException {
        return mapper.readValue(yamlPath.toFile(), HarnessConfig.class);
    }
}
```

Run: `mvn test -pl . -Dtest=ConfigLoaderTest`
Expected: 1 PASS

- [ ] **Step 4: 写 CredentialManager 的测试**

```java
package ai4se.harness.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CredentialManagerTest {
    @Test
    void shouldStoreAndRetrieveKey() {
        CredentialManager cm = new CredentialManager();
        cm.storeKey("sk-ant-test-key");
        assertThat(cm.getKey()).isPresent();
        assertThat(cm.getKey().get()).isEqualTo("sk-ant-test-key");
    }

    @Test
    void shouldClearKey() {
        CredentialManager cm = new CredentialManager();
        cm.storeKey("sk-ant-test-key");
        cm.clearKey();
        assertThat(cm.getKey()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoKey() {
        CredentialManager cm = new CredentialManager();
        assertThat(cm.getKey()).isEmpty();
    }
}
```

Note: This uses an in-memory implementation for testing. The real implementation uses Windows Credential Manager.

Run: `mvn test -pl . -Dtest=CredentialManagerTest`
Expected: 3 FAIL

- [ ] **Step 5: 实现 CredentialManager**

```java
package ai4se.harness.config;

import java.util.Optional;

public class CredentialManager {
    private String cachedKey;

    public void storeKey(String key) {
        this.cachedKey = key;
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(cachedKey);
    }

    public void clearKey() {
        this.cachedKey = null;
    }
}
```

Run: `mvn test -pl . -Dtest=CredentialManagerTest`
Expected: 3 PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ai4se/harness/config/ src/test/java/ai4se/harness/config/
git commit -m "feat: add HarnessConfig, ConfigLoader, and CredentialManager"
```

---

## Task 13: 记忆系统 — FileMemoryStore 和 MemoryRetriever

**Files:**
- Create: `src/main/java/ai4se/harness/memory/MemoryStore.java`
- Create: `src/main/java/ai4se/harness/memory/FileMemoryStore.java`
- Create: `src/main/java/ai4se/harness/memory/MemoryRetriever.java`
- Create: `src/test/java/ai4se/harness/memory/FileMemoryStoreTest.java`
- Create: `src/test/java/ai4se/harness/memory/MemoryRetrieverTest.java`

**Interfaces:**
- Produces: `MemoryStore.save(String key, String content)`, `MemoryStore.load(String key) → Optional<String>`
- Produces: `FileMemoryStore(Path storePath)` — 基于文件的实现
- Produces: `MemoryRetriever.search(String query, int topK) → List<String>` — 关键词检索

- [ ] **Step 1: 写 MemoryStore 接口和 FileMemoryStore 的失败测试**

```java
package ai4se.harness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class FileMemoryStoreTest {
    @Test
    void shouldSaveAndLoad(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("session_001", "Fixed bug in UserService");
        assertThat(store.load("session_001")).isPresent();
        assertThat(store.load("session_001").get()).isEqualTo("Fixed bug in UserService");
    }

    @Test
    void shouldReturnEmptyForMissingKey(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        assertThat(store.load("nonexistent")).isEmpty();
    }

    @Test
    void shouldOverwriteExistingKey(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("key", "v1");
        store.save("key", "v2");
        assertThat(store.load("key").get()).isEqualTo("v2");
    }
}
```

Run: `mvn test -pl . -Dtest=FileMemoryStoreTest`
Expected: 3 FAIL

- [ ] **Step 2: 实现 MemoryStore 和 FileMemoryStore**

```java
package ai4se.harness.memory;

import java.util.Optional;

public interface MemoryStore {
    void save(String key, String content);
    Optional<String> load(String key);
}
```

```java
package ai4se.harness.memory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

public class FileMemoryStore implements MemoryStore {
    private final Path storePath;

    public FileMemoryStore(Path storePath) {
        this.storePath = storePath.toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, String content) {
        try {
            Files.createDirectories(storePath);
            Files.writeString(storePath.resolve(key + ".md"), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save memory: " + key, e);
        }
    }

    @Override
    public Optional<String> load(String key) {
        Path file = storePath.resolve(key + ".md");
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
```

Run: `mvn test -pl . -Dtest=FileMemoryStoreTest`
Expected: 3 PASS

- [ ] **Step 3: 写 MemoryRetriever 的失败测试**

```java
package ai4se.harness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class MemoryRetrieverTest {
    @Test
    void shouldRetrieveByKeyword(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("decisions_1", "Chose Jackson for YAML parsing because of performance");
        store.save("decisions_2", "Used Mockito for testing because of JUnit integration");
        MemoryRetriever retriever = new MemoryRetriever(store);

        var results = retriever.search("YAML", 3);
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains("Jackson");
    }

    @Test
    void shouldReturnEmptyForNoMatch(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        store.save("decisions_1", "Used Java 17");
        MemoryRetriever retriever = new MemoryRetriever(store);

        var results = retriever.search("python", 3);
        assertThat(results).isEmpty();
    }
}
```

Run: `mvn test -pl . -Dtest=MemoryRetrieverTest`
Expected: 2 FAIL

- [ ] **Step 4: 实现 MemoryRetriever**

```java
package ai4se.harness.memory;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryRetriever {
    private final MemoryStore store;

    public MemoryRetriever(MemoryStore store) {
        this.store = store;
    }

    public List<String> search(String query, int topK) {
        List<String> results = new ArrayList<>();
        String queryLower = query.toLowerCase();
        String[] keywords = queryLower.split("\\s+");

        for (String key : List.of("decisions_1", "decisions_2", "session_latest")) {
            store.load(key).ifPresent(content -> {
                String contentLower = content.toLowerCase();
                for (String kw : keywords) {
                    if (contentLower.contains(kw)) {
                        results.add(content);
                        break;
                    }
                }
            });
        }
        return results.stream().limit(topK).collect(Collectors.toList());
    }
}
```

Run: `mvn test -pl . -Dtest=MemoryRetrieverTest`
Expected: 2 PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ai4se/harness/memory/ src/test/java/ai4se/harness/memory/
git commit -m "feat: add MemoryStore, FileMemoryStore, and MemoryRetriever"
```

---

## Task 14: 反馈闭环 — 模型与 FailureClassifier（重点维度核心）

**Files:**
- Create: `src/main/java/ai4se/harness/feedback/Feedback.java`
- Create: `src/main/java/ai4se/harness/feedback/FailureType.java`
- Create: `src/main/java/ai4se/harness/feedback/Severity.java`
- Create: `src/main/java/ai4se/harness/feedback/FailureClassifier.java`
- Create: `src/test/java/ai4se/harness/feedback/FailureClassifierTest.java`

**Interfaces:**
- Produces: `Feedback` — `Feedback(boolean success, FailureType type, Severity severity, String suggestion)`
- Produces: `FailureType` — `COMPILE_ERROR`, `RUNTIME_ERROR`, `TEST_FAILURE`, `COMMAND_REJECTED`, `FILE_NOT_FOUND`, `PERMISSION_DENIED`, `TIMEOUT`, `UNKNOWN`
- Produces: `Severity` — `FATAL`, `ERROR`, `WARNING`, `INFO`
- Produces: `FailureClassifier.classify(String output, int exitCode, String actionName) → FailureType`

- [ ] **Step 1: 写 FailureClassifier 的失败测试**

```java
package ai4se.harness.feedback;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FailureClassifierTest {
    private final FailureClassifier classifier = new FailureClassifier();

    @Test
    void shouldClassifyCompileError() {
        FailureType type = classifier.classify(
            "Main.java:5: error: ';' expected", 1, "shell");
        assertThat(type).isEqualTo(FailureType.COMPILE_ERROR);
    }

    @Test
    void shouldClassifyTestFailure() {
        FailureType type = classifier.classify(
            "Tests run: 3, Failures: 1\nExpected: 42 but was: 0", 1, "shell");
        assertThat(type).isEqualTo(FailureType.TEST_FAILURE);
    }

    @Test
    void shouldClassifyRuntimeError() {
        FailureType type = classifier.classify(
            "Exception in thread \"main\" java.lang.NullPointerException", 1, "shell");
        assertThat(type).isEqualTo(FailureType.RUNTIME_ERROR);
    }

    @Test
    void shouldClassifyTimeout() {
        FailureType type = classifier.classify(
            "Command timed out after 30s", 143, "shell");
        assertThat(type).isEqualTo(FailureType.TIMEOUT);
    }

    @Test
    void shouldClassifyCommandRejected() {
        FailureType type = classifier.classify(
            "Dangerous command detected: rm -rf", 1, "shell");
        assertThat(type).isEqualTo(FailureType.COMMAND_REJECTED);
    }

    @Test
    void shouldClassifyFileNotFound() {
        FailureType type = classifier.classify(
            "File not found: src/Main.java", 1, "file");
        assertThat(type).isEqualTo(FailureType.FILE_NOT_FOUND);
    }

    @Test
    void shouldClassifyUnknown() {
        FailureType type = classifier.classify("Something went wrong", 1, "shell");
        assertThat(type).isEqualTo(FailureType.UNKNOWN);
    }
}
```

Run: `mvn test -pl . -Dtest=FailureClassifierTest`
Expected: 7 FAIL

- [ ] **Step 2: 实现 FailureType, Severity, Feedback**

```java
package ai4se.harness.feedback;

public enum FailureType {
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TEST_FAILURE,
    COMMAND_REJECTED,
    FILE_NOT_FOUND,
    PERMISSION_DENIED,
    TIMEOUT,
    UNKNOWN
}
```

```java
package ai4se.harness.feedback;

public enum Severity {
    FATAL,
    ERROR,
    WARNING,
    INFO
}
```

```java
package ai4se.harness.feedback;

public class Feedback {
    private final boolean success;
    private final FailureType type;
    private final Severity severity;
    private final String suggestion;

    public Feedback(boolean success, FailureType type, Severity severity, String suggestion) {
        this.success = success;
        this.type = type;
        this.severity = severity;
        this.suggestion = suggestion;
    }

    public boolean isSuccess() { return success; }
    public FailureType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public String getSuggestion() { return suggestion; }
}
```

- [ ] **Step 3: 实现 FailureClassifier**

```java
package ai4se.harness.feedback;

public class FailureClassifier {
    public FailureType classify(String output, int exitCode, String actionName) {
        if (output == null) return FailureType.UNKNOWN;

        String lower = output.toLowerCase();

        if (lower.contains("error:") && (lower.contains(".java:") || lower.contains(".kt:"))) {
            return FailureType.COMPILE_ERROR;
        }
        if (lower.contains("tests run:") && lower.contains("failures:")) {
            return FailureType.TEST_FAILURE;
        }
        if (lower.contains("exception in thread") || lower.contains("stacktrace")) {
            return FailureType.RUNTIME_ERROR;
        }
        if (lower.contains("timed out") || lower.contains("timeout")) {
            return FailureType.TIMEOUT;
        }
        if (lower.contains("dangerous command") || lower.contains("access denied") || lower.contains("blocked")) {
            return FailureType.COMMAND_REJECTED;
        }
        if (lower.contains("file not found") || lower.contains("no such file")) {
            return FailureType.FILE_NOT_FOUND;
        }
        if (lower.contains("permission denied")) {
            return FailureType.PERMISSION_DENIED;
        }

        return FailureType.UNKNOWN;
    }
}
```

Run: `mvn test -pl . -Dtest=FailureClassifierTest`
Expected: 7 PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ai4se/harness/feedback/ src/test/java/ai4se/harness/feedback/
git commit -m "feat: add Feedback, FailureType, Severity, and FailureClassifier"
```

---

## Task 15: 反馈闭环 — 其余组件

**Files:**
- Create: `src/main/java/ai4se/harness/feedback/FeedbackCollector.java`
- Create: `src/main/java/ai4se/harness/feedback/SeverityJudge.java`
- Create: `src/main/java/ai4se/harness/feedback/CorrectionSuggester.java`
- Create: `src/main/java/ai4se/harness/feedback/FeedbackPipeline.java`
- Create: `src/test/java/ai4se/harness/feedback/FeedbackCollectorTest.java`
- Create: `src/test/java/ai4se/harness/feedback/SeverityJudgeTest.java`
- Create: `src/test/java/ai4se/harness/feedback/CorrectionSuggesterTest.java`
- Create: `src/test/java/ai4se/harness/feedback/FeedbackPipelineTest.java`

**Interfaces:**
- Consumes: `ToolResult` (from Task 4), `FailureClassifier` (from Task 14)
- Produces: `FeedbackCollector.collect(ToolResult result, String actionName) → Feedback`
- Produces: `SeverityJudge.judge(FailureType type, boolean isRecoverable) → Severity`
- Produces: `CorrectionSuggester.suggest(FailureType type, String errorDetail) → String`
- Produces: `FeedbackPipeline.process(ToolResult result, String actionName, int round) → Feedback`

- [ ] **Step 1: 写 FeedbackCollector 的失败测试**

```java
package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FeedbackCollectorTest {
    private final FeedbackCollector collector = new FeedbackCollector();

    @Test
    void shouldCollectSuccessFeedback() {
        ToolResult result = new ToolResult(true, "Compilation successful");
        Feedback feedback = collector.collect(result, "shell");
        assertThat(feedback.isSuccess()).isTrue();
    }

    @Test
    void shouldCollectFailureFeedback() {
        ToolResult result = new ToolResult(false, "Main.java:5: error: ';' expected", 1);
        Feedback feedback = collector.collect(result, "shell");
        assertThat(feedback.isSuccess()).isFalse();
        assertThat(feedback.getType()).isEqualTo(FailureType.COMPILE_ERROR);
    }
}
```

Run: `mvn test -pl . -Dtest=FeedbackCollectorTest`
Expected: 2 FAIL

- [ ] **Step 2: 实现 FeedbackCollector**

```java
package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;

public class FeedbackCollector {
    private final FailureClassifier classifier = new FailureClassifier();

    public Feedback collect(ToolResult result, String actionName) {
        if (result.isSuccess()) {
            return new Feedback(true, FailureType.UNKNOWN, Severity.INFO, null);
        }
        FailureType type = classifier.classify(result.getOutput(), result.getExitCode(), actionName);
        return new Feedback(false, type, null, null);
    }
}
```

Run: `mvn test -pl . -Dtest=FeedbackCollectorTest`
Expected: 2 PASS

- [ ] **Step 3: 写 SeverityJudge 和 CorrectionSuggester 的测试**

```java
package ai4se.harness.feedback;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SeverityJudgeTest {
    private final SeverityJudge judge = new SeverityJudge();

    @Test
    void shouldJudgeCompileErrorAsError() {
        assertThat(judge.judge(FailureType.COMPILE_ERROR)).isEqualTo(Severity.ERROR);
    }

    @Test
    void shouldJudgeCommandRejectedAsFatal() {
        assertThat(judge.judge(FailureType.COMMAND_REJECTED)).isEqualTo(Severity.FATAL);
    }

    @Test
    void shouldJudgeTimeoutAsWarning() {
        assertThat(judge.judge(FailureType.TIMEOUT)).isEqualTo(Severity.WARNING);
    }
}

class CorrectionSuggesterTest {
    private final CorrectionSuggester suggester = new CorrectionSuggester();

    @Test
    void shouldSuggestCompileFix() {
        String suggestion = suggester.suggest(FailureType.COMPILE_ERROR, "Main.java:5: error: ';' expected");
        assertThat(suggestion).contains("编译错误");
        assertThat(suggestion).contains("Main.java:5");
    }

    @Test
    void shouldSuggestTestFix() {
        String suggestion = suggester.suggest(FailureType.TEST_FAILURE, "Expected: 42 but was: 0");
        assertThat(suggestion).contains("测试失败");
    }

    @Test
    void shouldSuggestAlternativeForRejected() {
        String suggestion = suggester.suggest(FailureType.COMMAND_REJECTED, "rm -rf blocked");
        assertThat(suggestion).contains("被护栏拦截");
        assertThat(suggestion).contains("换一种安全");
    }
}
```

Run: `mvn test -pl . -Dtest=SeverityJudgeTest,CorrectionSuggesterTest`
Expected: 6 FAIL

- [ ] **Step 4: 实现 SeverityJudge 和 CorrectionSuggester**

```java
package ai4se.harness.feedback;

import java.util.Map;

public class SeverityJudge {
    private static final Map<FailureType, Severity> MAPPING = Map.of(
        FailureType.COMMAND_REJECTED, Severity.FATAL,
        FailureType.PERMISSION_DENIED, Severity.FATAL,
        FailureType.COMPILE_ERROR, Severity.ERROR,
        FailureType.RUNTIME_ERROR, Severity.ERROR,
        FailureType.TEST_FAILURE, Severity.ERROR,
        FailureType.TIMEOUT, Severity.WARNING,
        FailureType.FILE_NOT_FOUND, Severity.WARNING,
        FailureType.UNKNOWN, Severity.ERROR
    );

    public Severity judge(FailureType type) {
        return MAPPING.getOrDefault(type, Severity.ERROR);
    }
}
```

```java
package ai4se.harness.feedback;

import java.util.Map;

public class CorrectionSuggester {
    private static final Map<FailureType, String> TEMPLATES = Map.of(
        FailureType.COMPILE_ERROR, "编译错误，请根据以下错误信息修正代码：%s",
        FailureType.TEST_FAILURE, "测试失败，请检查测试输出并修正实现：%s",
        FailureType.RUNTIME_ERROR, "运行时错误，请检查异常信息并修正：%s",
        FailureType.COMMAND_REJECTED, "操作被护栏拦截，请换一种安全的方案：%s",
        FailureType.FILE_NOT_FOUND, "文件未找到，请检查文件路径：%s",
        FailureType.PERMISSION_DENIED, "权限不足，请检查权限：%s",
        FailureType.TIMEOUT, "命令超时，请尝试拆分任务或优化执行方式：%s",
        FailureType.UNKNOWN, "执行失败，请检查错误详情：%s"
    );

    public String suggest(FailureType type, String errorDetail) {
        String template = TEMPLATES.getOrDefault(type, "执行失败：%s");
        return String.format(template, errorDetail != null ? errorDetail : "未知错误");
    }
}
```

Run: `mvn test -pl . -Dtest=SeverityJudgeTest,CorrectionSuggesterTest`
Expected: 6 PASS

- [ ] **Step 5: 写 FeedbackPipeline 的失败测试**

```java
package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FeedbackPipelineTest {
    private final FeedbackPipeline pipeline = new FeedbackPipeline();

    @Test
    void shouldProcessSuccessfulResult() {
        ToolResult result = new ToolResult(true, "OK");
        Feedback feedback = pipeline.process(result, "shell", 1);
        assertThat(feedback.isSuccess()).isTrue();
    }

    @Test
    void shouldProcessFailedResultWithFullFeedback() {
        ToolResult result = new ToolResult(false, "Main.java:5: error: ';' expected", 1);
        Feedback feedback = pipeline.process(result, "shell", 1);
        assertThat(feedback.isSuccess()).isFalse();
        assertThat(feedback.getType()).isEqualTo(FailureType.COMPILE_ERROR);
        assertThat(feedback.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(feedback.getSuggestion()).contains("编译错误");
        assertThat(feedback.getSuggestion()).contains("Main.java:5");
    }
}
```

Run: `mvn test -pl . -Dtest=FeedbackPipelineTest`
Expected: 2 FAIL

- [ ] **Step 6: 实现 FeedbackPipeline**

```java
package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;

public class FeedbackPipeline {
    private final FeedbackCollector collector = new FeedbackCollector();
    private final SeverityJudge judge = new SeverityJudge();
    private final CorrectionSuggester suggester = new CorrectionSuggester();

    public Feedback process(ToolResult result, String actionName, int round) {
        Feedback feedback = collector.collect(result, actionName);
        if (feedback.isSuccess()) return feedback;

        Severity severity = judge.judge(feedback.getType());
        String suggestion = suggester.suggest(feedback.getType(), result.getOutput());

        return new Feedback(false, feedback.getType(), severity, suggestion);
    }
}
```

Run: `mvn test -pl . -Dtest=FeedbackPipelineTest`
Expected: 2 PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/ai4se/harness/feedback/ src/test/java/ai4se/harness/feedback/
git commit -m "feat: add FeedbackCollector, SeverityJudge, CorrectionSuggester, and FeedbackPipeline"
```

---

## Task 16: Agent 核心 — ActionParser, ContextAssembler, StopCondition

**Files:**
- Create: `src/main/java/ai4se/harness/core/Action.java`
- Create: `src/main/java/ai4se/harness/core/ActionParser.java`
- Create: `src/main/java/ai4se/harness/core/ContextAssembler.java`
- Create: `src/main/java/ai4se/harness/core/StopCondition.java`
- Create: `src/test/java/ai4se/harness/core/ActionParserTest.java`
- Create: `src/test/java/ai4se/harness/core/ContextAssemblerTest.java`
- Create: `src/test/java/ai4se/harness/core/StopConditionTest.java`

**Interfaces:**
- Consumes: `LlmResponse` (from Task 2), `Tool` (from Task 4), `MemoryRetriever` (from Task 13), `Conversation` (from Task 2)
- Produces: `Action` — `Action(String toolName, Map<String, Object> params)`
- Produces: `ActionParser.parse(LlmResponse response) → Action`
- Produces: `ContextAssembler.assemble(String task, List<Tool> tools, MemoryRetriever memory, Conversation history) → List<Message>`
- Produces: `StopCondition.shouldStop(LlmResponse response, int round, int maxRounds) → boolean`

- [ ] **Step 1: 写 ActionParser 的失败测试**

```java
package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ActionParserTest {
    private final ActionParser parser = new ActionParser();

    @Test
    void shouldParseActionResponse() {
        LlmResponse resp = new LlmResponse(null, "shell", Map.of("command", "mvn test"), "tool_use");
        Action action = parser.parse(resp);
        assertThat(action.getToolName()).isEqualTo("shell");
        assertThat(action.getParams()).containsEntry("command", "mvn test");
    }

    @Test
    void shouldReturnNullForTextResponse() {
        LlmResponse resp = new LlmResponse("Task completed.", null, null, "end_turn");
        Action action = parser.parse(resp);
        assertThat(action).isNull();
    }
}
```

Run: `mvn test -pl . -Dtest=ActionParserTest`
Expected: 2 FAIL

- [ ] **Step 2: 实现 Action 和 ActionParser**

```java
package ai4se.harness.core;

import java.util.Map;

public class Action {
    private final String toolName;
    private final Map<String, Object> params;

    public Action(String toolName, Map<String, Object> params) {
        this.toolName = toolName;
        this.params = params;
    }

    public String getToolName() { return toolName; }
    public Map<String, Object> getParams() { return params; }
}
```

```java
package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;

public class ActionParser {
    public Action parse(LlmResponse response) {
        if (response.hasAction()) {
            return new Action(response.getActionName(), response.getActionParams());
        }
        return null;
    }
}
```

Run: `mvn test -pl . -Dtest=ActionParserTest`
Expected: 2 PASS

- [ ] **Step 3: 写 ContextAssembler 的失败测试**

```java
package ai4se.harness.core;

import ai4se.harness.llm.Conversation;
import ai4se.harness.llm.Message;
import ai4se.harness.memory.FileMemoryStore;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ContextAssemblerTest {
    private final ContextAssembler assembler = new ContextAssembler();

    @Test
    void shouldAssembleContextWithSystemPrompt(@TempDir Path tempDir) {
        FileMemoryStore store = new FileMemoryStore(tempDir);
        MemoryRetriever retriever = new MemoryRetriever(store);
        List<Tool> tools = List.of(new TestTool());

        List<Message> messages = assembler.assemble("write a test", tools, retriever, new Conversation());

        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getRole()).isEqualTo("system");
        assertThat(messages.get(0).getContent()).contains("coding agent");
        assertThat(messages.get(0).getContent()).contains("test");
        assertThat(messages.get(messages.size() - 1).getRole()).isEqualTo("user");
        assertThat(messages.get(messages.size() - 1).getContent()).isEqualTo("write a test");
    }

    static class TestTool implements Tool {
        public String name() { return "test"; }
        public String description() { return "test tool"; }
        public ai4se.harness.tools.ToolResult execute(Map<String, Object> p) { return null; }
    }
}
```

Run: `mvn test -pl . -Dtest=ContextAssemblerTest`
Expected: 1 FAIL

- [ ] **Step 4: 实现 ContextAssembler**

```java
package ai4se.harness.core;

import ai4se.harness.llm.Conversation;
import ai4se.harness.llm.Message;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.Tool;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContextAssembler {
    public List<Message> assemble(String task, List<Tool> tools, MemoryRetriever memory, Conversation history) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt(tools, memory);
        messages.add(new Message("system", systemPrompt));

        messages.addAll(history.getMessages());
        messages.add(new Message("user", task));

        return messages;
    }

    private String buildSystemPrompt(List<Tool> tools, MemoryRetriever memory) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a coding agent. You can use tools to complete tasks.\n\n");
        sb.append("Available tools:\n");
        for (Tool tool : tools) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
        }
        sb.append("\nFormat your response as a tool call or text.\n");

        List<String> relevantMemories = memory.search("convention project", 3);
        if (!relevantMemories.isEmpty()) {
            sb.append("\nRelevant memories:\n");
            for (String mem : relevantMemories) {
                sb.append("- ").append(mem).append("\n");
            }
        }

        return sb.toString();
    }
}
```

Run: `mvn test -pl . -Dtest=ContextAssemblerTest`
Expected: 1 PASS

- [ ] **Step 5: 写 StopCondition 的失败测试**

```java
package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class StopConditionTest {
    private final StopCondition stopCondition = new StopCondition();

    @Test
    void shouldStopOnEndTurn() {
        LlmResponse resp = new LlmResponse("done", null, null, "end_turn");
        assertThat(stopCondition.shouldStop(resp, 1, 10)).isTrue();
    }

    @Test
    void shouldStopOnMaxRounds() {
        LlmResponse resp = new LlmResponse(null, "shell", java.util.Map.of(), "tool_use");
        assertThat(stopCondition.shouldStop(resp, 10, 10)).isTrue();
    }

    @Test
    void shouldContinueOnToolUse() {
        LlmResponse resp = new LlmResponse(null, "shell", java.util.Map.of(), "tool_use");
        assertThat(stopCondition.shouldStop(resp, 5, 10)).isFalse();
    }
}
```

Run: `mvn test -pl . -Dtest=StopConditionTest`
Expected: 3 FAIL

- [ ] **Step 6: 实现 StopCondition**

```java
package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;

public class StopCondition {
    public boolean shouldStop(LlmResponse response, int round, int maxRounds) {
        if (round >= maxRounds) return true;
        if ("end_turn".equals(response.getStopReason())) return true;
        if ("stop_sequence".equals(response.getStopReason())) return true;
        return false;
    }
}
```

Run: `mvn test -pl . -Dtest=StopConditionTest`
Expected: 3 PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/ai4se/harness/core/ src/test/java/ai4se/harness/core/
git commit -m "feat: add Action, ActionParser, ContextAssembler, and StopCondition"
```

---

## Task 17: AgentLoop — 主循环（核心组装）

**Files:**
- Create: `src/main/java/ai4se/harness/core/AgentLoop.java`
- Create: `src/test/java/ai4se/harness/core/AgentLoopTest.java`

**Interfaces:**
- Consumes: All previous modules (LlmProvider, ToolRegistry, GuardrailChain, FeedbackPipeline, MemoryRetriever, ContextAssembler, ActionParser, StopCondition, HarnessConfig)
- Produces: `AgentLoop.run(String task) → String` — 完整的主循环

- [ ] **Step 1: 写 AgentLoop 的失败测试（mock LLM 驱动）**

```java
package ai4se.harness.core;

import ai4se.harness.config.HarnessConfig;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.GuardrailChain;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AgentLoopTest {
    @Test
    void shouldCompleteSimpleTask(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "echo hello"), "tool_use"),
            new LlmResponse("Task completed", null, null, "end_turn")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10));

        AgentLoop loop = new AgentLoop(
            mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            createDefaultConfig()
        );

        String result = loop.run("say hello");
        assertThat(result).contains("Task completed");
    }

    private HarnessConfig createDefaultConfig() {
        HarnessConfig config = new HarnessConfig();
        HarnessConfig.LoopConfig loopConfig = new HarnessConfig.LoopConfig();
        loopConfig.setMaxRounds(10);
        config.setLoop(loopConfig);
        HarnessConfig.FeedbackConfig feedbackConfig = new HarnessConfig.FeedbackConfig();
        feedbackConfig.setMaxRounds(3);
        config.setFeedback(feedbackConfig);
        return config;
    }
}
```

Run: `mvn test -pl . -Dtest=AgentLoopTest`
Expected: 1 FAIL

- [ ] **Step 2: 实现 AgentLoop**

```java
package ai4se.harness.core;

import ai4se.harness.config.HarnessConfig;
import ai4se.harness.feedback.Feedback;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.GuardResult;
import ai4se.harness.guardrails.GuardrailChain;
import ai4se.harness.llm.*;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.*;
import java.util.List;
import java.util.Optional;

public class AgentLoop {
    private final LlmProvider llm;
    private final ToolRegistry tools;
    private final GuardrailChain guardrails;
    private final FeedbackPipeline feedback;
    private final ContextAssembler assembler;
    private final ActionParser parser;
    private final StopCondition stopCondition;
    private final MemoryRetriever memory;
    private final HarnessConfig config;

    public AgentLoop(LlmProvider llm, ToolRegistry tools, GuardrailChain guardrails,
                     FeedbackPipeline feedback, ContextAssembler assembler,
                     ActionParser parser, StopCondition stopCondition,
                     MemoryRetriever memory, HarnessConfig config) {
        this.llm = llm;
        this.tools = tools;
        this.guardrails = guardrails;
        this.feedback = feedback;
        this.assembler = assembler;
        this.parser = parser;
        this.stopCondition = stopCondition;
        this.memory = memory;
        this.config = config;
    }

    public String run(String task) {
        Conversation history = new Conversation();
        int round = 0;
        int correctionRound = 0;
        StringBuilder resultSummary = new StringBuilder();

        while (round < config.getLoop().getMaxRounds()) {
            round++;
            System.out.println("[Round " + round + "]");

            List<Message> messages = assembler.assemble(task, tools.getAll(), memory, history);
            LlmResponse response = llm.complete(messages, tools.getAll());

            if (stopCondition.shouldStop(response, round, config.getLoop().getMaxRounds())) {
                return response.getText() != null ? response.getText() : "Completed.";
            }

            Action action = parser.parse(response);
            if (action == null) {
                history.add(new Message("assistant", response.getText()));
                resultSummary.append(response.getText());
                correctionRound = 0;
                continue;
            }

            GuardResult guard = guardrails.check(action.getToolName(), action.getParams());
            if (guard.isBlock()) {
                Feedback fb = new Feedback(false, ai4se.harness.feedback.FailureType.COMMAND_REJECTED,
                    ai4se.harness.feedback.Severity.FATAL, guard.getReason());
                history.add(new Message("user", "[FEEDBACK] " + fb.getSuggestion()));
                correctionRound++;
                if (correctionRound > config.getFeedback().getMaxRounds()) {
                    return "Failed after " + correctionRound + " correction rounds.";
                }
                continue;
            }

            Optional<Tool> tool = tools.get(action.getToolName());
            if (tool.isEmpty()) {
                history.add(new Message("user", "[FEEDBACK] Unknown tool: " + action.getToolName()));
                continue;
            }

            ToolResult toolResult = tool.get().execute(action.getParams());
            Feedback fb = feedback.process(toolResult, action.getToolName(), round);

            if (!fb.isSuccess()) {
                history.add(new Message("user", "[FEEDBACK] 失败类型: " + fb.getType() +
                    "\n错误详情: " + toolResult.getOutput() +
                    "\n修正建议: " + fb.getSuggestion()));
                correctionRound++;
                if (correctionRound > config.getFeedback().getMaxRounds()) {
                    resultSummary.append("Failed after ").append(correctionRound).append(" correction rounds.");
                    return resultSummary.toString();
                }
            } else {
                history.add(new Message("user", "[RESULT] " + toolResult.getOutput()));
                resultSummary.append(toolResult.getOutput());
                correctionRound = 0;
            }
        }

        return resultSummary.toString();
    }
}
```

Run: `mvn test -pl . -Dtest=AgentLoopTest`
Expected: 1 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/core/AgentLoop.java src/test/java/ai4se/harness/core/AgentLoopTest.java
git commit -m "feat: add AgentLoop - main agent loop with full orchestration"
```

---

## Task 18: ClaudeProvider — 真实 LLM 实现

**Files:**
- Create: `src/main/java/ai4se/harness/llm/ClaudeProvider.java`
- Create: `src/test/java/ai4se/harness/llm/ClaudeProviderTest.java`

**Interfaces:**
- Consumes: `LlmProvider` (from Task 2), OkHttp, Jackson
- Produces: `ClaudeProvider(String apiKey, String model)` — 调用 Anthropic Messages API

- [ ] **Step 1: 写 ClaudeProvider 的失败测试（集成测试，可选跳过）**

```java
package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ClaudeProviderTest {
    @Test
    void shouldConstructWithApiKey() {
        ClaudeProvider provider = new ClaudeProvider("sk-ant-test", "claude-sonnet-4-20250514");
        assertThat(provider).isNotNull();
    }
}
```

Run: `mvn test -pl . -Dtest=ClaudeProviderTest`
Expected: 1 FAIL

- [ ] **Step 2: 实现 ClaudeProvider**

```java
package ai4se.harness.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import ai4se.harness.tools.Tool;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClaudeProvider implements LlmProvider {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String apiKey;
    private final String model;
    private final OkHttpClient client;

    public ClaudeProvider(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public LlmResponse complete(List<Message> messages, List<Tool> tools) {
        try {
            Map<String, Object> body = buildRequestBody(messages, tools);
            String json = mapper.writeValueAsString(body);

            Request request = new Request.Builder()
                .url(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .post(RequestBody.create(json, JSON))
                .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    return new LlmResponse("API error: " + response.code() + " " + responseBody, null, null, "end_turn");
                }
                return parseResponse(responseBody);
            }
        } catch (IOException e) {
            return new LlmResponse("API call failed: " + e.getMessage(), null, null, "end_turn");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestBody(List<Message> messages, List<Tool> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);

        List<Map<String, Object>> msgs = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            msgs.add(m);
        }
        body.put("messages", msgs);

        if (!tools.isEmpty()) {
            List<Map<String, Object>> toolDefs = new ArrayList<>();
            for (Tool tool : tools) {
                Map<String, Object> td = new LinkedHashMap<>();
                td.put("name", tool.name());
                td.put("description", tool.description());
                td.put("input_schema", Map.of("type", "object", "properties", Map.of()));
                toolDefs.add(td);
            }
            body.put("tools", toolDefs);
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String responseBody) throws IOException {
        Map<String, Object> resp = mapper.readValue(responseBody, Map.class);
        String stopReason = (String) resp.getOrDefault("stop_reason", "end_turn");

        List<Map<String, Object>> content = (List<Map<String, Object>>) resp.get("content");
        if (content == null) {
            return new LlmResponse("No content in response", null, null, stopReason);
        }

        for (Map<String, Object> block : content) {
            String type = (String) block.get("type");
            if ("tool_use".equals(type)) {
                String name = (String) block.get("name");
                Map<String, Object> input = (Map<String, Object>) block.get("input");
                return new LlmResponse(null, name, input, stopReason);
            }
            if ("text".equals(type)) {
                return new LlmResponse((String) block.get("text"), null, null, stopReason);
            }
        }

        return new LlmResponse("No parsable content", null, null, stopReason);
    }
}
```

Run: `mvn test -pl . -Dtest=ClaudeProviderTest`
Expected: 1 PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/llm/ClaudeProvider.java src/test/java/ai4se/harness/llm/ClaudeProviderTest.java
git commit -m "feat: add ClaudeProvider - Anthropic Messages API integration"
```

---

## Task 19: CLI 入口 — HarnessApp

**Files:**
- Create: `src/main/java/ai4se/harness/HarnessApp.java`

**Interfaces:**
- Consumes: All previous modules
- Produces: CLI 通过 Picocli 实现，支持 `run`, `config set-key`, `config show-key`, `config clear-key` 子命令

- [ ] **Step 1: 实现 HarnessApp**

```java
package ai4se.harness;

import ai4se.harness.config.*;
import ai4se.harness.core.*;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.*;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(name = "harness", subcommands = {HarnessApp.RunCommand.class, HarnessApp.ConfigCommand.class})
public class HarnessApp implements Callable<Integer> {
    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "run", description = "Run a coding agent task")
    static class RunCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Task description")
        private String task;

        @Override
        public Integer call() throws Exception {
            Path configPath = Path.of("harness.yaml");
            HarnessConfig config = ConfigLoader.load(configPath);

            CredentialManager cm = new CredentialManager();
            String apiKey = cm.getKey().orElse(System.getenv("ANTHROPIC_API_KEY"));
            if (apiKey == null) {
                System.err.println("No API key found. Run 'harness config set-key' first.");
                return 1;
            }

            LlmProvider llm = new ClaudeProvider(apiKey, config.getLlm().getModel());
            ToolRegistry registry = new ToolRegistry();
            Path projectRoot = Path.of(".").toAbsolutePath().normalize();
            registry.register(new FileTool(projectRoot));
            registry.register(new ShellTool(config.getTools().getShellTimeout()));
            registry.register(new GitTool(projectRoot));
            registry.register(new SearchTool(projectRoot));

            GuardrailChain guardrails = new GuardrailChain(List.of(
                new CommandGuardrail(config.getGuardrails().getCommandDenylist()),
                new FileGuardrail(projectRoot),
                new NetworkGuardrail()
            ));

            FileMemoryStore store = new FileMemoryStore(Path.of(config.getMemory().getStorePath()));
            MemoryRetriever retriever = new MemoryRetriever(store);

            AgentLoop loop = new AgentLoop(
                llm, registry, guardrails, new FeedbackPipeline(),
                new ContextAssembler(), new ActionParser(), new StopCondition(),
                retriever, config
            );

            String result = loop.run(task);
            System.out.println("\n=== Result ===");
            System.out.println(result);
            return 0;
        }
    }

    @Command(name = "config", description = "Manage configuration")
    static class ConfigCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new HarnessApp()).execute(args);
        System.exit(exitCode);
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ai4se/harness/HarnessApp.java
git commit -m "feat: add CLI entry point with Picocli (run and config commands)"
```

---

## Task 20: 机制演示（Demo）

**Files:**
- Create: `src/test/java/ai4se/harness/demo/DemoTest.java`

**Interfaces:**
- Consumes: All previous modules
- Produces: 三项确定性向演示，mock LLM 驱动，不依赖网络

- [ ] **Step 1: 写 DemoTest**

```java
package ai4se.harness.demo;

import ai4se.harness.core.*;
import ai4se.harness.feedback.*;
import ai4se.harness.guardrails.*;
import ai4se.harness.llm.*;
import ai4se.harness.memory.*;
import ai4se.harness.tools.*;
import ai4se.harness.config.HarnessConfig;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class DemoTest {

    @Test
    @DisplayName("Demo 1: Guardrail blocks dangerous command")
    void demo1_guardrailBlocksDangerousCommand() {
        CommandGuardrail guard = new CommandGuardrail(List.of("rm -rf", "sudo", "chmod 777"));
        GuardResult result = guard.check("shell", Map.of("command", "rm -rf /"));
        assertThat(result.isBlock()).isTrue();
        assertThat(result.getReason()).contains("rm -rf");
    }

    @Test
    @DisplayName("Demo 2: Feedback loop drives self-correction")
    void demo2_feedbackLoopDrivesSelfCorrection(@TempDir Path tempDir) {
        MockLlmProvider mock = new MockLlmProvider();
        mock.setSequence(List.of(
            new LlmResponse(null, "shell", Map.of("command", "javac Broken.java"), "tool_use"),
            new LlmResponse(null, "shell", Map.of("command", "echo fixed"), "tool_use"),
            new LlmResponse("Task completed successfully", null, null, "end_turn")
        ));

        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellTool(10) {
            private int callCount = 0;
            @Override
            public ToolResult execute(Map<String, Object> params) {
                callCount++;
                if (callCount == 1) {
                    return new ToolResult(false, "Broken.java:5: error: ';' expected", 1);
                }
                return new ToolResult(true, "Compilation successful", 0);
            }
        });

        AgentLoop loop = new AgentLoop(
            mock, registry, new GuardrailChain(List.of()), new FeedbackPipeline(),
            new ContextAssembler(), new ActionParser(), new StopCondition(),
            new MemoryRetriever(new FileMemoryStore(tempDir)),
            createConfig()
        );

        String result = loop.run("compile and fix code");
        assertThat(result).contains("successful");
    }

    @Test
    @DisplayName("Demo 3: FailureClassifier covers 3+ failure types")
    void demo3_failureClassifierCoversMultipleTypes() {
        FailureClassifier classifier = new FailureClassifier();

        assertThat(classifier.classify("Main.java:5: error: ';' expected", 1, "shell"))
            .isEqualTo(FailureType.COMPILE_ERROR);
        assertThat(classifier.classify("Tests run: 3, Failures: 1", 1, "shell"))
            .isEqualTo(FailureType.TEST_FAILURE);
        assertThat(classifier.classify("Exception in thread \"main\" NullPointerException", 1, "shell"))
            .isEqualTo(FailureType.RUNTIME_ERROR);
        assertThat(classifier.classify("Command timed out after 30s", 143, "shell"))
            .isEqualTo(FailureType.TIMEOUT);
        assertThat(classifier.classify("Dangerous command detected: rm -rf", 1, "shell"))
            .isEqualTo(FailureType.COMMAND_REJECTED);
    }

    private HarnessConfig createConfig() {
        HarnessConfig c = new HarnessConfig();
        HarnessConfig.LoopConfig lc = new HarnessConfig.LoopConfig();
        lc.setMaxRounds(10);
        c.setLoop(lc);
        HarnessConfig.FeedbackConfig fc = new HarnessConfig.FeedbackConfig();
        fc.setMaxRounds(3);
        c.setFeedback(fc);
        return c;
    }
}
```

Run: `mvn test -pl . -Dtest=DemoTest`
Expected: 3 PASS

- [ ] **Step 2: Commit**

```bash
git add src/test/java/ai4se/harness/demo/
git commit -m "feat: add mechanism demo (3 deterministic demonstrations with mock LLM)"
```

---

## Task 21: CI — GitHub Actions

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: 创建 ci.yml**

```yaml
name: CI

on:
  push:
    branches: [master, main]
  pull_request:
    branches: [master, main]

jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: mvn test
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions workflow for unit tests"
```

---

## Task 22: Docker 分发

**Files:**
- Create: `Dockerfile`

- [ ] **Step 1: 创建 Dockerfile**

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven
RUN mvn dependency:go-offline
COPY src ./src
COPY harness.yaml .
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/harness-1.0.0.jar harness.jar
COPY harness.yaml .
ENTRYPOINT ["java", "-jar", "harness.jar"]
```

- [ ] **Step 2: Commit**

```bash
git add Dockerfile
git commit -m "feat: add Dockerfile for container distribution"
```

---

## Task 23: README

**Files:**
- Create: `README.md`

- [ ] **Step 1: 创建 README.md**

```markdown
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
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README"
```

---

## 依赖关系

```
T1 (Scaffolding)
 ├── T2 (LLM models)
 │    └── T3 (MockLlmProvider)
 ├── T4 (Tool interface)
 │    ├── T5 (FileTool)
 │    ├── T6 (ShellTool)
 │    ├── T7 (GitTool)
 │    └── T8 (SearchTool)
 ├── T9 (Guardrail interface + CommandGuardrail)
 │    ├── T10 (FileGuardrail)
 │    ├── T11 (NetworkGuardrail)
 │    └── T12 (GuardrailChain)
 ├── T12 (Config)
 ├── T13 (Memory)
 ├── T14 (Feedback models + FailureClassifier)
 │    └── T15 (FeedbackCollector, SeverityJudge, CorrectionSuggester, FeedbackPipeline)
 ├── T16 (ActionParser, ContextAssembler, StopCondition)
 │    └── T17 (AgentLoop)
 │         └── T18 (ClaudeProvider)
 │              └── T19 (CLI)
 │                   ├── T20 (Demo)
 │                   ├── T21 (CI)
 │                   ├── T22 (Docker)
 │                   └── T23 (README)
```

**可并行执行**: T2, T4, T9, T12, T13 可同时进行；T5-T8 可并行；T10-T11 可并行；T14-T16 可部分并行；T21-T23 可并行。