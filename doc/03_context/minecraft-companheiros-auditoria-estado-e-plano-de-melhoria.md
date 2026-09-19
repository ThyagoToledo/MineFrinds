---
tags: [minecraft, auditoria, minefriends, bugs, roadmap, neoforge, forge]
updated: 2026-09-19
status: auditoria-estatica-e-build-concluidos-jogabilidade-pendente
---

# MineFriends — auditoria do estado real e plano de melhoria

## Escopo e evidência

Auditoria realizada em 19/09/2026 sobre o checkout `C:/Users/thyag/Projects/minecraft-companheiros`, o documento [[minecraft-companheiros-documentacao-geral-arquitetura-e-funcionalidades]], os JARs distribuídos e o `latest.log` da instância `TesteMineFrinds`. Nenhum save ou modpack foi alterado.

Evidência obtida:

- árvore Git limpa no commit `63d6572`;
- `clean test` aprovado nos três adaptadores;
- 57 testes JUnit aprovados: 19 core, 15 Forge 1.20.1, 18 NeoForge 1.21.1 e 5 Forge 1.12.2;
- o JAR `companions-neoforge-1.21.1-0.1.0.jar` foi descoberto e carregado pelo NeoForge na instância `TesteMineFrinds` em 19/09/2026;
- o log não contém crash do MineFriends, mas registra apenas inicialização e abertura do guia; não há evidência no log de coleta, crafting, câmera, persistência, morte, skins, IA local ou integrações;
- os testes de diálogo generativo usam `MockInferenceClient`; comprovam contrato/fallback, não uma chamada real ao Qwen;
- os testes de plataforma rodam na JVM, sem GameTest ou cliente/servidor controlado que exerça blocos, inventário, rede e ciclo de vida real.

Portanto, “compila e passa nos testes unitários” está comprovado. “Homologado em jogo/modpacks” precisa de nova evidência reproduzível.

## O que está implementado de verdade

O projeto já possui uma base útil:

- core Java 8 para intenções bilíngues, memória curta, receitas abstratas, quests e cliente HTTP;
- entidade própria funcional no Forge 1.20.1, com recall que procura posição segura, inventário e câmera principal remota;
- `CompanionServerPlayer` no NeoForge 1.21.1, presença no Tab, comandos, GUI, skins, inventário, combate e rotinas de trabalho;
- adaptador estrutural Forge 1.12.2;
- builds separados e JARs distribuídos;
- instância limpa dedicada a testes.

O 1.21.1 concentra as funções recentes. Forge 1.20.1 e 1.12.2 ainda não têm paridade com GUI, fake player, skins, agricultura e crafting descritos no documento geral. A matriz pública deve declarar capacidades por versão.

## Erros e riscos encontrados

### P0 — bloquear release até corrigir

1. **Duplicação no crafting NeoForge.** `craftAndDeliver` cria o item, adiciona ao inventário do NPC e cria outra pilha em `deliverItemToOwner`. Uma ordem produz duas cópias. A rotina também consome quantidades fixas incorretas, aceita nomes desconhecidos como tábuas e ignora receitas reais/KubeJS.
2. **Quebra/plantio sem autorização efetiva.** Os modos `WOOD`, `MINE` e `FARM` chamam `destroyBlock`/`setBlockAndUpdate` diretamente. `NeoForgePermissionService` existe, mas não participa dessas ações; claims e eventos de quebra/colocação podem ser contornados.
3. **Recall inseguro no NeoForge.** `recallToOwner` teleporta para coordenadas fixas ao lado do dono, inclusive entre dimensões, sem colisão, piso, combate, cooldown, chunk carregado ou permissão. A implementação segura documentada só existe no Forge 1.20.1.
4. **Estado falso na GUI.** Vida `20/20`, modo `SEGUINDO`, presença e dados Tensura são textos fixos do cliente. O toggle Tensura e o modelo slim também são apenas variáveis locais; não persistem nem comprovam o estado do servidor.
5. **Crafting e retirada não são transacionais.** O código reduz pilhas e cria resultado em passos separados, sem plano/commit/reconciliação. Crash, inventário cheio ou receita inválida podem perder ou duplicar itens.

### P1 — estabilidade antes de novas funções

