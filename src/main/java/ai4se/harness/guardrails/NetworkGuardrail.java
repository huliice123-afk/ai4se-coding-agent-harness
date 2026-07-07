package ai4se.harness.guardrails;

import java.util.List;
import java.util.Map;

public class NetworkGuardrail implements Guardrail {
    private static final List<String> BLOCKED = List.of("curl", "wget", "nc ", "telnet");

    @Override
    public String name() { return "network-guardrail"; }

    @Override
    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        if (!"shell".equals(actionName)) return GuardResult.pass();

        String command = (String) actionParams.get("command");
        if (command == null) return GuardResult.pass();

        String lower = command.toLowerCase();
        for (String blocked : BLOCKED) {
            if (lower.contains(blocked)) {
                return GuardResult.block("Network request blocked: " + blocked);
            }
        }
        return GuardResult.pass();
    }
}