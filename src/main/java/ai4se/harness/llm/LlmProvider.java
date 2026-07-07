package ai4se.harness.llm;

import java.util.List;

public interface LlmProvider {
    LlmResponse complete(List<Message> messages, List<ai4se.harness.tools.Tool> tools);
}