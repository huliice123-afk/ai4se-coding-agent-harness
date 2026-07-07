package ai4se.harness.guardrails;

import java.util.List;
import java.util.Map;

public class CommandGuardrail implements Guardrail {
    private final List<String> denylist;

    public CommandGuardrail(List<String> denylist) {
        this.denylist = denylist;
    }

    @Override
    public String name() { return "command-guardrail"; }

    @Override
    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        if (!"shell".equals(actionName)) return GuardResult.pass();

        String command = (String) actionParams.get("command");
        if (command == null) return GuardResult.pass();

        String lower = command.toLowerCase();
        for (String dangerous : denylist) {
            if (lower.contains(dangerous.toLowerCase())) {
                return GuardResult.block("Dangerous command detected: " + dangerous);
            }
        }
        return GuardResult.pass();
    }
}