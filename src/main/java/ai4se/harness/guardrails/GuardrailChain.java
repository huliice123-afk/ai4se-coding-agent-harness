package ai4se.harness.guardrails;

import java.util.List;
import java.util.Map;

public class GuardrailChain {
    private final List<Guardrail> guardrails;

    public GuardrailChain(List<Guardrail> guardrails) {
        this.guardrails = guardrails;
    }

    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        for (Guardrail guardrail : guardrails) {
            GuardResult result = guardrail.check(actionName, actionParams);
            if (!result.isPass()) {
                return result;
            }
        }
        return GuardResult.pass();
    }
}