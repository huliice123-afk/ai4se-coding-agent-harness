package ai4se.harness.tools;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileTool implements Tool {
    private final Path projectRoot;

    public FileTool(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String name() { return "file"; }

    @Override
    public String description() { return "Read, write, or glob files within the project"; }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "read");
        String path = (String) params.get("path");
        if (path == null) return new ToolResult(false, "Missing required parameter: path");

        Path resolved = projectRoot.resolve(path).normalize();
        if (!resolved.startsWith(projectRoot)) {
            return new ToolResult(false, "Access denied: path outside project root");
        }

        try {
            switch (action) {
                case "read": return readFile(resolved);
                case "write": return writeFile(resolved, (String) params.get("content"));
                case "glob": return globFiles((String) params.getOrDefault("pattern", "*"));
                default: return new ToolResult(false, "Unknown action: " + action);
            }
        } catch (IOException e) {
            return new ToolResult(false, "IO error: " + e.getMessage());
        }
    }

    private ToolResult readFile(Path path) throws IOException {
        if (!Files.exists(path)) return new ToolResult(false, "File not found: " + path);
        String content = Files.readString(path);
        return new ToolResult(true, content);
    }

    private ToolResult writeFile(Path path, String content) throws IOException {
        if (content == null) return new ToolResult(false, "Missing required parameter: content");
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return new ToolResult(true, "Written to " + path);
    }

    private ToolResult globFiles(String pattern) throws IOException {
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            String result = stream
                .filter(p -> p.getFileName().toString().contains(pattern.replace("*", "")))
                .map(p -> projectRoot.relativize(p).toString())
                .limit(50)
                .collect(Collectors.joining("\n"));
            return new ToolResult(true, result.isEmpty() ? "No files matched" : result);
        }
    }
}