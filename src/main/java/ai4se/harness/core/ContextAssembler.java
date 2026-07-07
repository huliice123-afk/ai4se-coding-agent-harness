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