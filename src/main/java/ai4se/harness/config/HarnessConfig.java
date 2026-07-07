package ai4se.harness.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class HarnessConfig {
    private LlmConfig llm;
    private ToolsConfig tools;
    private GuardrailsConfig guardrails;
    private FeedbackConfig feedback;
    private LoopConfig loop;
    private MemoryConfig memory;

    public LlmConfig getLlm() { return llm; }
    public void setLlm(LlmConfig llm) { this.llm = llm; }
    public ToolsConfig getTools() { return tools; }
    public void setTools(ToolsConfig tools) { this.tools = tools; }
    public GuardrailsConfig getGuardrails() { return guardrails; }
    public void setGuardrails(GuardrailsConfig guardrails) { this.guardrails = guardrails; }
    public FeedbackConfig getFeedback() { return feedback; }
    public void setFeedback(FeedbackConfig feedback) { this.feedback = feedback; }
    public LoopConfig getLoop() { return loop; }
    public void setLoop(LoopConfig loop) { this.loop = loop; }
    public MemoryConfig getMemory() { return memory; }
    public void setMemory(MemoryConfig memory) { this.memory = memory; }

    public static class LlmConfig {
        private String provider;
        private String model;
        @JsonProperty("max_tokens")
        private int maxTokens;
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class ToolsConfig {
        private List<String> allowed;
        @JsonProperty("shell_timeout")
        private int shellTimeout;
        public List<String> getAllowed() { return allowed; }
        public void setAllowed(List<String> allowed) { this.allowed = allowed; }
        public int getShellTimeout() { return shellTimeout; }
        public void setShellTimeout(int shellTimeout) { this.shellTimeout = shellTimeout; }
    }

    public static class GuardrailsConfig {
        private boolean hitl;
        @JsonProperty("command_denylist")
        private List<String> commandDenylist;
        @JsonProperty("network_blocked")
        private boolean networkBlocked;
        public boolean isHitl() { return hitl; }
        public void setHitl(boolean hitl) { this.hitl = hitl; }
        public List<String> getCommandDenylist() { return commandDenylist; }
        public void setCommandDenylist(List<String> commandDenylist) { this.commandDenylist = commandDenylist; }
        public boolean isNetworkBlocked() { return networkBlocked; }
        public void setNetworkBlocked(boolean networkBlocked) { this.networkBlocked = networkBlocked; }
    }

    public static class FeedbackConfig {
        @JsonProperty("max_rounds")
        private int maxRounds;
        public int getMaxRounds() { return maxRounds; }
        public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
    }

    public static class LoopConfig {
        @JsonProperty("max_rounds")
        private int maxRounds;
        public int getMaxRounds() { return maxRounds; }
        public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
    }

    public static class MemoryConfig {
        @JsonProperty("store_path")
        private String storePath;
        @JsonProperty("search_top_k")
        private int searchTopK;
        public String getStorePath() { return storePath; }
        public void setStorePath(String storePath) { this.storePath = storePath; }
        public int getSearchTopK() { return searchTopK; }
        public void setSearchTopK(int searchTopK) { this.searchTopK = searchTopK; }
    }
}