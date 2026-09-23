package com.thyagotoledo.companions.neoforge.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningScanBudgetTest {
    @Test
    void budgetIsSharedByAllCompanionsForOneTick() {
        MiningScanBudget.resetForTests();
        for (int i = 0; i < MiningScanBudget.maxPerTick(); i++) {
            assertTrue(MiningScanBudget.tryAcquire(10L));
        }
        assertFalse(MiningScanBudget.tryAcquire(10L));
        assertTrue(MiningScanBudget.tryAcquire(11L));
    }
}
