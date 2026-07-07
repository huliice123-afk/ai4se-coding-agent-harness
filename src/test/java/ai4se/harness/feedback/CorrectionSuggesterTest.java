package ai4se.harness.feedback;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CorrectionSuggesterTest {
    private final CorrectionSuggester suggester = new CorrectionSuggester();

    @Test
    void shouldSuggestCompileFix() {
        String suggestion = suggester.suggest(FailureType.COMPILATION_ERROR, "Main.java:5: error: ';' expected");
        assertThat(suggestion).contains("compilation error");
        assertThat(suggestion).contains("Main.java:5");
    }

    @Test
    void shouldSuggestTestFix() {
        String suggestion = suggester.suggest(FailureType.TEST_FAILURE, "Expected: 42 but was: 0");
        assertThat(suggestion).contains("test failure");
    }

    @Test
    void shouldSuggestAlternativeForRejected() {
        String suggestion = suggester.suggest(FailureType.COMMAND_REJECTED, "rm -rf blocked");
        assertThat(suggestion).contains("blocked");
        assertThat(suggestion).contains("alternative");
    }
}