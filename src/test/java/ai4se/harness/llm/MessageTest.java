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