package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FeedbackPipelineTest {
    private final FeedbackPipeline pipeline = new FeedbackPipeline();

    @Test
    void shouldProcessSuccessfulResult() {
        ToolResult result = new ToolResult(true, "OK");
        Feedback feedback = pipeline.process(result, "shell", 1);
        assertThat(feedback.isSuccess()).isTrue();
    }

    @Test
    void shouldProcessFailedResultWithFullFeedback() {
        ToolResult result = new ToolResult(false, "Main.java:5: error: ';' expected", 1);
        Feedback feedback = pipeline.process(result, "shell", 1);
        assertThat(feedback.isSuccess()).isFalse();
        assertThat(feedback.getType()).isEqualTo(FailureType.COMPILE_ERROR);
        assertThat(feedback.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(feedback.getSuggestion()).contains("compilation error");
        assertThat(feedback.getSuggestion()).contains("Main.java:5");
    }

    @Test
    void shouldGenerateFeedbackForFailedToolResult() {
        ToolResult result = new ToolResult(false, "Main.java:5: error: ';' expected", 1);
        Feedback feedback = pipeline.collect(result);
        assertThat(feedback.isSuccess()).isFalse();
        assertThat(feedback.getType()).isEqualTo(FailureType.COMPILE_ERROR);
        assertThat(feedback.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(feedback.getSuggestion()).isNotBlank();
    }

    @Test
    void shouldInjectFeedbackIntoContextAfterFailure() {
        ToolResult result = new ToolResult(false, "Main.java:5: error: ';' expected", 1);
        pipeline.collect(result);
        List<String> additions = pipeline.getContextAdditions();
        assertThat(additions).isNotEmpty();
        assertThat(additions).hasSize(1);
        assertThat(additions.get(0)).contains("compilation error");
    }

    @Test
    void shouldNotGenerateErrorFeedbackForSuccessfulResult() {
        ToolResult result = new ToolResult(true, "Build successful");
        Feedback feedback = pipeline.collect(result);
        assertThat(feedback.isSuccess()).isTrue();
        assertThat(pipeline.getContextAdditions()).isEmpty();
    }
}