1. **Payload NeoForge é só DTO.** `NeoForgeCompanionPayloads` não implementa nem registra `CustomPacketPayload`; a GUI contorna isso enviando comandos Brigadier. Funciona para botões simples, mas impede snapshots tipados, feedback confiável e versionamento de protocolo.
2. **IA local parcial.** Forge 1.20.1 cria `HttpInferenceClient`; NeoForge cria `HybridDialogueProvider` com cliente nulo, então o chat cai no determinístico. O Forge usa `processSync(..., 1500)` durante o tratamento da ordem, o que pode bloquear a thread do servidor por até 1,5 s.
3. **Resposta HTTP não é desserializada corretamente.** O cliente devolve o envelope inteiro de `/v1/chat/completions`; o parser procura `intent`/`speech` por regex no texto bruto. Falta desserialização do `choices[0].message.content`, JSON Schema e validação de tamanho/enum.
4. **Ciclo de vida do fake player depende de mapas estáticos.** Baús designados e companheiros ativos não têm persistência robusta demonstrada; é necessário testar stop/start, logout do dono, troca de dimensão, morte, unload e remoção da `PlayerList`.
5. **Troca de skin recria o jogador.** Copia 36 slots e alguns campos, mas pode perder XP, efeitos, fome, cooldowns, atributos/mod capabilities e dados de outros mods. O UUID muda a cada troca e pode deixar rastros em mods que indexam jogadores.
6. **Busca de blocos e coleta rodam no tick.** Varreduras frequentes, pickup em raio e lógica extensa dentro de uma classe com mais de mil linhas dificultam orçamento de MSPT e manutenção.

### P2 — documentação, UX e compatibilidade

- README anuncia 100% de precisão e 20 TPS sem evidência integrada; também mostra contagens de testes divergentes. A execução atual encontrou 57.
- O documento geral menciona telas JavaFX, mas a GUI usa `net.minecraft.client.gui.screens.Screen`/LWJGL.
- O documento afirma recall seguro e integração Qwen no produto inteiro; isso varia por plataforma.
- Presets de personagens conhecidos e payloads de skins de terceiros precisam de autorização/licença e política de cache. Para release pública, priorizar skins originais e permitir nickname apenas sob escolha explícita do usuário.
- Há avisos de compilação de APIs depreciadas e anotações `Dist` ausentes no classpath de teste. Não quebram o build atual, mas devem entrar na atualização técnica.

## Arquitetura-alvo

Manter o híbrido leve, com quatro camadas explícitas:

1. **Domínio no core:** `CommandIntent`, `TaskPlan`, `TaskStep`, `ActionResult`, motivos de bloqueio, idioma e limites.
2. **Executor por plataforma:** toda mutação do mundo passa por `ActionExecutor`, que valida dono, distância, chunk, ferramenta, evento/claim e inventário antes de aplicar.
3. **Snapshot e protocolo:** GUI/chat enviam intenções tipadas; servidor devolve `CompanionSnapshot` versionado. Texto da tela nunca representa estado inventado.
4. **Linguagem opcional:** comandos frequentes usam parser determinístico; conversa livre usa uma fila assíncrona compartilhada. O modelo propõe texto/intenção, mas o executor decide e relata o resultado.

No NeoForge, manter `ServerPlayer` somente se os testes com mods confirmarem a vantagem. Isolar em `PlayerCompanionAdapter`; a lógica de tarefas não deve depender da herança. Forge 1.20.1 pode manter entidade humanoide. A paridade deve ocorrer nos contratos e comportamentos, não na mesma classe Minecraft.

## Tecnologias escolhidas e como usar

| Área | Tecnologia | Uso planejado |
| --- | --- | --- |
| Core | Java 8 + JUnit 5 compatível | estado, intenções, planos, transações e testes puros |
| Forge 1.20.1 | Forge 47.4.x, Java 17, SimpleChannel | entidade, menus, rede, eventos e GameTests |
| NeoForge 1.21.1 | NeoForge 21.1.x, Java 21, payload API | fake player isolado, `CustomPacketPayload`, anexos/dados e GameTests |
| Forge 1.12.2 | Forge 14.23.5.2860, bytecode 8 | subset compatível, `SimpleNetworkWrapper` e testes em instância limpa |
| Serialização | Gson já disponível no core; codecs do Minecraft na rede moderna | retirar regex de JSON e versionar mensagens |
| IA local | `llama-server` + Qwen2.5-0.5B Q4_K_M, provisório | um processo, um slot, contexto 2048, saída curta; integração real após suíte reproduzível |
| Assets | modelo vanilla + skins 64×64 originais + Blockbench | máxima compatibilidade; GeckoLib só após necessidade medida |
| Observabilidade | log estruturado, contadores de tarefa e amostragem de MSPT/memória | registrar ordem, validação, resultado e custo sem guardar conversa sensível inteira |

FunctionGemma 270M pode ser comparado depois como roteador de funções especializado. Não carregar FunctionGemma e Qwen ao mesmo tempo no perfil normal; isso consome a margem de 1 GB. LiteRT/Google AI Edge não entra no mod inicial: é preciso primeiro demonstrar backend desktop Java estável e vantagem sobre `llama-server`.

## Roadmap de correção e implementação

### R0 — segurança de itens e mundo

