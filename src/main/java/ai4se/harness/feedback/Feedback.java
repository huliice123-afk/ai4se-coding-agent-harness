package ai4se.harness.feedback;

public class Feedback {
    private final boolean success;
    private final FailureType type;
    private final Severity severity;
    private final String suggestion;

    public Feedback(boolean success, FailureType type, Severity severity, String suggestion) {
        this.success = success;
        this.type = type;
        this.severity = severity;
        this.suggestion = suggestion;
    }

    public boolean isSuccess() { return success; }
    public FailureType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public String getSuggestion() { return suggestion; }
}