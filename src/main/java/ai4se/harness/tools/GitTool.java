package ai4se.harness.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GitTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Path projectRoot;

    public GitTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String getName() { return "git"; }

    @Override
    public String getDescription() { return "Git operations: status, diff, commit, branch, log"; }

    @Override
    public String getParameters() {
        return "{\"type\":\"object\",\"properties\":{"
            + "\"action\":{\"type\":\"string\",\"enum\":[\"status\",\"diff\",\"diff-staged\",\"log\",\"branch\"],\"description\":\"Git action to perform\"}"
            + "},\"required\":[\"action\"]}";
    }

    @Override
    public ToolResult execute(String args) {
        Map<String, Object> params;
        try {
            params = MAPPER.readValue(args, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new ToolResult(false, "Invalid arguments: " + e.getMessage());
        }

        String action = (String) params.get("action");
        if (action == null) return new ToolResult(false, "Missing required parameter: action");

        try {
            switch (action) {
                case "status": return runGit("status", "--short");
                case "diff": return runGit("diff");
                case "diff-staged": return runGit("diff", "--staged");
                case "log": return runGit("log", "--oneline", "-10");
                case "branch": return runGit("branch");
                default: return new ToolResult(false, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return new ToolResult(false, "Git error: " + e.getMessage());
        }
    }

    private ToolResult runGit(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "git";
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd).directory(projectRoot.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
        }

        process.waitFor(10, TimeUnit.SECONDS);
        int exitCode = process.exitValue();
        return new ToolResult(exitCode == 0, output.toString(), exitCode);
    }
}
