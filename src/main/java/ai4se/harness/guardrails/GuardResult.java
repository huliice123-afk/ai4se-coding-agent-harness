package ai4se.harness.guardrails;

public class GuardResult {
    public enum Status { PASS, BLOCK, HITL }

    private final Status status;
    private final String reason;

    public GuardResult(Status status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public static GuardResult pass() { return new GuardResult(Status.PASS, ""); }
    public static GuardResult block(String reason) { return new GuardResult(Status.BLOCK, reason); }
    public static GuardResult hitl(String reason) { return new GuardResult(Status.HITL, reason); }

    public Status getStatus() { return status; }
    public String getReason() { return reason; }
    public boolean isPass() { return status == Status.PASS; }
    public boolean isBlock() { return status == Status.BLOCK; }
    public boolean isHitl() { return status == Status.HITL; }
}