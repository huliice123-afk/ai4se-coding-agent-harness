package ai4se.harness.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class SearchTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Path projectRoot;

    public SearchTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String getName() { return "search"; }

    @Override
    public String getDescription() { return "Search files: grep for content, glob for filenames"; }

    @Override
    public String getParameters() {
        return "{\"type\":\"object\",\"properties\":{"
            + "\"action\":{\"type\":\"string\",\"enum\":[\"grep\",\"glob\"],\"description\":\"Search action\"},"
            + "\"pattern\":{\"type\":\"string\",\"description\":\"Search pattern\"}"
            + "},\"required\":[\"pattern\"]}";
    }

    @Override
    public ToolResult execute(String args) {
        Map<String, Object> params;
        try {
            params = MAPPER.readValue(args, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new ToolResult(false, "Invalid arguments: " + e.getMessage());
        }

        String action = (String) params.getOrDefault("action", "grep");
        String pattern = (String) params.get("pattern");
        if (pattern == null) return new ToolResult(false, "Missing required parameter: pattern");

        try {
            switch (action) {
                case "grep": return grep(pattern);
                case "glob": return glob(pattern);
                default: return new ToolResult(false, "Unknown action: " + action);
            }
        } catch (IOException e) {
            return new ToolResult(false, "Search error: " + e.getMessage());
        }
    }

    private ToolResult grep(String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            stream.filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains(File.separator + "."))
                .forEach(p -> {
                    try {
                        for (String line : Files.readAllLines(p)) {
                            if (line.contains(pattern)) {
                                sb.append(projectRoot.relativize(p)).append(": ").append(line).append("\n");
                            }
                        }
                    } catch (IOException ignored) {}
                });
        }
        String result = sb.toString();
        return new ToolResult(true, result.isEmpty() ? "No matches found" : result);
    }

    private ToolResult glob(String pattern) throws IOException {
        String result;
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            result = stream
                .filter(p -> !p.toString().contains(File.separator + "."))
                .filter(p -> p.getFileName().toString().matches(globToRegex(pattern)))
                .map(p -> projectRoot.relativize(p).toString())
                .limit(50)
                .collect(Collectors.joining("\n"));
        }
        return new ToolResult(true, result.isEmpty() ? "No files matched" : result);
    }

    private String globToRegex(String glob) {
        return glob.replace(".", "\\.").replace("*", ".*").replace("?", ".");
    }
}
