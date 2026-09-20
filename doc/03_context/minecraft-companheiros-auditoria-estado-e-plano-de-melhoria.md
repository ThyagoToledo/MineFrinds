---
tags: [minecraft, auditoria, minefriends, bugs, roadmap, neoforge, forge]
updated: 2026-09-19
status: auditoria-estatica-e-build-concluidos-jogabilidade-pendente
---

# MineFriends — auditoria do estado real e plano de melhoria

## Escopo e evidência

Captura inicial da auditoria em 19/09/2026 sobre o checkout `C:/Users/thyag/Projects/minecraft-companheiros`, o documento [[minecraft-companheiros-documentacao-geral-arquitetura-e-funcionalidades]], os JARs distribuídos e o `latest.log` da instância `TesteMineFrinds`. Os incrementos posteriores estão registrados ao final deste documento; a fotografia inicial era somente leitura; instalações posteriores na instância de teste estão registradas nos incrementos abaixo.

Evidência obtida:

- árvore Git limpa no commit `63d6572`;
- `clean test` aprovado nos três adaptadores;
- na captura inicial, 57 testes JUnit aprovados: 19 core, 15 Forge 1.20.1, 18 NeoForge 1.21.1 e 5 Forge 1.12.2; o ciclo final desta revisão chegou a 74;
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

- **Etapa R1 concluida com sucesso**:
  - DTO canonico `CompanionSnapshot` criado no modulo `:core`, desacoplado do Minecraft e 100% testavel em Java puro.
  - Rede tipada oficial implementada via `CustomPacketPayload` (`RequestSnapshotPayload`, `SnapshotPayload`, `CommandPayload`, `FeedbackPayload`) e registrada no `RegisterPayloadHandlersEvent` com `StreamCodec`.
  - Persistencia nativa implementada com `CompanionSavedData extends SavedData` no NeoForge 1.21.1, garantindo persistencia de baus designados e dados de companheiros com schema versionado (`schema_version = 1`).
  - Preservacao integral de equipamentos: `updateCompanionSkin` e `spawnPlayerCompanion` salvam e restauram os 6 slots de equipamento (elmo, peitoral, calcas, botas, mao secundaria e mao principal), inventario e niveis de experiencia.
  - Interface grafica `CompanionScreen` vinculada aos dados reais do snapshot de servidor, eliminando completamente strings fixas/falsas na GUI.
  - 4 novos testes unitarios e de integracao adicionados; naquele marco eram 64 testes JUnit aprovados (`:core`: 20, `neoforge-1.21.1`: 24, `forge-1.20.1`: 15, `forge-1.12.2`: 5).
  - JARs recompilados, distribuidos e hashes SHA-256 atualizados em `LEIAME-E-HASHES-SHA256.md`.

- **Proxima etapa recomendada**:
  - **R2 — IA assincrona real e bilingue**: implementar `InferenceSupervisor` desacoplado, validacao rigorosa de envelopes JSON OpenAI-compatible sem chamadas bloqueantes na thread principal do servidor (`processSync`), com conjunto de avaliacao bilingue de intencoes (pt-BR e en-US).

## Revisão do plano detalhado R2

Revisão em 19/09/2026 do `implementation_plan.md` criado para `InferenceSupervisor`. Direção aprovada, execução condicionada aos ajustes abaixo:

