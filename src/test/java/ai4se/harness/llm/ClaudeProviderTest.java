package ai4se.harness.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ClaudeProviderTest {
    @Test
    void shouldConstructWithApiKey() {
        ClaudeProvider provider = new ClaudeProvider("sk-ant-test", "claude-sonnet-4-20250514");
        assertThat(provider).isNotNull();
    }
}