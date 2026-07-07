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