package com.thyagotoledo.companions.core.quest;

import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;
import com.thyagotoledo.companions.core.planner.RecipeCatalog;
import com.thyagotoledo.companions.core.planner.RecipeRequirement;

import java.util.*;

public class QuestPlanner {
    private final QuestService questService;
    private final RecipeCatalog recipeCatalog;

    public QuestPlanner(QuestService questService, RecipeCatalog recipeCatalog) {
        this.questService = questService;
        this.recipeCatalog = recipeCatalog != null ? recipeCatalog : new RecipeCatalog();
    }

    public QuestPlanResult planQuest(UUID playerUuid, String questId, InventorySnapshot playerInv, InventorySnapshot companionInv) {
        if (questId == null || questService == null) {
            return new QuestPlanResult(QuestPlanResult.Status.QUEST_NOT_FOUND, questId, Collections.emptyMap(), Collections.emptyMap(), "Quest nao encontrada.");
        }

        Quest quest = questService.getQuest(playerUuid, questId);
        if (quest == null || quest.isHidden()) {
            return new QuestPlanResult(QuestPlanResult.Status.QUEST_NOT_FOUND, questId, Collections.emptyMap(), Collections.emptyMap(), "Quest nao encontrada ou oculta.");
        }

        if (!questService.areDependenciesMet(playerUuid, questId)) {
            return new QuestPlanResult(QuestPlanResult.Status.BLOCKED_DEPENDENCY, questId, Collections.emptyMap(), Collections.emptyMap(),
                    "A missao possui dependencias pendentes que precisam ser concluidas primeiro.");
        }

        Map<String, Integer> missingItems = new LinkedHashMap<>();
        Map<String, Integer> rawMaterialsNeeded = new LinkedHashMap<>();
        boolean unknownMachineFound = false;

        Map<String, Integer> availableCounts = new HashMap<>();
        if (playerInv != null) {
            for (ItemSlot slot : playerInv.getSlots()) {
                availableCounts.put(slot.getItemId(), availableCounts.getOrDefault(slot.getItemId(), 0) + slot.getCount());
            }
        }
        if (companionInv != null) {
            for (ItemSlot slot : companionInv.getSlots()) {
                availableCounts.put(slot.getItemId(), availableCounts.getOrDefault(slot.getItemId(), 0) + slot.getCount());
            }
        }

        for (QuestTask task : quest.getTasks()) {
            if (task.getType() == QuestTask.Type.ITEM) {
                int needed = task.getRemainingCount();
                int have = availableCounts.getOrDefault(task.getTargetId(), 0);
                if (have < needed) {
                    int diff = needed - have;
                    missingItems.put(task.getTargetId(), diff);

                    RecipeRequirement recipe = recipeCatalog.getRecipe(task.getTargetId());
                    if (recipe != null) {
                        int outputPerCraft = Math.max(1, recipe.getOutput().getCount());
                        int craftOps = (diff + outputPerCraft - 1) / outputPerCraft;
                        for (ItemSlot input : recipe.getInputs()) {
                            int totalInput = input.getCount() * craftOps;
                            int haveInput = availableCounts.getOrDefault(input.getItemId(), 0);
                            int missingInput = totalInput - haveInput;
                            if (missingInput > 0) {
                                rawMaterialsNeeded.put(input.getItemId(),
                                        rawMaterialsNeeded.getOrDefault(input.getItemId(), 0) + missingInput);
                            }
                        }
                    } else if (task.getTargetId().contains("machine") || task.getTargetId().contains("furnace") || task.getTargetId().contains("botania") || task.getTargetId().contains("create")) {
                        unknownMachineFound = true;
                    }
                }
            }
        }

        if (missingItems.isEmpty()) {
            return new QuestPlanResult(QuestPlanResult.Status.READY_TO_SUBMIT, questId, Collections.emptyMap(), Collections.emptyMap(),
                    "Todos os itens da missao estao prontos para entrega.");
        }

        if (unknownMachineFound && rawMaterialsNeeded.isEmpty()) {
            return new QuestPlanResult(QuestPlanResult.Status.UNKNOWN_RECIPE_OR_MACHINE, questId, missingItems, Collections.emptyMap(),
                    "Este item requer uma maquina ou processo avancado sem receita no catalogo comum.");
        }

        StringBuilder expl = new StringBuilder("Faltam itens para a missao: ");
        for (Map.Entry<String, Integer> entry : missingItems.entrySet()) {
            expl.append(entry.getValue()).append("x ").append(entry.getKey()).append(", ");
        }
        if (expl.length() > 2) {
            expl.setLength(expl.length() - 2);
        }

        return new QuestPlanResult(QuestPlanResult.Status.MISSING_ITEMS, questId, missingItems, rawMaterialsNeeded, expl.toString());
    }

    public QuestService getQuestService() {
        return questService;
    }

    public RecipeCatalog getRecipeCatalog() {
        return recipeCatalog;
    }
}
