---
tags: [minecraft, port, neoforge, 1.21.1, java21, tensura, p7]
updated: 2026-09-19
status: p7-concluido-aprovado
---

# Relatorio de Port e Homologacao P7: NeoForge 21.1.248 e Forge 1.21.1

Autor: ThyagoToledo
Escopo: Etapa P7 do Roadmap (minecraft-companheiros-plano.md)
Data da medicao: 19/09/2026

---

## 1. Contexto e Ambientes de Ensaio

A Etapa P7 realizou a expansao de plataforma do mod MineFriends / Companions para a versao Minecraft 1.21.1 em Java 21, priorizando o loader **NeoForge 21.1.248** presente nas instalacoes locais:
- `Tensura All The Slime 1` (NeoForge 21.1.248 com suite FTB NeoForge, Tensura e OPAC)
- `Tempest Protocol` (NeoForge 21.1.248)

A plataforma `platforms/neoforge-1.21.1` foi construida desacoplada do core compartilhado `:core` (Java 8), garantindo que nenhuma dependencia de loader ou versao vaze para as outras plataformas.

---

## 2. Matriz Tecnica e Comparativo entre Loaders (1.21.1)

| Caracteristica | NeoForge 21.1.248 (Alvo Principal) | Forge 1.21.1 (Instancia Limpa) |
| :--- | :--- | :--- |
| **Status Local** | Presente em 2 modpacks locais reais | Nenhuma instancia instalada |
| **Rede** | `CustomPacketPayload` (Canais de Comando e Feedback) | `CustomPacketPayload` |
| **Estrutura de Claims** | Suporte composto: FTB Chunks NeoForge + Tensura OPAC | Protecao vanilla / Fallback em memoria |
| **Assistencia a Quests** | FTB Quests NeoForge (`ftbquests-neoforge-2101.x`) | Fallback em memoria |
| **Tensura / Curios** | Integracao com habilidades e reinos de Tensura | N/A (mods exclusivos de NeoForge na 1.21.1) |
| **Artefato de Saida** | `companions-neoforge-1.21.1-0.1.0-1.21.1.jar` | Compilacao independente sob demanda |

---

## 3. Entregas da Plataforma `platforms/neoforge-1.21.1`

1. **Toolchain Java 21 e Gradle**:
   - `build.gradle` configurado com `JavaLanguageVersion.of(21)` e inclusao composta do modulo `:core`.
   - `neoforge.mods.toml` configurado para compatibilidade com NeoForge 21.1.248+ e Minecraft 1.21.1.
2. **Adaptadores de Rede (`NeoForgeCompanionPayloads`)**:
   - Implementacao de pacotes de comando (`companions:command_payload`) e feedback (`companions:feedback_payload`) conforme o modelo `CustomPacketPayload` obrigatorio em 1.21.1.
3. **Servico de Permissoes Composto (`NeoForgePermissionService`)**:
   - Compativel com verificacao de claims de faccao (Tensura OPAC) e protecao de blocos do FTB Chunks NeoForge.
   - Bloqueio imediato de colheita/mineracao em areas protegidas com seguranca contra nulos.
4. **Servico de Quests (`NeoForgeQuestService`)**:
   - Integracao com a suite FTB Quests NeoForge e fallback resiliente em memoria.
5. **Entidade Companheira (`NeoForgeCompanionEntity`)**:
   - Suporte completo aos comandos bilingues (pt-BR e en-US), integracao com `HybridDialogueProvider`, `ConversationMemory` e bounded execution em ticks de servidor.

---

## 4. Resultados dos Testes Automatizados

A suíte `NeoForgeCompanionTests` validou 100% dos cenarios planejados:
- `testModMetadataAndLoader`: Metadados `companions`, versao `0.1.0-1.21.1` e loader `neoforge`.
- `testPayloadContracts1211`: Serializacao, canais e integridade de payloads de comando e feedback.
- `testTensuraAndFtbProtectionOnNeoForge`: Bloqueio rigoroso de acoes em claims de terceiros (OPAC / FTB Chunks).
- `testNeoForgeCompanionEntityDialogueAndExecution`: Despacho de ordens deterministicas e dialogos generativos assincronos com modelo local.
- `testNeoForgeReloadAndQuestCache`: Resiliencia a comandos `/reload` com invalidacao atômica de cache.

---

## 5. Conclusao e Proximos Passos

- **Status de P7**: **Concluido e Aprovado**.
- O mod dispoe agora de duas plataformas funcionais homologadas:
  - **Forge 1.20.1** (Cozy Zen 47.4.10 e Society 47.4.0)
  - **NeoForge 1.21.1** (Tempest Protocol e Tensura All The Slime 1)
- **Proximo Passo**: **Etapa P8 — Port Forge 1.12.2 com escopo explicito**.
