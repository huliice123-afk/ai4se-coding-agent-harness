package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;

public class FeedbackPipeline {
    private final FeedbackCollector collector = new FeedbackCollector();
    private final SeverityJudge judge = new SeverityJudge();
    private final CorrectionSuggester suggester = new CorrectionSuggester();

    public Feedback process(ToolResult result, String actionName, int round) {
        Feedback feedback = collector.collect(result, actionName);
        if (feedback.isSuccess()) return feedback;

        Severity severity = judge.judge(feedback.getType());
        String suggestion = suggester.suggest(feedback.getType(), result.getOutput());

        return new Feedback(false, feedback.getType(), severity, suggestion);
    }
}