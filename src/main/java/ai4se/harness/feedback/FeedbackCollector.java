package ai4se.harness.feedback;

import ai4se.harness.tools.ToolResult;

public class FeedbackCollector {
    private final FailureClassifier classifier = new FailureClassifier();

    public Feedback collect(ToolResult result, String actionName) {
        if (result.isSuccess()) {
            return new Feedback(true, FailureType.UNKNOWN, Severity.INFO, null);
        }
        FailureType type = classifier.classify(result.getOutput(), result.getExitCode(), actionName);
        return new Feedback(false, type, null, null);
    }
}