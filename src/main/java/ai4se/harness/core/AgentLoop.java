package ai4se.harness.core;

import ai4se.harness.config.HarnessConfig;
import ai4se.harness.feedback.Feedback;
import ai4se.harness.feedback.FeedbackPipeline;
import ai4se.harness.guardrails.GuardResult;
import ai4se.harness.guardrails.GuardrailChain;
import ai4se.harness.llm.*;
import ai4se.harness.memory.MemoryRetriever;
import ai4se.harness.tools.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

public class AgentLoop {
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

            if (stopCondition.shouldStop(response, round, config.getLoop().getMaxRounds())) {
                return response.getText() != null ? response.getText() : "Completed.";
            }

            Action action = parser.parse(response);
            if (action == null) {
                String text = response.getText();
                if (text == null || text.isBlank()) {
                    System.out.println("LLM returned empty response, stopping");
                    return "Agent stopped: received empty response from LLM after " + round + " rounds.";
                }
                System.out.println(text);
                history.add(new Message("assistant", text));
                resultSummary.append(text).append("\n");
                correctionRound = 0;
                continue;
            }

            System.out.println("[Tool: " + action.getToolName() + "]");

            GuardResult guard = guardrails.check(action.getToolName(), action.getParams());
            if (!handleGuardrailResult(guard, action)) {
                Feedback fb = new Feedback(false, ai4se.harness.feedback.FailureType.COMMAND_REJECTED,
                    ai4se.harness.feedback.Severity.FATAL, guard.getReason());
                history.add(new Message("user", "[FEEDBACK] " + fb.getSuggestion()));
                correctionRound++;
                if (correctionRound > config.getFeedback().getMaxRounds()) {
                    return "Failed after " + correctionRound + " correction rounds.";
                }
                continue;
            }

            Optional<Tool> tool = tools.get(action.getToolName());
            if (tool.isEmpty()) {
                history.add(new Message("user", "[FEEDBACK] Unknown tool: " + action.getToolName()));
                continue;
            }

            ToolResult toolResult = tool.get().execute(action.getParams());
            System.out.println(toolResult.getOutput());
            Feedback fb = feedback.process(toolResult, action.getToolName(), round);

            if (!fb.isSuccess()) {
                history.add(new Message("user", "[FEEDBACK] Failure type: " + fb.getType() +
                    "\nError details: " + toolResult.getOutput() +
                    "\nSuggestion: " + fb.getSuggestion()));
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
        }

        String summary = resultSummary.toString();
        if (!summary.isEmpty()) {
            memory.save("session_latest", "Task: " + task + "\nResult: " + summary);
        }
        return summary;
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
            System.out.println("Action: " + action.getToolName() + " " + action.getParams());
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