---
tags: [minecraft, port, forge, 1.12.2, java8, legacy, p8]
updated: 2026-09-19
status: p8-concluido-aprovado
---

# Relatorio de Port e Homologacao P8: Forge 1.12.2 (Java 8)

Autor: ThyagoToledo
Escopo: Etapa P8 do Roadmap (minecraft-companheiros-plano.md)
Data da medicao: 19/09/2026

---

## 1. Contexto e Objetivos

A Etapa P8 realizou a criacao da plataforma para o ambiente legado **Minecraft 1.12.2 (Forge 14.23.5.x)** executando em **Java 8 puro**.

Gracas a decisao arquitetural estabelecida no inicio do projeto de manter o modulo compartilhado `:core` em Java 8 estrito (sem sintaxes modernas como `var` ou `List.of()`), a totalidade dos algoritmos de IA generativa hibrida, memoria circular de dialogo, planejamento de receitas e calculos de inventario foi reaproveitada de forma 100% direta e sem retrabalho.

---

## 2. Matriz de Diferencas Arquiteturais (1.12.2 vs Moderno)

| Aspecto | Forge 1.12.2 (Legado) | Forge 1.20.1 / NeoForge 1.21.1 (Moderno) |
| :--- | :--- | :--- |
| **Java Bytecode** | **Java 8** (`options.release = 8`) | Java 17 (1.20.1) / Java 21 (1.21.1) |
| **Metadados de Mod** | `mcmod.info` | `mods.toml` / `neoforge.mods.toml` |
| **Dicionarios de Idioma** | Formato chave=valor `.lang` (`pt_br.lang`, `en_us.lang`) | Arquivos JSON (`pt_br.json`, `en_us.json`) |
| **Camada de Rede** | `SimpleNetworkWrapper` (`IMessage`, `IMessageHandler`) | `SimpleChannel` (1.20.1) / `CustomPacketPayload` (1.21.1) |
| **IA de Entidades** | `EntityAIBase`, `tasks.addTask(priority, ai)` | `GoalSelector`, `Goal` |
| **Protecao de Terreno** | FTB Utilities / GriefPrevention (`BlockEvent.BreakEvent`) | FTB Chunks / Tensura OPAC |
| **Integracao de Quests** | FTB Quests legado / Better Questing | FTB Quests moderno / FTB Teams |

---

## 3. Entregas da Plataforma `platforms/forge-1.12.2`

1. **Estrutura de Build em Java 8**:
   - `build.gradle` configurado com `options.release = 8` e composite build com `:core`.
   - `mcmod.info` gerado com identificador `companions`, versao `0.1.0-1.12.2` e autoria exclusiva `ThyagoToledo`.
2. **Dicionarios Legados de Traducao (`.lang`)**:
   - `pt_br.lang` e `en_us.lang` gerados no diretorio `assets/companions/lang/`, mapeando 100% das mensagens bilingues de feedback, combate, ordens e quests.
3. **Adaptador de Rede Legado (`LegacyCompanionNetwork.java`)**:
   - Implementacao de `CommandMessage` e `FeedbackMessage` compativeis com o padrao `IMessage` do Forge 1.12.2, com serializacao e desserializacao binaria em streams `DataOutputStream` / `DataInputStream`.
4. **Servico de Permissoes Legado (`LegacyPermissionService.java`)**:
   - Adaptador desacoplado para checagem de claims em areas protegidas com seguranca contra nulos.
5. **Servico de Quests Legado (`LegacyQuestService.java`)**:
   - Suporte a selecao de missoes e invalidacao de cache via `/reload`.
6. **Entidade Companheira (`LegacyCompanionEntity.java`)**:
   - Integracao com `HybridDialogueProvider`, `ConversationMemory` e despachador de tarefas deterministico/generativo.

---

## 4. Resultados dos Testes Automatizados

A suíte `LegacyForgeCompanionTests` aprovou 100% dos testes:
- `testModMetadataAndLoader`: Validacao de id `companions`, versao `0.1.0-1.12.2` e loader `forge`.
- `testNetworkSerializationIMessage`: Serializacao e desserializacao binaria de pacotes de comando e feedback.
- `testLegacyPermissionAndClaims`: Interceptacao de acoes em areas protegidas.
- `testLegacyCompanionEntityDialogueAndExecution`: Resposta deterministica instantanea (0ms) e integracao generativa com mock SLM.
- `testLegacyQuestServiceReloadAndCaching`: Selecao de quests e resiliencia ao recarregamento.

---

## 5. Conclusao e Proximos Passos

- **Status de P8**: **Concluido e Homologado**.
- O mod dispoe agora de todas as tres plataformas arquiteturais planejadas:
  - **Forge 1.20.1** (`companions-0.1.0.jar`)
  - **NeoForge 1.21.1** (`companions-neoforge-1.21.1-0.1.0-1.21.1.jar`)
  - **Forge 1.12.2** (`companions-forge-1.12.2-0.1.0-1.12.2.jar`)
- **Proximo Passo**: **Etapa P9 — Distribuicao e preparacao final dos artefatos de release**.
