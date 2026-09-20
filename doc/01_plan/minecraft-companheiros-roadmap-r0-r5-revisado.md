---
tags: [minecraft, minefriends, roadmap, auditoria, neoforge, forge, ia]
updated: 2026-09-19
status: roadmap-revisado-pos-r1
---

# MineFriends — roadmap revisado R0–R5

## Planos dedicados

- [[R0-integridade-mundo-itens]]
- [[R1-protocolo-snapshot-persistencia]]
- [[R2-ia-assincrona-bilingue]]
- [[R3-arquitetura-desempenho-operacao]]
- [[R4-experiencia-assets-paridade]]
- [[R5-homologacao-modpacks-release]]

## Leitura do estado atual

Esta revisão compara os commits `98046b0` (R0) e `e9850e6` (R1) com o código atual. As três suítes de plataforma passaram em execução limpa e sequencial. Há 74 testes JUnit registrados (incluindo transação de crafting, contratos de parser/supervisor, DTO de intenção e replay do R1.1), mas a maioria é unitária: isso comprova contratos Java e compilação, não claims reais, execução dentro do mundo, persistência após reinício nem desempenho de modpack.

| Etapa | Estado real | Decisão |
| --- | --- | --- |
| R0 | Parcialmente concluída | Abrir R0.1 antes de liberar crafting/trabalho como estáveis |
| R1 | Parcialmente concluída | Revisão, requestId, replay e DTO de intenção implementados; migração completa e persistência operacional pendentes |
| R2 | Parcialmente concluída | Supervisor/parser, caminhos async modernos, chat tipado com revalidação de revisão e métricas implementados; avaliação opt-in e intents de ação diretas pendentes |
| R3 | Parcialmente iniciada | Cadência de coleta, cooldown de scans, fila global e métricas aplicadas; decomposição, orçamento global e profiling ainda pendentes |
| R4 | Protótipo parcial | Consolidar depois de snapshots e telemetria estáveis |
| R5 | Não comprovada | Relatórios anteriores não substituem homologação reproduzível |

## R0.1 — integridade de mundo e itens

### O que já foi feito

- removida a duplicação direta que adicionava a saída ao NPC e entregava outra pilha ao dono;
- item desconhecido passou a ser rejeitado;
- pré-checagem de ingredientes, destino único da saída e verificações antes de quebrar/plantar;
- recall NeoForge ganhou combate, cooldown e busca de posição segura;
- testes unitários documentam as regras pretendidas.

### O que ainda precisa mudar

1. `NeoForgePermissionService` só delega ao `DefaultPermissionService`; não integra FTB Chunks ou OPAC. `mayInteract` não substitui os eventos dos mods de claim.
2. `destroyBlock(..., this)` e `setBlockAndUpdate` continuam mutando o mundo diretamente. Executar quebra pelo fluxo do fake player/GameMode ou publicar e respeitar os eventos corretos antes do commit.
3. O crafting usa tabela manual de itens e fórmulas de tronco/pedra/trigo. Substituir por `RecipeManager`, `RecipeHolder`, ingredientes efetivos, quantidade de saída, itens restantes, componentes/NBT e receitas KubeJS carregadas.
4. A operação chamada “atômica” ainda não tem reserva/rollback. Criar `CraftTransaction`: simular entradas e destino, reservar, consumir uma vez, inserir uma vez e reconciliar falha.
5. Retirada parcial do baú não pode continuar silenciosamente para outra fabricação sem contabilizar o que já foi entregue.
6. Persistir ou cancelar de modo seguro `pendingCraftItem` e recursos reservados em save/reload, morte e dismiss.

### Implementação

- Criar `WorldActionExecutor` por plataforma com `breakBlock`, `placeBlock`, `interactContainer` e `teleportSafely`.
- Criar `MinecraftRecipeAdapter` no NeoForge e Forge moderno; converter receita suportada para DTO do core.
- Criar `InventoryTransaction` com plano imutável, versão dos inventários e resultado tipado.
- Manter whitelist inicial de crafting shaped/shapeless simples; rejeitar receita especial/dinâmica até haver adaptador.
- Adicionar GameTests de conservação, inventário cheio, recipiente restante, claim negado, cancelamento e reload.

### Critério para fechar R0

- contagem total de cada item é conservada em sucesso, falha e cancelamento;
- FTB Chunks e OPAC negam ações em cópias reais dos packs;
- receita alterada por KubeJS produz o resultado efetivo ou é recusada com motivo;
- recall nunca força chunk, cruza dimensão por padrão ou posiciona em perigo;
- evidência inclui teste de mundo, log e versões dos mods de claim.

## R1.1 — protocolo, snapshot e persistência confiáveis

### O que já foi feito

- `CompanionSnapshot` no core;
- quatro payloads NeoForge registrados com `StreamCodec`;
- GUI usa snapshot para estado básico;
- `CompanionSavedData` com `schema_version = 1` guarda baú e metadados;
- troca de skin copia inventário, equipamento, XP, vida e modo durante a recriação em memória.

### O que ainda precisa mudar

