package ai4se.harness.core;

import ai4se.harness.llm.Conversation;
import ai4se.harness.llm.Message;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.Tool;
import java.util.ArrayList;
import java.util.List;

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
        sb.append("You are a coding agent that completes tasks by calling tools. Respond with a tool call or text.\n\n");
        sb.append("Available tools:\n");
        sb.append("- file: params={action:\"read\"|\"write\"|\"glob\", path:\"relative/path\", content:\"text for write\"}\n");
        sb.append("- shell: params={command:\"shell command\"}\n");
        sb.append("- git: params={action:\"status\"|\"diff\"|\"log\"|\"branch\"}\n");
        sb.append("- search: params={action:\"grep\"|\"glob\", pattern:\"search pattern\"}\n");
        sb.append("\nWhen calling a tool, always provide ALL required params. After each tool result, call the next tool or respond with text when done.\n");

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