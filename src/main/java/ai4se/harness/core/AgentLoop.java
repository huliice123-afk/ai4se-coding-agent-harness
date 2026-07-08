package ai4se.harness.core;

import ai4se.harness.config.HarnessConfig;
import ai4se.harness.feedback.Feedback;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.feedback.FailureType;
import ai4se.harness.feedback.Severity;
import ai4se.harness.guardrails.GuardResult;
import ai4se.harness.guardrails.GuardrailChain;
import ai4se.harness.llm.*;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class AgentLoop {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmProvider llm;
    private final ToolRegistry tools;
    private final GuardrailChain guardrails;
    private final FeedbackPipeline feedback;
    private final ContextAssembler assembler;
    private final ActionParser parser;
    private final StopCondition stopCondition;
    private final MemoryRetriever memory;
    private final HarnessConfig config;

    public AgentLoop(LlmProvider llm, ToolRegistry tools, GuardrailChain guardrails,
                     FeedbackPipeline feedback, ContextAssembler assembler,
                     ActionParser parser, StopCondition stopCondition,
                     MemoryRetriever memory, HarnessConfig config) {
        this.llm = llm;
        this.tools = tools;
        this.guardrails = guardrails;
        this.feedback = feedback;
        this.assembler = assembler;
        this.parser = parser;
        this.stopCondition = stopCondition;
        this.memory = memory;
        this.config = config;
    }

    public String run(String task) {
        Conversation history = new Conversation();
        int round = 0;
        int correctionRound = 0;
        StringBuilder resultSummary = new StringBuilder();

        String lastSession = memory.search("session task", 1).stream().findFirst().orElse("");
        if (!lastSession.isEmpty()) {
            System.out.println("Previous session: " + lastSession.substring(0, Math.min(80, lastSession.length())));
        }

        while (round < config.getLoop().getMaxRounds()) {
            round++;
            System.out.println("[Round " + round + "]");

            List<Message> messages = assembler.assemble(task, tools.getAll(), memory, history);
            LlmResponse response = llm.complete(messages, tools.getAll());

            if (response == null) {
                System.out.println("[Agent] Empty response, stopping.");
                break;
            }

            if (stopCondition.shouldStop(response, round, config.getLoop().getMaxRounds())) {
                String text = response.getText();
                if (text != null) {
                    System.out.println("[Agent] " + text);
                }
                break;
            }

            Action action = parser.parse(response);

            if (action == null) {
                String text = response.getText();
                if (text != null && !text.isBlank()) {
                    System.out.println("[Agent] " + text);
                    history.add(new Message("assistant", text));
                } else {
                    System.out.println("[Agent] Empty response, stopping.");
                }
                break;
            }

            System.out.println("[Tool] " + action.getToolName());
            history.add(new Message("assistant", "Calling " + action.getToolName() + " with " + action.getToolArgs()));

            Map<String, Object> params = parseParams(action.getToolArgs());
            GuardResult guard = guardrails.check(action.getToolName(), params);
            if (!handleGuardrailResult(guard, action)) {
                Feedback fb = new Feedback(false, FailureType.COMMAND_REJECTED,
                    Severity.CRITICAL, guard.getReason());
                history.add(new Message("user", "[FEEDBACK] " + fb.getMessage()));
                correctionRound++;
                if (correctionRound > config.getFeedback().getMaxRounds()) {
                    return "Failed after " + correctionRound + " correction rounds.";
                }
                continue;
            }

            Optional<Tool> tool = tools.getTool(action.getToolName());
            if (tool.isEmpty()) {
                history.add(new Message("user", "[FEEDBACK] Unknown tool: " + action.getToolName()));
                continue;
            }

            ToolResult toolResult = tool.get().execute(action.getToolArgs());
            System.out.println("[Tool] " + action.getToolName() + " → " + toolResult.getOutput());

            Feedback fb = feedback.collect(toolResult);

            if (!fb.isSuccess()) {
                history.add(new Message("user", "[FEEDBACK] Failure type: " + fb.getType() +
                    "\nError details: " + toolResult.getOutput() +
                    "\nSuggestion: " + fb.getMessage()));
                correctionRound++;
                if (correctionRound > config.getFeedback().getMaxRounds()) {
                    resultSummary.append("Failed after ").append(correctionRound).append(" correction rounds.");
                    return resultSummary.toString();
                }
            } else {
                history.add(new Message("user", "[RESULT] " + toolResult.getOutput()));
                resultSummary.append(toolResult.getOutput());
                correctionRound = 0;
            }

            for (String addition : feedback.getContextAdditions()) {
                history.add(new Message("user", "[CONTEXT] " + addition));
            }
        }

        String summary = resultSummary.toString();
        if (!summary.isEmpty()) {
            memory.saveSessionSummary("Task: " + task + "\nResult: " + summary);
        }
        return summary.isEmpty() ? "Completed." : summary;
    }

    public void chat() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Chat mode. Type '退出' to exit.");

        Conversation history = new Conversation();
        StringBuilder chatSummary = new StringBuilder();

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            if (input.equals("退出")) break;

            history.add(new Message("user", input));
            chatSummary.append("User: ").append(input).append("\n");
            int round = 0;

            while (round < config.getLoop().getMaxRounds()) {
                round++;
                LlmResponse response = llm.complete(
                    assembler.assemble(input, tools.getAll(), memory, history),
                    tools.getAll());

                if (response == null) {
                    System.out.println("[Agent] Empty response, stopping.");
                    break;
                }

                Action action = parser.parse(response);

                if (action == null) {
                    String text = response.getText();
                    if (text != null && !text.isBlank()) {
                        System.out.println("[Agent] " + text);
                        history.add(new Message("assistant", text));
                        chatSummary.append("Agent: ").append(text).append("\n");
                    }
                    break;
                }

                System.out.println("[Tool] " + action.getToolName());
                history.add(new Message("assistant", "Calling " + action.getToolName() + " with " + action.getToolArgs()));

                Map<String, Object> params = parseParams(action.getToolArgs());
                GuardResult guard = guardrails.check(action.getToolName(), params);
                if (!guard.isPass()) {
                    System.out.println("[Blocked] " + guard.getReason());
                    history.add(new Message("assistant", "[Blocked] " + guard.getReason()));
                    continue;
                }

                Optional<Tool> tool = tools.getTool(action.getToolName());
                ToolResult toolResult = tool
                    .map(t -> t.execute(action.getToolArgs()))
                    .orElse(ToolResult.error("Unknown tool: " + action.getToolName()));
                System.out.println("[Tool] " + action.getToolName() + " → " + toolResult.getOutput());

                if (feedback != null) {
                    Feedback fb = feedback.collect(toolResult);
                    if (!fb.isSuccess()) {
                        history.add(new Message("user", "[FEEDBACK] Failure type: " + fb.getType() +
                            "\nError details: " + toolResult.getOutput() +
                            "\nSuggestion: " + fb.getMessage()));
                    } else {
                        history.add(new Message("user", "[RESULT] " + toolResult.getOutput()));
                    }
                    for (String addition : feedback.getContextAdditions()) {
                        history.add(new Message("user", "[CONTEXT] " + addition));
                    }
                } else {
                    history.add(new Message("user", "[RESULT] " + toolResult.getOutput()));
                }
            }
        }

        memory.saveSessionSummary("Chat session.\n" + chatSummary);
    }

    private Map<String, Object> parseParams(String args) {
        if (args == null || args.isBlank()) return Map.of();
        try {
            return MAPPER.readValue(args, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    boolean handleGuardrailResult(GuardResult guard, Action action) {
        if (guard.isPass()) {
            return true;
        }
        if (guard.isBlock()) {
            return false;
        }
        if (guard.isHitl()) {
            System.out.println("[WARNING] Human-in-the-loop: " + guard.getReason());
            System.out.println("Action: " + action.getToolName() + " " + action.getToolArgs());
            System.out.print("Proceed? (y/n): ");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String input = reader.readLine();
                return input != null && input.trim().equalsIgnoreCase("y");
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