1. `CommandPayload` aceita string iniciada por `/` e a entrega ao dispatcher. Trocar por `CommandIntentPayload` com enum, parâmetros limitados, UUID/revisão e allowlist; nunca transportar comando arbitrário.
2. Snapshot precisa de `requestId`, `revision`, timestamp lógico e UUID lógico estável do companheiro. Descartar resposta velha no cliente.
3. A instância cliente usa snapshot estático global; limpar em logout/troca de servidor/mundo e vincular ao dono/sessão.
4. `SavedData` persiste metadados, mas não tarefa, reservas, política de morte ou inventário lógico. Definir fonte única para cada dado e evitar salvar inventário tanto como player quanto como registro próprio.
5. Usar o `SavedData` do overworld para estado global entre dimensões e validar migração de schema desconhecido.
6. Troca de skin ainda recria um `ServerPlayer` com UUID novo e pode perder attachments/capabilities/efeitos de mods. Separar UUID lógico do perfil de renderização e testar os dados que devem sobreviver.
7. Busca remota de skin usa HTTP síncrono de até 2,5 s; mover para serviço assíncrono limitado, cache e fallback.

### Implementação

- Definir `CompanionCommandRequest`, `CompanionSnapshotV2` e `FeedbackResult` no core.
- Registrar codecs com limites de string/coleção e validação server-side de dono, estado e intenção.
- Criar `CompanionRepository` sobre `SavedData`, com migrações explícitas e backup lógico antes de upgrade.
- Emitir snapshot por mudança relevante e atualização limitada; evitar polling por tick.
- Criar testes de pacote malformado, spoof de dono, replay, snapshot fora de ordem, logout e schema futuro.

### Critério para fechar R1

- nenhum payload executa comandos gerais;
- reiniciar servidor restaura somente os dados definidos, sem duplicar ou perder inventário;
- snapshots antigos são descartados e dois jogadores veem apenas seus companheiros;
- troca de skin não muda identidade lógica nem perde dados declarados;
- teste real de cliente + servidor dedicado confirma o ciclo completo.

## R2 — IA assíncrona real e bilíngue

### Escopo revisado

- Um `InferenceSupervisor` por servidor, desligado por padrão.
- `InferenceTransport` faz apenas HTTP; supervisor possui uma fila de oito, uma geração ativa, timeout, circuit breaker e métricas.
- Comandos determinísticos continuam locais. Conversa livre e classificação ambígua seguem pela fila.
- Toda requisição leva `requestId`, NPC, dono, idioma, revisão e deadline. A resposta volta pela thread do servidor e é revalidada.
- O modelo propõe `Intent`; o mesmo validador de R1 decide se ela pode virar ordem. Relatório de sucesso vem somente do executor de R0.
- Remover `processSync` dos caminhos produtivos Forge 1.20.1 e NeoForge 1.21.1. No legado, manter generativo desligado até existir despacho seguro.
- Gson desserializa envelope OpenAI-compatible e conteúdo JSON direto; limitar corpo, fala e enums. Preservar Unicode pt-BR.

### Avaliação

- Unitários com transporte fake: fila, timeout, circuit breaker com relógio injetável, shutdown, JSON inválido e cancelamento.
- Conjunto de desenvolvimento separado de 50 frases reservadas por idioma.
- Avaliação Qwen opt-in guarda GGUF/hash, runtime, prompt, respostas, acurácia, latência e memória.
- Meta: pelo menos 95% de intenção em cada idioma. Latência determinística é benchmark, não assert `<1 ms` de CI.

### Critério para fechar R2

- nenhum tick espera rede;
- resposta após morte/dismiss/troca de dimensão é descartada;
- provedor ausente mantém todas as ações determinísticas;
- fala nunca anuncia ação sem `ActionResult` confirmado;
- perfil integrado continua ≤1 GB comum e ≤2 GB de pico no ambiente aprovado.

## R3 — decomposição, desempenho e operação

### Objetivo

Reduzir o custo e a fragilidade do `CompanionServerPlayer`, atualmente responsável por movimento, sensores, combate, trabalho, inventário, crafting, fala e ciclo de vida.

### Implementação

- Extrair `CompanionSensors`, `MovementController`, `CombatController`, `WorkController`, `InventoryController` e `CompanionLifecycle`.
- Escalonar scans entre NPCs; impor orçamento global de buscas e caminhos por tick.
- Cachear alvos brevemente e invalidar por evento; não escanear cubos completos todo tick.
- Medir quantidade/duração de scans, replanejamentos, tarefas, fila de IA e falhas.
- Remover HTTP síncrono de skin e qualquer I/O da thread do servidor.
- Corrigir build concorrente: adaptadores hoje compartilham `core/build`, portanto `clean` paralelo pode apagar classes. Publicar o core em repositório local temporário por revisão ou serializar os jobs explicitamente.
- Criar configuração comum: máximo de NPCs, raios, frequências, teleporte, fila e IA.

### Critério para fechar R3

