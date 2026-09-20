---
tags: [minecraft, minefriends, r1, rede, snapshot, persistencia]
updated: 2026-09-19
status: parcialmente-concluido-r1-1-pendente
---

# R1 — Protocolo, snapshot e persistência

## Resultado esperado

Cliente e servidor trocam intenções e estados tipados, limitados e versionados. O servidor é autoridade; reinícios e trocas de dimensão preservam somente o estado definido, sem duplicação nem vazamento entre jogadores.

## Estado atual

O commit `e9850e6` criou `CompanionSnapshot`, payloads com `StreamCodec`, GUI alimentada por snapshot e `CompanionSavedData` schema 1. A base compila e passa nos testes, mas o comando ainda aceita texto amplo, snapshots não têm revisão e a persistência cobre metadados, não todo o estado operacional.

## Entregas

### R1.1 — protocolo v2

- Criar `CompanionCommandRequest` com `requestId`, `companionId`, `expectedRevision`, `IntentType` e parâmetros tipados.
- Criar `CompanionSnapshotV2` com `revision`, sessão, tarefa, fila, bloqueio, distância/dimensão e capacidades disponíveis.
- Criar `FeedbackResult` com sucesso, motivo, chave traduzível e `ActionResult` opcional.
- Definir limites de strings, listas, quantidade e frequência por jogador.
- Remover execução de strings `/...` do `CommandPayload`.

### R1.2 — autoridade e ciclo de vida

- Validar dono, mundo, NPC ativo, revisão, cooldown e capacidade antes de aceitar intenção.
- Deduplicar `requestId`; rejeitar replay e revisão antiga.
- Limpar snapshots no logout, desconexão, troca de servidor e unload do mundo.
- Enviar snapshot por mudança relevante ou polling limitado, nunca por tick.

### R1.3 — repositório persistente

- Criar `CompanionRepository` sobre `SavedData` global do overworld.
- Definir fonte única para identidade, skin, modo, baú, tarefa, reservas e inventário.
- Manter inventário no formato nativo do player quando seguro; não duplicá-lo no `SavedData`.
- Implementar migração schema 1→2, preservação de schema futuro e erro recuperável.
- Separar UUID lógico estável do UUID/perfil usado para atualizar skin.

## Ordem de implementação

1. Congelar protocolo 1.0 e adicionar DTOs v2 no core.
2. Implementar codecs e validações sem remover v1 durante teste.
3. Migrar GUI e comandos para intenção tipada.
4. Implementar revisão e deduplicação.
5. Migrar SavedData e ciclo de vida.
6. Remover v1 antes do release 0.2.

## Testes obrigatórios

- pacote truncado, enum inválido, string excessiva e quantidade negativa;
- jogador tentando controlar NPC alheio;
- replay, snapshot fora de ordem e resposta após logout;
- dois jogadores e dois idiomas simultâneos;
- stop/start, mudança de dimensão, morte e skin durante tarefa;
- schema 1, schema 2 e schema futuro;
- servidor dedicado sem classes de cliente.

## Evidência para conclusão

Captura de ciclo C2S/S2C, GameTest ou harness de servidor, save descartável antes/depois, log de migração e demonstração em cliente + servidor dedicado.

## Critério de aceite

Nenhum payload executa comandos gerais; cliente não inventa estado; snapshots antigos são descartados; identidade lógica permanece estável; stop/start restaura o contrato declarado sem duplicar inventário.

## Dependências e saída

Consome `ActionResult` de R0. Entrega request/revision e feedback necessários para R2 e R4.

## Progresso executado em 19/09/2026

- `CompanionSnapshot` agora carrega uma `revision` monotônica, mantendo o construtor legado com revisão `0`.
- `RequestSnapshotPayload` gera e transporta `requestId`; `SnapshotPayload` ecoa o identificador e serializa a revisão.
- `CommandPayload` aceita `requestId` e `revision`; o servidor rejeita revisão diferente da versão atual do companheiro.
- `CompanionScreen` descarta snapshots com revisão inferior à já exibida.
- O `CompanionManager` incrementa revisão em spawn, dismiss, skin e baú designado.
- O servidor mantém uma janela limitada de 32 `requestId` por dono e rejeita replay; o histórico é limpo ao remover o companheiro ou limpar o manager.
- A GUI limpa snapshot, feedback e revisão ao evento de logout do cliente, impedindo vazamento visual entre servidores/sessões.
- `CompanionCommandRequest` foi adicionado ao core; payloads NeoForge mapeiam somente intenções conhecidas e rejeitam `/kill`, texto desconhecido e parâmetros fora dos limites.
- A GUI NeoForge envia por payload tipado modos, ações e conversa quando possui snapshot válido; spawn/skin e outros comandos administrativos continuam no caminho vanilla até terem intents próprias.
- Teste NeoForge confirma round-trip de requestId e revisão; as suítes core, NeoForge, Forge 1.20.1 e Forge 1.12.2 passam em builds limpos sequenciais.

Ainda pendente: migrar interfaces das outras plataformas, deduplicar respostas de inferência e persistir tarefa/reservas operacionais.