1. `InferenceSupervisor` é serviço único por servidor, criado no startup e encerrado no server stopping. Entidades não criam pools, clientes HTTP ou circuit breakers próprios.
2. Separar `InferenceTransport` de supervisão. O transporte faz uma requisição; o supervisor possui fila limitada, uma geração ativa, timeout lógico, circuit breaker e métricas. Substituir ou simplificar `HttpInferenceClient` para não manter uma segunda fila/thread.
3. Remover `processSync` de todos os caminhos de produção no Forge 1.20.1 e NeoForge 1.21.1. No 1.12.2, manter IA generativa desabilitada até existir despacho seguro para a thread correta. Apenas depreciar o método não elimina o travamento atual.
4. Toda solicitação carrega `requestId`, UUID do NPC/dono, revisão de estado, idioma e deadline. Ao concluir, o adaptador agenda na thread do servidor, verifica mundo/dono/NPC/revisão e descarta resposta obsoleta. O modelo nunca altera modo diretamente; propõe intenção que passa pelo mesmo validador das ordens determinísticas.
5. `OpenAiResponseParser` aceita envelope OpenAI-compatible e conteúdo JSON direto, limita corpo HTTP e fala, rejeita intent desconhecida e preserva Unicode pt-BR. Remover cercas Markdown é tolerância de entrada; não usar regex para interpretar o JSON. Proibição de emoji é preferência configurável, não requisito de segurança.
6. Configuração padrão: `enabled=false`, endpoint loopback, um slot, fila 8, contexto 2048, saída 160, timeout a medir. Endpoint remoto e credenciais são opt-in; segredos não entram em logs, saves ou pacotes ao cliente.
7. Circuit breaker usa relógio injetável nos testes, estados fechado/aberto/meio-aberto e uma tentativa controlada de recuperação. Uma falha não deixa `isAvailable=false` permanentemente.
8. Separar testes rápidos de avaliação real. Unitários usam transporte fake e verificam fila, timeout, cancelamento, parsing e transições. As 100 frases ficam em fixture versionada com conjunto de desenvolvimento separado. O ensaio com Qwen é tarefa opt-in, guarda saídas/hashes e não roda como teste comum.
9. Meta de intenção é >=95% em cada idioma no conjunto reservado. “100%” não é critério obrigatório. Latência determinística é benchmark informativo; não usar `<1 ms` como assert de CI por ser dependente da máquina.
10. Acrescentar testes de shutdown/reload, resposta depois de dismiss/morte/troca de dimensão, fila cheia, corpo HTTP excessivo, status não-200, JSON truncado, fala vazia e dois jogadores em idiomas diferentes.

Ordem recomendada: o núcleo do R2 pode ser implementado agora, sem ativar ações do modelo. A integração de intenções com ações depende das garantias de R0; snapshots e feedback usam a fundação concluída em R1. O marco R2 só termina após regressão nas três plataformas, teste opt-in com Qwen e perfil integrado de memória/MSPT.

## Reauditoria posterior aos commits R0 e R1

O roadmap detalhado [[../01_plan/minecraft-companheiros-roadmap-r0-r5-revisado]] substitui o uso de “concluído” sem qualificação. R0 e R1 ficam classificados como **parcialmente concluídos**, com R0.1 e R1.1 obrigatórios. As principais razões são: permissões NeoForge ainda não integram claims reais; crafting ainda usa fórmulas manuais; e `SavedData` cobre metadados, não todo o estado operacional. Os 74 testes atuais passam, mas continuam insuficientes para homologação em jogo.

## Implementação executada após a reauditoria — 19/09/2026

Foram aplicadas correções incrementais de R0/R2 mantendo o estado parcial documentado:

- NeoForge agora publica `BreakEvent` e `EntityPlaceEvent` antes de ações do fake player, respeitando cancelamento de outros mods.
- Payload de comando NeoForge limita UTF-8, valida UUID do companheiro contra o snapshot do dono e só aceita a raiz `/companion`; comandos arbitrários não são encaminhados.
- `InferenceSupervisor` passou a centralizar fila limitada, timeout, circuit breaker e shutdown. Forge 1.20.1 e NeoForge 1.21.1 compartilham um supervisor por servidor, desativado por padrão e encerrado no evento de parada.
- Forge 1.20.1 deixou de esperar `processSync` no caminho de comando: a inferência é assíncrona e os efeitos são reaplicados na thread do servidor. NeoForge usa `handleCommandAsync` para o mesmo retorno seguro.
- O parser OpenAI-compatible foi mantido sem Gson; o runtime Forge 1.12.2 não garante essa biblioteca, e o parser anterior fazia fallback silencioso para `UNKNOWN_OR_BLOCKED`.
- Novos testes cobrem envelope, JSON direto, fallback, limite de fala, supervisor desativado e sucesso do transporte fake.

Validação limpa e sequencial em 19/09/2026: Forge 1.12.2, Forge 1.20.1 e NeoForge 1.21.1 passaram `clean test`. Isso comprova compilação e contratos unitários; ainda não comprova claims reais, GameTests, reinício de servidor, modpacks instalados ou limite de RAM de 1/2 GB. R2 continua parcial porque requestId/revisão e descarte de resposta obsoleta ainda não cobrem o caminho de inferência completo, além de faltar avaliação opt-in do modelo local.

## Incremento R1.1 — revisão e requestId — 19/09/2026

O núcleo e o adaptador NeoForge agora carregam revisão monotônica do estado e identificador de requisição nos snapshots. O cliente descarta uma resposta com revisão menor que a já apresentada; o servidor compara a revisão de um `CommandPayload` com o snapshot atual e rejeita comandos obsoletos. O `CompanionManager` atualiza a revisão em spawn, dismiss, skin e baú designado. O round-trip foi coberto por teste NeoForge e as quatro execuções `clean test` continuam aprovadas.