- quatro NPCs no perfil comum sem crescimento contínuo de heap;
- oito NPCs degradam frequência/fila sem ultrapassar o teto;
- delta p95 de MSPT medido contra baseline e dentro da meta aprovada;
- servidor desliga sem threads, requests ou fake players órfãos;
- clean builds são reproduzíveis e o CI não sofre corrida do core.

## R4 — experiência, assets e paridade por versão

### Implementação

- Painel com lista de NPCs, tarefa/fila, bloqueio, inventário, quests, aparência, idioma e configurações.
- Feedback usa `FeedbackResult` e snapshot; remover textos de sucesso locais antes de confirmação do servidor.
- Câmera principal remota continua padrão barato; picture-in-picture permanece experimento desligado.
- Quatro skins originais 64×64 com autoria/licença; presets de terceiros ficam opcionais e sujeitos a política de uso/cache.
- Todas as strings próprias em `pt_br` e `en_us`; nomes/IDs de pack recebem fallback explícito.
- Publicar matriz por plataforma: NeoForge 1.21.1 é referência; Forge 1.20.1 recebe paridade definida; 1.12.2 pode ser subset documentado.
- Acessibilidade: teclado, escala de GUI, texto sem corte e estados diferenciados além da cor.

### Critério para fechar R4

- nenhum estado inventado no cliente;
- GUI funciona em escalas e resoluções previstas;
- câmera sempre tem saída local e não força chunks;
- assets têm fonte e licença;
- diferenças entre versões aparecem no próprio painel/README.

## R5 — homologação, modpacks e release

### Matriz mínima

- NeoForge 1.21.1 limpo (`TesteMineFrinds`), Tempest, Tensura All The Slime e Tensura Neo Otherworld;
- Forge 1.20.1 limpo, Cozy Zen e Society Sunlit Valley;
- Forge 1.12.2 limpo e pack legado quando identificado;
- cliente integrado e servidor dedicado para os alvos modernos.

### Roteiro obrigatório

Spawn/dismiss, logout/relogin, stop/start, morte, dimensão, recall, claim permitido/negado, crafting, inventário cheio, depósito, câmera, skin, dois jogadores, dois idiomas, IA offline/timeout/fila cheia, reload de receitas e quest real suportada.

Cada execução guarda loader, Java, mod list, hash do JAR, configuração, save descartável, log, resultado e medição. Relatórios P6–P9 anteriores passam a histórico preliminar até esse roteiro produzir evidência.

O inventário atual das seis instâncias está em [[../03_context/minecraft-companheiros-r5-inventario-instances]]. Ele é evidência de ambiente disponível, não de execução do mod.

### Critério para fechar R5

- zero duplicação/perda nos cenários aprovados;
- nenhuma ação atravessa claim negado;
- RAM comum ≤1.000 MB e pico ≤2.000 MB adicionais;
- limitações e matriz de recursos publicadas;
- hashes correspondem exatamente aos JARs testados;
- release recebe nova versão; não sobrescrever `0.1.0` com binário diferente.

## Ordem de execução

1. R0.1: claims/eventos e crafting real/transacional.
2. R1.1: intenção tipada, revisão de snapshot e persistência/ciclo de vida.
3. R2 núcleo pode avançar em paralelo lógico, mas a ativação de ações espera 1 e 2.
4. R3 após os contratos estabilizarem.
5. R4 sobre snapshots/feedback finais.
6. R5 somente com builds candidatos versionados.

O próximo release deve usar no mínimo `0.2.0-alpha.1`, pois os JARs `0.1.0` já mudaram de hash e comportamento várias vezes.

## Incrementos posteriores — 19/09/2026

- R0 ganhou correção de quantidade parcial no crafting e o contrato puro `CraftTransaction`, ainda aguardando adaptador `RecipeManager`/rollback de `ItemStack`.
- R2 passou a encaminhar conversa livre tipada diretamente ao provider e a descartar resposta quando a revisão do snapshot mudou.
- R3 reduziu varreduras sem alvo com intervalos de 20 ticks para trabalho e 10 ticks para busca de monstros, além dos 4 Hz de coleta de drops.
- O total de testes executados no ciclo atual é 74: 29 core, 15 Forge 1.20.1, 25 NeoForge 1.21.1 e 5 Forge 1.12.2.


## Incremento de 20/09/2026 — alpha.2

A entrega descrita na [[../03_context/minecraft-companheiros-auditoria-estado-e-plano-de-melhoria#Jogabilidade, IA local e controle remoto — 20/09/2026 — alpha.2|auditoria atualizada]] implementa receitas carregadas com commit sobre cópias (R0), checkpoints de equipamento e sessão de câmera (R1), configuração/ensaio real do modelo bilíngue (R2), percepção/navegação limitada e prioridades (R3), Rimuru e escolha observar/controlar (R4). R5 recebe candidato com versão própria e regressão JVM; não está homologado em jogo.

A fila seguinte é QA jogável da alpha.2, correção de falhas observadas, fundição/estações e planejamento de ingredientes, adaptação a máquinas específicas, depois paridade dos outros loaders e medição formal de RAM/MSPT. O limite de memória continua sendo critério de aceitação, não resultado certificado.
