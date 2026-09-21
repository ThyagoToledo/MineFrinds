package com.thyagotoledo.companions.core.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Plano sequencial pequeno; dependências e execução permanecem no servidor. */
public final class TaskPlan {
    private final long revision;
    private final List<TaskStep> steps;
    private int current;

    public TaskPlan(long revision, List<TaskStep> steps) {
        this.revision = Math.max(0L, revision);
        this.steps = new ArrayList<>();
        if (steps != null) this.steps.addAll(steps);
    }

    public TaskStep current() {
        while (current < steps.size() && steps.get(current).getStatus() == TaskStatus.COMPLETED) current++;
        return current < steps.size() ? steps.get(current) : null;
    }

    public boolean startCurrent() { TaskStep step = current(); return step != null && step.start(); }

    public boolean applyCurrent(ActionResult result) {
        TaskStep step = current();
        if (step == null || !step.apply(result)) return false;
        if (step.getStatus() == TaskStatus.COMPLETED) current++;
        return true;
    }

    public void cancel() { TaskStep step = current(); if (step != null) step.cancel(); }
    public long getRevision() { return revision; }
    public int getCurrentIndex() { return current; }
    public List<TaskStep> getSteps() { return Collections.unmodifiableList(steps); }
    public boolean isComplete() { return current() == null; }
}
