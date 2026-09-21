package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.planner.ActionResult;
import com.thyagotoledo.companions.core.planner.TaskPlan;
import com.thyagotoledo.companions.core.planner.TaskStatus;
import com.thyagotoledo.companions.core.planner.TaskStep;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TaskPlanTests {
    @Test
    void onlyServerResultCompletesAPlanAndProgressCanRetry() {
        TaskStep gather = new TaskStep("gather_logs", 2);
        TaskStep craft = new TaskStep("craft_pickaxe", 1);
        TaskPlan plan = new TaskPlan(7, Arrays.asList(gather, craft));

        assertTrue(plan.startCurrent());
        assertTrue(plan.applyCurrent(ActionResult.progress(3, "drops received")));
        assertEquals(TaskStatus.QUEUED, gather.getStatus());
        assertTrue(plan.startCurrent());
        assertTrue(plan.applyCurrent(ActionResult.succeeded(5, "target reached")));
        assertEquals(1, plan.getCurrentIndex());
        assertTrue(plan.startCurrent());
        assertTrue(plan.applyCurrent(ActionResult.succeeded(1, "crafted")));
        assertTrue(plan.isComplete());
    }

    @Test
    void failedStepBlocksAfterAttemptBudget() {
        TaskStep step = new TaskStep("mine", 1);
        TaskPlan plan = new TaskPlan(1, Arrays.asList(step));
        assertTrue(plan.startCurrent());
        assertTrue(plan.applyCurrent(ActionResult.failed(ActionResult.Code.NO_PATH, "no safe route")));
        assertEquals(TaskStatus.BLOCKED, step.getStatus());
        assertFalse(plan.startCurrent());
    }
}
