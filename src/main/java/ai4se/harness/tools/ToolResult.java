package ai4se.harness.tools;

public class ToolResult {
    private final boolean success;
    private final String output;
    private final int exitCode;

    public ToolResult(boolean success, String output) {
        this(success, output, success ? 0 : 1);
    }

    public ToolResult(boolean success, String output, int exitCode) {
        this.success = success;
        this.output = output;
        this.exitCode = exitCode;
    }

    public boolean isSuccess() { return success; }
    public String getOutput() { return output; }
    public int getExitCode() { return exitCode; }
}