O mesmo caminho agora mantém uma janela de 32 `requestId` por dono para rejeitar replay, isolando o histórico entre jogadores e limpando-o no ciclo de remoção.

O cliente NeoForge também limpa snapshot, feedback e revisão no evento de logout, evitando reutilizar estado visual de uma sessão anterior.

O protocolo recebeu `CompanionCommandRequest` no core. O payload NeoForge converte comandos recebidos para uma enumeração de intenções conhecida antes de encaminhar ao dispatcher; entradas como `/kill @e` ou texto não reconhecido são rejeitadas. A migração das telas para enviar o DTO diretamente ainda está pendente.

A tela NeoForge agora usa esse payload para modos, ações e conversa quando há snapshot válido; spawn, skin e comandos administrativos continuam no fallback vanilla até receberem intents próprias.

Essa entrega fecha apenas a parte de ordenação do R1.1. Ainda faltam intents tipadas sem texto de comando, limpeza de sessão no logout/troca de servidor e persistência das tarefas/reservas de operação.

## Incremento R3 — cadência de coleta — 19/09/2026

`CompanionServerPlayer` não pesquisa mais drops do chão em todos os ticks. A coleta automática passou a rodar a cada cinco ticks (4 Hz), mantendo o comportamento e reduzindo o custo de busca de entidades por NPC. A fila de inferência já é compartilhada e limitada por servidor. Isso inicia R3, mas não substitui o profiling de MSPT/heap nem a extração dos controladores planejados.

## Incremento R0/R2/R3/R5 — ciclo final desta revisão — 19/09/2026

- O crafting NeoForge agora calcula `remainingCount` depois de retirar itens prontos da bolsa ou do baú. A fabricação pendente e a entrega produzem somente o saldo solicitado; isso elimina o excesso causado por retirada parcial.
- O core recebeu `CraftTransaction`, com plano imutável e commit atômico para consumo/saída. Quatro testes cobrem sucesso, entrada insuficiente, saída cheia e requisito inválido sem mutação parcial. A integração com `RecipeManager` e itens restantes ainda é pendência R0.2.
- Conversa livre enviada pelo payload NeoForge não passa mais pelo dispatcher textual: o request validado chega ao provider assíncrono e a resposta é descartada se a revisão do snapshot mudou antes da aplicação. Modos e ações permanecem no dispatcher apenas durante a migração das intents.
- Varreduras de árvore/minério/fazenda sem alvo têm intervalo de 20 ticks; busca de monstros sem alvo prioritário ocorre a cada 10 ticks. O supervisor passou a expor contadores de requisições, sucesso, falha e rejeição.
- Foi adicionado `tools/collect-r5-instance-inventory.ps1`, executado contra `C:/Users/thyag/curseforge/minecraft/Instances`. O relatório registrou seis instâncias e está em `doc/03_context/minecraft-companheiros-r5-inventario-instances.md`.
- Validação JVM sequencial deste ciclo: core 29, Forge 1.20.1 15, NeoForge 1.21.1 25 e Forge 1.12.2 5; total de 74 testes aprovados. Isso não substitui a execução de cliente/servidor e medições de RAM/MSPT nos saves descartáveis.

O estado correto permanece: R0, R1, R2 e R3 avançaram, mas continuam parciais; R4 depende de assets/UX finais; R5 está preparado com inventário automatizado, porém sem homologação funcional declarada.

## Correção de coleta de drops — 20/09/2026

O fake player não dependia mais somente do raio imediato do próprio corpo. A implementação agora mantém uma `pendingDropCollectionBox` por 100 ticks após cada quebra de madeira, minério ou safra, revisitando a área a cada cinco ticks. A caixa é unida durante o corte em cascata, então os drops de troncos altos também entram na coleta depois do pickup delay normal. O código contabiliza a diferença da pilha antes/depois de `Inventory.add`, preservando a parte que não couber quando o inventário estiver cheio.

A suíte NeoForge passou após a alteração e um novo candidato foi gerado. O JAR instalado em `TesteMineFrinds` agora tem SHA-256 `6CE2D1F8649E82081B7DF185DD2E402CE692E8FB6E44D1655F7CC971A6CA0744`.


## Jogabilidade, IA local e controle remoto — 20/09/2026 — alpha.2

Escopo desta entrega: implementação no NeoForge 1.21.1 (`TesteMineFrinds`), com regressão do núcleo compartilhado. Não representa paridade de jogabilidade com Forge 1.20.1/1.12.2 nem encerramento dos R0–R5.

