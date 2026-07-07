package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedbackPipeline {
    private final FeedbackCollector collector = new FeedbackCollector();
    private final SeverityJudge judge = new SeverityJudge();
    private final CorrectionSuggester suggester = new CorrectionSuggester();
    private final List<String> contextAdditions = new ArrayList<>();

    public Feedback process(ToolResult result, String actionName, int round) {
        Feedback feedback = collector.collect(result, actionName);
        if (feedback.isSuccess()) return feedback;

        Severity severity = judge.judge(feedback.getType());
        String suggestion = suggester.suggest(feedback.getType(), result.getOutput());

        return new Feedback(false, feedback.getType(), severity, suggestion);
    }

    public Feedback collect(ToolResult result) {
        Feedback feedback = process(result, "tool", 0);
        if (!feedback.isSuccess() && feedback.getSuggestion() != null) {
            contextAdditions.add(feedback.getSuggestion());
        }
        return feedback;
    }

    public List<String> getContextAdditions() {
        return Collections.unmodifiableList(contextAdditions);
    }
}