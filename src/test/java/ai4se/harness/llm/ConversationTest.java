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