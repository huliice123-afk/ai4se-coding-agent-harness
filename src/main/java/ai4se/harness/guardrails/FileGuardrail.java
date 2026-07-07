package ai4se.harness.guardrails;

import java.nio.file.Path;
import java.util.Map;

public class FileGuardrail implements Guardrail {
    private final Path projectRoot;

    public FileGuardrail(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public String name() { return "file-guardrail"; }

    @Override
    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        if (!"file".equals(actionName)) return GuardResult.pass();

        String path = (String) actionParams.get("path");
        if (path == null) return GuardResult.pass();

        Path resolved = projectRoot.resolve(path).normalize();
        if (!resolved.startsWith(projectRoot)) {
            return GuardResult.block("File access outside project root: " + path);
        }
        return GuardResult.pass();
    }
}