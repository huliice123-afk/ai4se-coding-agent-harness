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
        sb.append("You are a coding agent. Use tools to complete tasks.\n\n");
        sb.append("Available tools:\n");
        for (Tool tool : tools) {
            sb.append("- ").append(tool.getName())
              .append(": ").append(tool.getDescription())
              .append("\n  Parameters: ").append(tool.getParameters())
              .append("\n");
        }
        sb.append("\nWhen calling a tool, provide ALL required params as JSON.\n");
        sb.append("After each tool result, call the next tool or respond with text when done.\n");

        List<String> relevantMemories = memory.search("convention project", 3);
        if (!relevantMemories.isEmpty()) {
            sb.append("\nPrevious session memory:\n");
            for (String mem : relevantMemories) {
                sb.append("- ").append(mem).append("\n");
            }
        }

        return sb.toString();
    }
}
