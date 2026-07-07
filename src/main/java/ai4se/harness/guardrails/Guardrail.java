package ai4se.harness.guardrails;

import java.util.Map;

public interface Guardrail {
    String name();
    GuardResult check(String actionName, Map<String, Object> actionParams);
}