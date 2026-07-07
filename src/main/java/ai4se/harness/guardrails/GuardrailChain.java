package ai4se.harness.guardrails;

import ai4se.harness.config.HarnessConfig;
import java.util.List;
import java.util.Map;

public class GuardrailChain {
    private final List<Guardrail> guardrails;
    private final boolean hitlMode;

    public GuardrailChain(List<Guardrail> guardrails) {
        this.guardrails = guardrails;
        this.hitlMode = false;
    }

    public GuardrailChain(List<Guardrail> guardrails, HarnessConfig config) {
        this.guardrails = guardrails;
        this.hitlMode = config.getGuardrails() != null && config.getGuardrails().isHitl();
    }

    public GuardResult check(String actionName, Map<String, Object> actionParams) {
        for (Guardrail guardrail : guardrails) {
            GuardResult result = guardrail.check(actionName, actionParams);
            if (!result.isPass()) {
                if (hitlMode && result.isBlock()) {
                    return GuardResult.hitl(result.getReason());
                }
                return result;
            }
        }
        return GuardResult.pass();
    }
}