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
        assertThat(feedback.getType()).isEqualTo(FailureType.COMPILATION_ERROR);
    }
}