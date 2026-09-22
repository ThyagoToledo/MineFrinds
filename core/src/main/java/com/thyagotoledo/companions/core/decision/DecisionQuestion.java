package com.thyagotoledo.companions.core.decision;

/** Pergunta estruturada do contrato Jev-like local; nunca contém execução. */
public interface DecisionQuestion {
    enum Type { CHOICE, SCORE, NOUL }

    String getId();
    Type getType();
    String getInstructions();
}
