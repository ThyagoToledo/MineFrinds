package com.thyagotoledo.companions.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plano puro de consumo e produção. Adaptadores do Minecraft devem montar o plano
 * a partir da receita efetiva e só chamar commit depois de validar o inventário real.
 */
public final class CraftTransaction {
    public enum Status {
        SUCCESS,
        INVALID,
        INSUFFICIENT_INPUT,
        OUTPUT_FULL
    }

    public static final class Plan {
        private final Map<String, Integer> requirements;
        private final String output;
        private final int outputCount;
        private final int outputExisting;
        private final int outputCapacity;

        private Plan(Map<String, Integer> requirements, String output, int outputCount,
                     int outputExisting, int outputCapacity) {
            this.requirements = Collections.unmodifiableMap(new LinkedHashMap<>(requirements));
            this.output = output;
            this.outputCount = outputCount;
            this.outputExisting = outputExisting;
            this.outputCapacity = outputCapacity;
        }

        public Map<String, Integer> getRequirements() { return requirements; }
        public String getOutput() { return output; }
        public int getOutputCount() { return outputCount; }

        /**
         * Aplica tudo de uma vez numa cópia e só altera o mapa original após todas
         * as pré-condições passarem. Em caso de falha, o inventário permanece igual.
         */
        public Status commit(Map<String, Integer> inventory) {
            if (inventory == null || output == null || output.isEmpty() || outputCount <= 0
                    || outputCapacity < outputExisting || outputExisting < 0) {
                return Status.INVALID;
            }
            Map<String, Integer> next = new LinkedHashMap<>(inventory);
            for (Map.Entry<String, Integer> requirement : requirements.entrySet()) {
                String item = requirement.getKey();
                int needed = requirement.getValue() != null ? requirement.getValue() : 0;
                if (item == null || item.isEmpty() || needed <= 0) return Status.INVALID;
                int available = positive(next.get(item));
                if (available < needed) return Status.INSUFFICIENT_INPUT;
                set(next, item, available - needed);
            }
            if (outputExisting + outputCount > outputCapacity) return Status.OUTPUT_FULL;
            set(next, output, positive(next.get(output)) + outputCount);
            inventory.clear();
            inventory.putAll(next);
            return Status.SUCCESS;
        }
    }

    private CraftTransaction() { }

    public static Plan plan(Map<String, Integer> requirements, String output, int outputCount,
                            int outputExisting, int outputCapacity) {
        Map<String, Integer> safe = requirements == null
                ? Collections.<String, Integer>emptyMap()
                : new LinkedHashMap<>(requirements);
        return new Plan(safe, output, outputCount, outputExisting, outputCapacity);
    }

    private static int positive(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private static void set(Map<String, Integer> inventory, String item, int count) {
        if (count <= 0) inventory.remove(item);
        else inventory.put(item, count);
    }
}
