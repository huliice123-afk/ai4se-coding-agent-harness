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