### Causas encontradas e correções

- A inferência estava desabilitada por padrão, sem configuração por instância. Agora `InferenceSettings` lê `config/companions-ai.properties` através de `FMLPaths.CONFIGDIR`; o supervisor é recriado ao iniciar cada servidor. Comandos continuam funcionando quando o endpoint cai, com aviso explícito na conversa. `/companion ai` mostra disponibilidade lógica e contadores HTTP, sem fingir que houve health check.
- Memória de conversa era gravada, mas não enviada ao modelo. O prompt inclui as entradas recentes, idioma do cliente, modo e até 12 entradas do inventário atualizado. HTTP solicita JSON com `CASUAL_CHAT` e desliga thinking. O modelo conversa; ações de mundo continuam sob a política determinística.
- Os reconhecedores confundiam palavras no meio de frases: “para sobreviver” virava STAY e “mina” podia capturar texto de conversa. Ordens motoras foram delimitadas. Foram preservadas formas educadas simples.
- Seguir/parar deixaram de encarar continuamente o dono: olhar ocioso alterna entre ambiente e dono, com rotação gradual. Navegação local usa busca terrestre limitada a 256 expansões e raio de 12 blocos, sem carregar chunks. Não é navegação completa de sobrevivência.
- `/companion mode auto` ativa uma política inicial: madeira, bancada, picareta, coleta e tentativas de melhorar equipamento com as receitas e ingredientes disponíveis. As tarefas têm limites; não há agente irrestrito executando ações do LLM.
- Mineração busca blocos vistos por raycast dentro do cone de visão, varre o entorno antes de decidir e considera tags comuns de minério/pedra. Seleciona ferramenta pelo contrato real de drops/velocidade e usa `gameMode.destroyBlock`, durabilidade e progresso de quebra. Pode tentar descida em degraus por até seis passos, com apoio e vizinhança sem fluidos. Não cava diretamente sob os próprios pés.
- `RecipeCraftingService` usa receitas shaped/shapeless carregadas pelo `RecipeManager`, planeja em cópias, preserva sobras/recipientes e só escreve a mochila após concluir a cadeia inteira. Receitas 3×3 exigem bancada próxima. Limites: profundidade 4, orçamento 256, até 8 alternativas por ingrediente. Pedidos incompletos são tentados novamente por até 60 segundos; não retiram ingredientes remotamente de baús.
- Combate mantém alvo, respeita cooldown de ataque, prioriza hostis próximos ao dono e interrompe a tarefa. Ao acabar, a tarefa continua. Inclui `Enemy`, categoria MONSTER e mobs mirando dono/NPC; aliados e neutros não agressivos são excluídos. Ficar parado não autoriza perseguição.
- `EquipmentPolicy` usa atributos reais de dano/velocidade, armadura/toughness/knockback, condição e um pequeno desempate por encantamento. Ferramentas usam capacidade real de colher. Evita trocas por nome de material e troca itens sem descartar os anteriores. A avaliação não compreende todos os efeitos mágicos/encantamentos de mods.
- Inventário do NPC continua acessível por `/companion inventory`; corrige a duplicação visual da mão principal, bloqueia slots vazios decorativos e restringe acesso ao dono. Alimentação passa pelo uso normal do alimento, sem cura instantânea inventada.
- O NPC é forçado a survival após carregar dados. Keep inventory é próprio do NPC, sem alterar gamerule global. Mochila, armadura, offhand, slot selecionado e XP são salvos por dono no overworld (schema 2), inclusive antes da morte, ao dispensar e ao parar o servidor. `/companion spawn` restaura o checkpoint. Não há promessa de respawn automático.

### Rimuru e visão remota

A textura antiga era da conta Mojang “Rimuru”, não uma identificação confiável do personagem. O preset NeoForge agora usa um marcador próprio no GameProfile e renderer de cliente, com cache separado `rimuru_demonlord_v1.png` para não reaproveitar a imagem antiga.

