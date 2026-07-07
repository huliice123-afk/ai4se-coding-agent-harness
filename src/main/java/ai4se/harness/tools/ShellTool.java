package ai4se.harness.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final long timeoutSeconds;

    public ShellTool(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String getName() { return "shell"; }

    @Override
    public String getDescription() { return "Execute shell commands"; }

    @Override
    public String getParameters() {
        return "{\"type\":\"object\",\"properties\":{"
            + "\"command\":{\"type\":\"string\",\"description\":\"Shell command to execute\"}"
            + "},\"required\":[\"command\"]}";
    }

    @Override
    public ToolResult execute(String args) {
        Map<String, Object> params;
        try {
            params = MAPPER.readValue(args, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new ToolResult(false, "Invalid arguments: " + e.getMessage());
        }

        String command = (String) params.get("command");
        if (command == null) return new ToolResult(false, "Missing required parameter: command");

        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolResult(false, "Command timed out after " + timeoutSeconds + "s\n" + output.toString());
            }

            int exitCode = process.exitValue();
            return new ToolResult(exitCode == 0, output.toString(), exitCode);
        } catch (Exception e) {
            return new ToolResult(false, "Execution error: " + e.getMessage());
        }
    }
}
