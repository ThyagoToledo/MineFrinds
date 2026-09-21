package com.thyagotoledo.companions.core.planner;

import java.util.UUID;

/** Passo de tarefa com identificador estável para rejeitar respostas tardias. */
public final class TaskStep {
    private final UUID executionId;
    private final String actionId;
    private final int maxAttempts;
    private int attempts;
    private TaskStatus status = TaskStatus.QUEUED;

    public TaskStep(String actionId, int maxAttempts) {
        this.executionId = UUID.randomUUID();
        this.actionId = actionId != null ? actionId : "unknown";
        this.maxAttempts = Math.max(1, Math.min(32, maxAttempts));
    }

    public boolean start() {
        if (status != TaskStatus.QUEUED && status != TaskStatus.BLOCKED) return false;
        if (attempts >= maxAttempts) { status = TaskStatus.BLOCKED; return false; }
        attempts++;
        status = TaskStatus.RUNNING;
        return true;
    }

    public boolean apply(ActionResult result) {
        if (result == null || status != TaskStatus.RUNNING) return false;
        if (result.getCode() == ActionResult.Code.SUCCEEDED) status = TaskStatus.COMPLETED;
        else if (result.getCode() == ActionResult.Code.CANCELLED) status = TaskStatus.CANCELLED;
        else if (result.isTerminal()) status = TaskStatus.BLOCKED;
        else if (attempts >= maxAttempts) status = TaskStatus.BLOCKED;
        else status = TaskStatus.QUEUED;
        return true;
    }

    public void cancel() { if (!status.equals(TaskStatus.COMPLETED)) status = TaskStatus.CANCELLED; }
    public UUID getExecutionId() { return executionId; }
    public String getActionId() { return actionId; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public TaskStatus getStatus() { return status; }
}