Fonte da textura: [Rimuru Tempest Demon Lord, por Thetrees21](https://www.minecraftskins.com/skin/21145839/rimuru-tempest-demon-lord/). PNG 64×64, modelo clássico, inspecionado localmente. Para assegurar funcionamento offline imediato, carregamento sem atraso e proteção contra bloqueios de rede, a textura foi empacotada diretamente nos assets do mod (`assets/companions/textures/entity/rimuru_demonlord_v1.png`) com fallback instantâneo no renderer do cliente. Os demais presets por conta ainda exigem revisão visual. A substituição cobre o renderer do personagem; não foi homologada em todas as camadas visuais de outros mods.

O botão Visão Remota abre escolha entre observar e controlar. Comandos: `/companion view observe`, `/companion view control`, `/companion view exit`. Shift sai. Observar coloca o jogador em espectador e guarda posição, dimensão, rotação e modo anteriores. Ao sair, restaura o estado. Durante a sessão, o corpo do jogador acompanha a câmera pelo mecanismo vanilla; a posição original é o ponto de retorno, não um corpo físico deixado no local.

Controlar mantém o NPC em survival e transmite WASD, mira, salto, ataque, uso e seleção dos slots 1–9. Inputs têm nonce de sessão, sequência e validação de números/limites; o servidor aplica alcance, quebra e interação. HUD mostra vida e equipamento do NPC. E permite inspecionar a mochila. A sessão encerra diante de morte, remoção, troca de dimensão, logout ou timeout de controle. Um ponto de retorno fica no NBT persistente do jogador para recuperação no login após interrupção.

### Evidência e limites

- Testes JVM: core 31, Forge 1.20.1 15, NeoForge 1.21.1 33, Forge 1.12.2 5. Total esperado desta revisão: 84; conferir relatórios XML do build final antes da publicação.
- Novos testes cobrem roteamento de conversa, memória/idioma do prompt, cadeia de receitas, falta de insumos/espaço sem mutação, bancada, recipientes restantes, ciclos, atributos de equipamento, checkpoint NBT sem alias e codec/limites de controle remoto.
- Testes de receitas usam bootstrap Minecraft e receitas construídas em JVM; o recurso de idioma vazio existe **somente em test/resources**, pois o JAR de desenvolvimento não inclui assets vanilla. Não são GameTests nem validação em mundo ativo.
- Teste real do `HybridDialogueProvider` → `InferenceSupervisor` → HTTP → llama-server: Qwen2.5 0,5B falhou semanticamente e no formato; Qwen3 0,6B Q4_K_M devolveu CASUAL_CHAT em português e inglês. Última amostra: 1.220 ms / 900 ms e working set do processo de 821.567.488 bytes (~784 MiB). Uma amostra anterior foi ~591 MiB. Isso não mede pico nem memória adicional do mod dentro do jogo.
- Runtime local: `tools/start-local-ai.ps1`, contexto 2048, um slot, quatro threads CPU e zero camadas GPU. O script foi executado e respondeu health `ok`; configuração de exemplo está em `config/companions-ai.properties.example`. Não inicia automaticamente após reiniciar o computador.
- Binário e protocolo recebem versão nova: mod `0.2.0-alpha.2-1.21.1`, protocolo NeoForge `1.1.0`. Instâncias antigas precisam do mesmo JAR no cliente/servidor.
- Ainda pendentes: teste visual/jogável com cliente aberto, claims reais, morte/retorno/câmera em LAN, profiling de MSPT/heap/working set, fundição e máquinas de modpacks, receitas especiais, planejamento completo de sobrevivência e navegação de longo alcance. O LLM pequeno continua sujeito a respostas fracas; JSON válido não comprova conhecimento do modpack.

### Próximo QA executável

Responsável: próxima sessão de QA do projeto, em save descartável do `TesteMineFrinds`. Não modificar saves pessoais para homologação.

1. Atualizar a skin com `/skin Rimuru`; conferir corpo, braços, armadura e troca de skin durante observação.
2. Testar `auto` a partir de mochila vazia perto de árvore; conferir bancada/picareta e conservação exata dos itens.
3. Minerar pedra/carvão/ferro e minério de mod: bloquear minério atrás de parede, trocar ferramenta insuficiente, observar degraus, água/lava e caminho sem apoio.
4. Colocar zumbi e slime perto do dono enquanto trabalha: conferir prioridade, cooldown, todos os alvos, retomada e respeito ao modo stay.
5. Abrir mochila, inserir/remover equipamento e usar shift-click: sem duplicação da mão principal e sem perda nos espaços não editáveis.
6. Matar o NPC com gamerule keepInventory global false e convocá-lo de novo: conferir mochila, armadura, offhand e XP, inclusive após salvar/reabrir.
7. Observar e controlar: testar WASD/mouse, slots 1–9/roda, uso, alcance, Shift, inventário, desconexão, morte, mudança de skin e retorno de posição/modo.
8. Conversar nas duas línguas, parar o runtime, reabrir mundo e conferir fallback/reativação; medir 1/4/8 NPCs antes de afirmar cumprimento de 1 GB normal e 2 GB pico.
