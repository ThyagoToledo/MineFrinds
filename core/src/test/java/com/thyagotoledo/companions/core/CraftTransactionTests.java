package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.model.CraftTransaction;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CraftTransactionTests {
    @Test
    void successConsumesAndProducesAtomically() {
        Map<String, Integer> inventory = new LinkedHashMap<>();
        inventory.put("oak_log", 2);
        inventory.put("stick", 1);

        CraftTransaction.Plan plan = CraftTransaction.plan(
                mapOf("oak_log", 1, "stick", 1), "oak_planks", 4, 0, 64);

        assertEquals(CraftTransaction.Status.SUCCESS, plan.commit(inventory));
        assertEquals(1, inventory.get("oak_log"));
        assertEquals(4, inventory.get("oak_planks"));
        if (inventory.containsKey("stick")) assertEquals(0, inventory.get("stick"));
    }

    @Test
    void insufficientInputLeavesInventoryUntouched() {
        Map<String, Integer> inventory = new LinkedHashMap<>();
        inventory.put("oak_log", 1);
        Map<String, Integer> before = new LinkedHashMap<>(inventory);

        CraftTransaction.Plan plan = CraftTransaction.plan(
                mapOf("oak_log", 2), "oak_planks", 4, 0, 64);

        assertEquals(CraftTransaction.Status.INSUFFICIENT_INPUT, plan.commit(inventory));
        assertEquals(before, inventory);
    }

    @Test
    void fullOutputLeavesInputsUntouched() {
        Map<String, Integer> inventory = new LinkedHashMap<>();
        inventory.put("oak_log", 1);
        inventory.put("oak_planks", 63);
        Map<String, Integer> before = new LinkedHashMap<>(inventory);

        CraftTransaction.Plan plan = CraftTransaction.plan(
                mapOf("oak_log", 1), "oak_planks", 4, 63, 64);

        assertEquals(CraftTransaction.Status.OUTPUT_FULL, plan.commit(inventory));
        assertEquals(before, inventory);
    }

    @Test
    void invalidRequirementIsRejectedWithoutMutation() {
        Map<String, Integer> inventory = new LinkedHashMap<>();
        inventory.put("oak_log", 2);
        Map<String, Integer> before = new LinkedHashMap<>(inventory);

        CraftTransaction.Plan plan = CraftTransaction.plan(
                mapOf("", 1), "oak_planks", 4, 0, 64);

        assertEquals(CraftTransaction.Status.INVALID, plan.commit(inventory));
        assertEquals(before, inventory);
    }

    private static Map<String, Integer> mapOf(String firstKey, int firstValue,
                                              Object... rest) {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put(firstKey, firstValue);
        for (int i = 0; i + 1 < rest.length; i += 2) {
            result.put(String.valueOf(rest[i]), ((Number) rest[i + 1]).intValue());
        }
        return result;
    }
}
