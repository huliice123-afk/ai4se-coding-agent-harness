package ai4se.harness.core;

import ai4se.harness.llm.LlmResponse;

public class StopCondition {
    public boolean shouldStop(LlmResponse response, int round, int maxRounds) {
        if (round >= maxRounds) return true;
        if ("end_turn".equals(response.getStopReason())) return true;
        if ("stop_sequence".equals(response.getStopReason())) return true;
        return false;
    }
}