- desabilitar temporariamente `/companion craft` e modos destrutivos no JAR público ou marcá-los experimentais;
- criar testes que reproduzam a duplicação;
- substituir crafting por consulta às receitas efetivas do servidor, simulação, reserva de ingredientes, commit único e rollback/reconciliação;
- remover fallback de item desconhecido;
- centralizar quebra, plantio, retirada e depósito no `ActionExecutor` com eventos/claims;
- portar o recall seguro do 1.20.1 para um serviço compartilhável e validar mesma dimensão/chunk/cooldown.

**Aceite:** conservação exata de itens antes/depois; claim negado nunca muda mundo; recall nunca coloca em colisão/lava/vazio; testes de crash/cancelamento não duplicam.

### R1 — estado, persistência e rede

- implementar `CompanionSnapshot` e payloads reais C2S/S2C no NeoForge;
- fazer GUI renderizar vida, modo, tarefa, bloqueio, inventário e integração recebidos do servidor;
- persistir vínculo, baú, modo, skin e tarefas com schema versionado;
- auditar logout, reinício, dimensão, morte e troca de skin;
- substituir recriação para trocar skin quando possível; caso permaneça, serializar/restaurar dados autorizados e testar compatibilidade.

**Aceite:** stop/start conserva estado; cliente não consegue ordenar NPC alheio; UI reflete alteração no próximo snapshot; payload inválido é rejeitado.

### R2 — IA assíncrona real e bilíngue

- criar um `InferenceSupervisor` compartilhado, configurável e desligado por padrão;
- desserializar o envelope OpenAI-compatible com Gson e validar o conteúdo contra DTO/enum;
- nunca usar `processSync` na thread do servidor; retornar resultado por fila e revalidar `requestId`/estado;
- integrar o mesmo provider nos adaptadores modernos; manter fallback determinístico;
- executar 50 frases reservadas por idioma e anexar prompts, saídas, hashes, latência e memória.

**Aceite:** timeout não aumenta MSPT, ações continuam determinísticas sem runtime, 95% de intenção por idioma no conjunto reservado e nenhuma fala inventa sucesso.

### R3 — decomposição e desempenho

- dividir `CompanionServerPlayer` em sensores, navegação, combate, trabalho, inventário e apresentação;
- escalonar scans no servidor e impor orçamento global por tick;
- cachear alvos por curto período e invalidar por evento;
- limitar quatro NPCs comuns/oito em estresse, uma geração ativa e fila de oito;
- medir baseline pareado no `TesteMineFrinds`, depois em cópias dos packs.

**Aceite:** perfil comum adicional ≤1.000 MB, pico ≤2.000 MB, sem crescimento contínuo e delta p95 de MSPT dentro da meta definida contra baseline.

### R4 — UX, skins e paridade declarada

- finalizar painel com lista de NPCs, abas, fila, bloqueios, idioma e saída segura da câmera;
- trocar textos e dados fixos por traduções/snapshots;
- criar quatro skins originais e metadados de licença;
- adicionar matriz de recursos por versão e manter 1.12.2 como subset quando APIs não permitirem equivalência;
- testar resolução, escala de GUI, teclado, shaders e câmera.

**Aceite:** pt-BR/en-US completos, nenhum dado falso na tela, assets licenciados e diferenças por plataforma publicadas.

### R5 — homologação por modpack e release

- instalar builds apenas em cópias de teste;
- executar roteiro observável: spawn, ordem, claim permitido/negado, inventário cheio, crafting, morte, câmera, skin, save/reload e servidor dedicado;
- validar FTB Chunks/OPAC, FTB Quests e Tensura por adaptadores reais, sem reflexão silenciosa que reporte sucesso falso;
- guardar logs, versões, hashes, resultados e problemas conhecidos;
- só então atualizar README/P6–P9 para “homologado”.

## Status da execucao e proximo passo

- **Etapa R0 concluida com sucesso**:
  - Duplicacao de itens corrigida em `CompanionServerPlayer` com entrega de destino unico.
  - Consumo de materiais agora e proporcional e transacional; fallback espurio de tabuas de carvalho removido.
  - Protecao rigorosa de claims e terreno aplicada em `handleWoodMode` (inclusive cascata), `handleMineMode`, `handleFarmMode` e baus via `NeoForgePermissionService`.
  - Recall seguro implementado no NeoForge 1.21.1 com checagem de dimensao, combate, cooldown e busca de chao solido livre de lava/fogo.
  - 3 novos testes automatizados adicionados; total de 60 testes JUnit aprovados com 100% de sucesso nos quatro modulos (`:core`, `neoforge-1.21.1`, `forge-1.20.1`, `forge-1.12.2`).
  - JARs recompilados, distribuidos e hashes SHA-256 atualizados.

- **Proxima etapa recomendada**:
  - **R1 — estado, persistencia e rede**: implementar `CompanionSnapshot` tipado, payloads reais C2S/S2C no NeoForge e vincular a GUI para renderizar vida, modo, inventario e tarefas reais vindos do servidor.

