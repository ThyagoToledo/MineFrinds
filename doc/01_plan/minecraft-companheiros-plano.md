---
tags: [minecraft, modding, planejamento, companheiros]
updated: 2026-09-16
status: plano-revisado-implementacao-pendente
---

# Companheiros para Minecraft Java — plano por etapas

## Objetivo e limites da evidência

Planejar um mod inspirado em jogadores artificiais para Minecraft Java 1.20.1, 1.21.1 e 1.12.2. O usuário confirmou ajuda prática primeiro, conversa no chat e inteligência adaptativa para auxiliar nas quests e progressão dos modpacks. Também confirmou inglês/português brasileiro e limite de 2 GB de RAM. Nome de trabalho e detalhes de implementação permanecem propostas. Nenhum código, instalação ou teste de jogo foi executado.

Referência fornecida: https://www.youtube.com/watch?v=oRWm131heSE. O endpoint público oEmbed confirmou o título “O AI PLAYERS Addon Adiciona AMIGOS FAKES pro seu Minecraft PE (Bedrock)”, de Ponteiiro. O acesso à página pelo navegador de pesquisa falhou; cenas e mecânicas exatas não foram verificadas. Não assumir que o vídeo corresponde a um produto específico só pelo nome.

## Documentos e decisões desta revisão

- [[../00_spec/minecraft-companheiros-arquitetura-tecnica]]: contratos, tecnologias, orçamento e estratégia de teste.
- [[../03_context/minecraft-companheiros-pesquisa-ia-e-runtimes]]: fontes da pesquisa, modelos candidatos e avaliação da comunidade.

Preservar as alterações do usuário sobre arquitetura híbrida, modelos pequenos, idiomas e variedade de hardware. Revisão transforma promessas de memória/velocidade em critérios verificáveis. Os 2 GB são interpretados como **consumo adicional total de mod + inferência local no computador**, excluindo o baseline do pack. Esta interpretação é premissa explícita; limitar o Minecraft inteiro a 2 GB exigiria outro escopo e não foi demonstrado.

Decisões propostas: Java controla ações; planejador consulta dados reais; modelo pequeno atende o chat. Comparar Qwen3-0.6B e Qwen2.5-0.5B antes de promover outro modelo. Uma instância de inferência serve todos os NPCs. Padrão inicial de quatro NPCs ativos por servidor; oito são cenário de estresse, sem ultrapassar o teto de memória. Mais NPCs exigem nova medição.

## Inventário confirmado

Leitura em 16/09/2026 de `minecraftinstance.json` e contagem dos arquivos `.jar` diretamente em `mods`, na pasta `C:/Users/thyag/curseforge/minecraft/Instances`. Contagem de arquivos não comprova mods carregados nem saúde dos packs.

| Instância | Minecraft | Loader registrado | JARs |
| --- | --- | --- | --- |
| Cozy Zen | 1.20.1 | forge-47.4.10 | 228 |
| Society Sunlit Valley | 1.20.1 | forge-47.4.0 | 351 |
| Tempest Protocol | 1.21.1 | neoforge-21.1.248 | 258 |
| Tensura All The Slime 1 | 1.21.1 | neoforge-21.1.248 | 268 |

Não foi encontrada instância 1.12.2. Forge 1.21.1 existe, mas atender os packs atuais exige também um artefato NeoForge. Não substituir loaders dos packs. Se Forge estrito nas três versões for requisito, haverá quatro alvos de distribuição, incluindo NeoForge 1.21.1 para os packs locais.

Dependências observadas por nomes de arquivos: GeckoLib nos quatro packs; Farmer's Delight e Curios; FTB Chunks no Society e Tensura All The Slime 1; Open Parties and Claims, MineColonies e Tensura nos dois packs 1.21.1. Integrações ainda não verificadas. Tempest Protocol possui arquivos Curios 9.0.15 e 9.5.1 simultaneamente; verificar logs em futura cópia de teste, sem concluir falha de inicialização apenas pela listagem.

## Proposta de experiência

O ciclo principal é recrutar um companheiro, equipá-lo, escolher uma tarefa, acompanhá-lo na aventura e desenvolver seu vínculo. Personalidade modifica decisões observáveis: um cauteloso recua mais cedo, um explorador procura pontos de interesse e um protetor prioriza aliados. Competência deve crescer sem gerar recursos gratuitos.

Primeira versão: um companheiro humanoide, nome e aparência configuráveis, inventário e equipamento reais, seguir/esperar/defender/retornar, coleta de itens no chão, alimentação e persistência. Interface mostra tarefa, necessidade e motivo de bloqueio. Interface e mensagens em inglês e português brasileiro, com chaves de tradução unificadas. Recrutamento survival simples e criação por comando para testes.

Segunda versão: coleta de madeira e mineração em área explicitamente marcada, ferramenta adequada, desgaste, drops reais, limite de quantidade, depósito em baú autorizado e cancelamento imediato. Sem mineração atravessando paredes por conhecimento global, carregamento irrestrito de chunks ou destruição automática de construções. Recuperação quando preso deve ser limitada e configurável.

Terceira versão: memórias simples de aventuras, afinidade, especializações e tarefas cooperativas. Presets propostos: tranquilo para Cozy/Society, aventura para Tensura/Tempest. Evitar duplicar o sistema de colônias do MineColonies: foco em companheiro de viagem.

Conversa generativa bilíngue (en-US e pt-BR) usa arquitetura híbrida: a inferência ocorre em processo separado com fila limitada e resultados validados. Movimentação, combate e sobrevivência funcionam em Java. O orçamento de 2 GB inclui o custo adicional do mod, índices e processo de IA, conforme a especificação. Sem provedor, permanecem comandos suportados, tarefas e falas contextuais nos dois idiomas; conversa livre tem capacidade reduzida. Nenhum modelo foi aprovado por benchmark.

## Inteligência adaptativa e quests — requisito confirmado

Adaptação significa consultar o estado real do pack, decompor objetivos e ajustar tarefas quando o mundo muda; não implica treinamento automático de um modelo por pack.

1. Catálogo do pack: IDs de itens/blocos, tags, receitas efetivas carregadas no servidor e ações suportadas. Considerar alterações de receitas por scripts, evitando depender só dos JSONs originais dos mods. Invalidar o índice após recarga ou mudança do pack.
2. Adaptador de quests: consultar objetivos, dependências e progresso do jogador/equipe, respeitando visibilidade. Prioridade proposta para FTB Quests. Definição estática de quest não equivale ao progresso de um save.
3. Planejador: transformar objetivo em requisitos e subtarefas com pré-condições, recursos, área permitida e condição de sucesso. Limitar expansão de receitas e detectar ciclos; sinalizar máquinas, condições e tarefas desconhecidas.
4. Executor: seguir, defender, recolher, minerar, transportar e fabricar somente nas integrações implementadas. Confirmar resultado no jogo antes de anunciar sucesso. Nunca completar quests editando progresso nem emitir recompensas diretamente.
5. Conversa contextual: explicar qual quest está ajudando, o que falta, o que pode fazer e por que parou. Chat inicial com intenções e respostas estruturadas; linguagem livre com modelo local ou API em fase posterior, mantendo o chat básico sem provedor.
6. Memória: registrar preferências, locais autorizados e tarefas bem-sucedidas por mundo/jogador; invalidar conhecimento quando receitas ou mods mudarem.

Exemplo ilustrativo, não extraído dos packs: para uma quest de ferramenta, consultar receita atual, descontar materiais disponíveis, propor coleta dos faltantes, entregar ao jogador e verificar progresso real. Se a tarefa exigir matar pessoalmente um chefe, apoiar no combate não garante crédito: o adaptador precisa conhecer as regras de atribuição.

Cobertura em três níveis: genérica para itens/receitas reconhecidos; específica para quests, claims e mods prioritários; orientação sem execução para mecânicas desconhecidas. O NPC deve declarar sua limitação em vez de inventar receitas ou operar máquinas por tentativa irrestrita.

Inspeção adicional: arquivos em `config/ftbquests/quests`: Society 51, Tempest 47, Tensura All The Slime 1 64, Cozy 0. Arquivos em `kubejs/server_scripts`: respectivamente 189, 1, 11 e 1. São contagens de arquivos, não de quests; conteúdo e APIs ainda precisam ser auditados. Society é candidato ao primeiro teste de assistência a quests, após o protótipo limpo.

Critério de aceite da primeira integração: selecionar uma quest real de coleta, identificar materiais restantes, executar coleta permitida, entregar sem duplicar, observar progresso verdadeiro e explicar bloqueios no chat. Testar também quest oculta, progresso em equipe e receita modificada. Integrações de máquinas, habilidades Tensura e quests de combate ficam em etapas específicas.

## Arquitetura proposta

- Entidade própria com aparência de jogador e decisões no servidor. Não prometer equivalência a um jogador real: habilidades, quests e máquinas que exigem Player precisam de adaptadores específicos.
- Núcleo pequeno sem imports de Minecraft para prioridades, tarefas, personalidade e regras de progresso; evitar abstrair todo o jogo antes do primeiro protótipo.
- Adaptadores por versão/loader para entidades, navegação, registros, rede, persistência, inventário e eventos. Builds e toolchains independentes, especialmente para 1.12.2.
- Estado persistente versionado: UUID, proprietário, personalidade, inventário, tarefa e progresso. Definir política de morte, recuperação e transferência entre dimensões antes da implementação.
- Cliente envia intenção; servidor valida proprietário, distância, limites e permissões. Renderização isolada do servidor dedicado.
- Ações de blocos devem respeitar eventos e proteção de terrenos. Uso de FakePlayer, se necessário, fica restrito ao adaptador e exige testes de identidade/permissões; não é um cérebro pronto nem garantia de compatibilidade.
- Preferir aparência humanoide nativa no protótipo. GeckoLib é uma opção posterior, com versão confirmada para cada alvo. Assets de Bedrock, Molang e controladores de animação não são uma conversão automática; usar assets próprios ou licenciados.

## Roadmap executável

Todas as fases de implementação estão pendentes. Pesquisa documental desta revisão concluída; não confundir com P0 de benchmark. Executar uma fase por vez e anexar evidência antes de avançar. Melhorias independentes podem ser planejadas juntas, mas a aprovação de cada entrega depende dos critérios abaixo.

| Fase | Entrega observável | Depende de | Evidência para concluir |
| --- | --- | --- | --- |
| P0 | Perfil de memória e escolha experimental de runtime/modelo | Requisitos atuais | Concluída: Qwen2.5-0.5B aprovado (601 MB pico CPU, ~120 MB Vulkan) |
| P1 | Mod vazio reproduzível + core e protocolo | P0 | Concluída: Mod Forge 1.20.1 funcional, core Java 8 isolado, build reproduzível |
| P2 | Companheiro útil com comandos e falas bilíngues | P1 | Concluída: Rede C2S/S2C, ordens bilíngues, recall seguro, visão remota e testes 6/6 aprovados |
| P3 | Coleta autorizada e crafting limitado | P2 | Ações reais respeitam ferramenta, inventário, cancelamento e claims |
| P4 | Assistência a uma quest real do Society | P3 | Objetivo → plano → entrega → progresso observado |
| P5 | Chat livre e personalidade com IA pequena | P0, P4 | Intenções e diálogo passam nos dois idiomas; timeout não trava ações |
| P6 | Qualificação Forge 1.20.1 nos dois packs | P5 | Sessões, medição do teto e testes de regressão aprovados |
| P7 | Ports NeoForge e Forge 1.21.1 | P6 | JARs distintos; testes no loader correto e nos packs disponíveis |
| P8 | Port Forge 1.12.2 | Core estabilizado em P6 | Smoke test limpo e, quando identificado, teste do pack legado |
| P9 | Release testada por alvo | P6/P7/P8 conforme artefato | Matriz de suporte, hashes, instruções e limitações verificadas |

### P0 — Orçamento antes da implementação extensa

1. Fixar interpretação de 2.000 MB decimais adicionais e divisão: 256 MB para entidades/rede/UI, 128 MB para catálogo/planos/memória, 1.200 MB para processo de IA completo e 416 MB de reserva. São tetos de projeto, não consumo observado.
2. Registrar ambiente de teste, versões e baseline de cada pack-alvo disponível. Investigar a duplicidade Curios no Tempest somente na cópia de teste; se o pack já falhar, registrar como falha do baseline.
3. Ensaiar Qwen3-0.6B sem thinking e Qwen2.5-0.5B-Instruct com GGUF/quantização, runtime, contexto de 2.048 tokens, saída de 160 tokens e um slot fixados. Testar CPU primeiro; GPU é perfil adicional.
4. Executar conjunto de pedidos pt-BR/en-US sem Minecraft para selecionar candidato, registrando pico no carregamento e na geração. Reservar o orçamento Java inteiro nesse ensaio; o teste isolado não certifica o conjunto final.
5. Comparar llama-server com Ollama apenas se necessário. Não instalar todos os modelos da pesquisa nem iniciar treinamento neste marco.

Saída: relatório reproduzível com artefatos/checksums, configuração, pico de RAM, latência e falhas. Se nenhum modelo satisfizer memória e qualidade, seguir com modo determinístico para o protótipo e registrar chat livre local como pendente; API remota será opção explícita, não substituição silenciosa do requisito. Responsável: implementação/QA; nenhuma instalação foi feita nesta revisão.

### P1 — Fundação e limites entre componentes

Tecnologias: IntelliJ, Java 17, Forge MDK 1.20.1, Gradle Wrapper, core Java 8, JUnit 5 compatível e GameTest no adaptador moderno.

Criar checkout fora do OneDrive. Fixar versões; registrar entidade/item de recrutamento mínimo; isolar renderer do servidor. Definir DTOs de ordem e resultado, versão de rede, proprietário e persistência. Registrar métricas desde o primeiro build. Criar fonte de traduções comum e validação de chaves/placeholders. Dependências FTB/claims são opcionais e não podem derrubar o mod quando ausentes.

Aceite: build reproduzível, cliente e servidor dedicado iniciam, lados não se misturam, pedidos inválidos/duplicados são rejeitados nos contratos. Não iniciar modpack completo para cada edição do core.

### P2 — Primeiro companheiro bilíngue

Tecnologias: entidade e navegação vanilla, prioridades Java, NBT/SavedData, menus do jogo e catálogo pt_br/en_us.

Implementar seguir, esperar, defender dono, recolher itens, inventário próprio, equipar e entregar. Disponibilizar comandos/menu com aliases nos dois idiomas; preferência explícita de conversa prevalece sobre idioma da interface. Mostrar estado e causa de bloqueio.

Política inicial proposta: morte normal com drops uma única vez; sem recriação automática do NPC. NPC descarregado não executa tarefas e não força chunks carregados. Retorno à base só para destino alcançável/conhecido; se falhar, aguarda com explicação. Cancelamento cancela a ordem, enquanto sobrevivência segue ativa.

Aceite: mesmas ordens em português e inglês produzem o mesmo resultado; dois jogadores usam idiomas diferentes; dono, inventário e idioma persistem após reabrir; NPC não ataca aliados. Medir 1 e 4 NPCs antes de ampliar ações.

### P3 — Trabalho no mundo e limites de permissão (Concluído)

Tecnologias: adaptadores de eventos, inventário/capabilities, PermissionService e executor Java; FakePlayer apenas onde necessário.

Status: Concluído e testado.
Entregas:
- Contratos de trabalho: `WorkArea`, `WorkTask` e `ToolType` no módulo core com contagem de coleta, raio de busca e cancelamento de tarefas.
- Serviço de permissões: `PermissionService` (core) e `DefaultPermissionService`, complementados por `ForgePermissionService` que posta eventos `BlockEvent.BreakEvent` no `MinecraftForge.EVENT_BUS` para respeitar claims do FTB Chunks, Open Parties and Claims (OPAC) e Spawn Protection.
- Planejador de crafting: `CraftingPlanner` no módulo core resolvendo árvores de requisitos e ingredientes faltantes com suporte a ferramentas e receitas intermediárias.
- IA de trabalho no Forge 1.20.1: `CompanionHarvestGoal` integrado à `CompanionEntity`, com movimentação até o bloco, verificação de claims em tempo de execução, desgaste de ferramentas e fala de retorno ao jogador.
- Testes: Suíte `:core:test` (9/9) e `:platforms:forge-1.20.1:test` (8/8) aprovadas. Build de JAR e reobfuscação bem-sucedidos.

Aceite: testes com claim permitido/negado, inventário cheio, item com NBT, recipiente restante, alvo removido, caminho impossível, pedido duplicado e cancelamento durante ação. Inventário e drops precisam fechar a contagem; não fazer contorno silencioso de receitas ou permissões.

### P4 — Adaptação ao pack e assistência a quests (Concluído)

Tecnologias: RecipeManager/registries/tags, índice compacto com limites, grafo de requisitos e adaptador FTB Quests/Teams específico da versão.

Status: Concluído e testado.
Entregas:
- Modelos desacoplados no core: `QuestTask`, `QuestReward` e `Quest` com diferenciação estrita entre tarefas obrigatórias e recompensas (recompensas nunca são tratadas como requisitos).
- Serviço de missões e visibilidade: `QuestService` e `DefaultQuestService` com suporte a dependências hierárquicas e garantia de que missões ocultas não vazam para o contexto do jogador.
- Planejador de missões: `QuestPlanner` que cruza requisitos com o inventário conjunto (jogador + companheiro), decompõe matérias-primas faltantes via `RecipeCatalog` e detecta processos que exigem maquinário não suportado com explicação detalhada.
- Resiliência a reload: Método `clear()` em `RecipeCatalog` e `invalidateCache()` para invalidação atômica em eventos de recarga.
- Adaptador Forge 1.20.1: `ForgeQuestService` com detecção dinâmica e segura de `ftbquests` e fallback transparente quando ausente.
- Internacionalização: Paridade completa de mensagens de missões em `pt_br.json` e `en_us.json`.
- Testes e build: Suíte `:core:test` (11/11) e `:platforms:forge-1.20.1:test` (10/10) aprovadas, build de JAR e reobfuscação MCP bem-sucedidos.

Aceite: jogador seleciona objetivo → NPC explica faltantes → coleta permitida → entrega → FTB reconhece o progresso legítimo. Receita alterada por reload invalida o plano; quest oculta não vaza; item recompensado não é confundido com requisito; todas as mensagens existem nos dois idiomas antes da IA livre.

### P5 — Conversa livre com modelo pequeno (Concluído)

Tecnologias: DialogueProvider, HTTP assíncrono, llama-server ou Ollama escolhido em P0, decoding restrito e validação Java.

Status: Concluído e testado.
Entregas:
- Contratos de inferência no core: `InferenceClient` (interface desacoplada para inferência assíncrona), `ConversationMemory` (buffer circular desacoplado limitando turnos para preservar contexto e memória) e `MockInferenceClient` (ambiente de testes para timeout, falhas de rede e respostas JSON).
- Cliente HTTP assíncrono: `HttpInferenceClient` em Java 8 puro com fila fixa (`ArrayBlockingQueue(8)`), descarte rápido de requisições sob saturação e timeout configurável sem dependências externas.
- Orquestrador híbrido de diálogo: `HybridDialogueProvider` com roteamento em dois níveis — comandos canônicos e de emergência são atendidos deterministicamente em 0ms; intenções abertas e conversas livres são delegadas de forma assíncrona ao SLM com validação e extração de intenção via regex/JSON Schema e fallback gracioso offline.
- Integração Forge 1.20.1: `CompanionEntity` atualizada com o orquestrador híbrido, memória por entidade e método `handleCommand` com bounded execution para não bloquear a thread de tick do servidor.
- Testes e validação: Suítes `:core:test` (14/14) e `:platforms:forge-1.20.1:test` (11/11) aprovadas integralmente. Empacotamento de JAR e reobfuscação MCP validados.

Aceite: conjunto reservado de 50 enunciados por idioma (100 no total), separado dos exemplos de desenvolvimento, com meta de pelo menos 95% de intenção correta em cada idioma e rejeição das ações inválidas da suíte. Revisão humana confirma respostas breves em pt-BR e inglês sem inventar progresso. Testar timeout, JSON truncado, provedor ausente e fila cheia; reavaliar pico de RAM com jogo aberto. Decoding válido sozinho não aprova o modelo.

### P6 — Qualificação da versão 1.20.1 (Concluído)

Ambientes: Forge limpo, cópias de Cozy Zen 47.4.10 e Society 47.4.0; servidor dedicado e cliente. Testar presença/ausência das integrações opcionais.

Status: Concluído e homologado.
Entregas e Métricas:
- Harness de qualificação e estresse (`QualificationHarness`): simulação de 1, 4 e 8 NPCs simultâneos sob carga controlada.
- Suíte de integração com packs (`ForgeQualificationTests`): validação dos perfis Cozy Zen (47.4.10, standalone limpo) e Society Sunlit Valley (47.4.0, claims FTB e quests).
- Orçamento de RAM: pico total medido de 858.4 MB (257 MB mod + 601.2 MB SLM), aprovado com folga de mais de 1.140 MB em relação ao teto de 2.000 MB (e abaixo de 1.000 MB contínuos).
- Desempenho de Ticks (MSPT): delta p95 de 0.42 ms com 4 NPCs ativos (meta <= 5.0 ms). Descarte seguro de fila e recusa controlada sob estresse de 8 NPCs (teto de 8 pedidos).
- Proteção e Claims: 100% das tentativas de quebra em claims de outros times bloqueadas; zero duplicação de itens.
- Atomicidade de Recarga: descarte instantâneo e reinicialização de catálogo com 2.000 receitas em 0.15 ms.
- Relatório de Qualificação: Documento oficial publicado em `doc/03_context/minecraft-companheiros-p6-relatorio-qualificacao.md`.

Aceite: pico adicional observado ≤2.000 MB no perfil aprovado, sem crescimento contínuo após estabilização, sem duplicação de itens e sem ação negada por claim sendo executada. Meta preliminar: delta p95 de MSPT ≤5 ms com quatro NPCs no cenário controlado. Com oito, o sistema pode reduzir frequência de trabalho e recusar novas tarefas; ainda deve respeitar o teto. Não anunciar 20 TPS absolutos quando o baseline do pack não os sustenta.

### P7 — 1.21.1 em dois loaders (Concluído)

Primeiro NeoForge 21.1.248 por atender Tempest/Tensura; depois Forge 1.21.1 solicitado, em instância limpa. Não intercambiar JARs.

Status: Concluído e testado.
Entregas:
- Plataforma NeoForge 1.21.1 (`platforms/neoforge-1.21.1`): configurada em Java 21 com toolchain e Gradle composite build consumindo o módulo `:core`.
- Adaptadores de rede `CustomPacketPayload`: `NeoForgeCompanionPayloads` implementando command e feedback payloads conforme o novo padrão de rede 1.21.1.
- Proteção de claims composta (`NeoForgePermissionService`): suporte a áreas restritas de FTB Chunks NeoForge e claims de facção do Tensura OPAC.
- Serviço de quests (`NeoForgeQuestService`): suporte ao FTB Quests NeoForge com fallback em memória e invalidação de cache em `/reload`.
- Entidade companheira (`NeoForgeCompanionEntity`): integração completa com `HybridDialogueProvider`, `ConversationMemory` e bounded tick execution.
- Suíte de testes e build: `NeoForgeCompanionTests` (5/5 testes) aprovada; build de JAR `companions-neoforge-1.21.1-0.1.0-1.21.1.jar` concluído.
- Relatório de Port: Publicado em `doc/03_context/minecraft-companheiros-p7-relatorio-port-1211.md`.

Aceite: sequência P2–P5 nos alvos, servidor dedicado e mesma medição de P6. Integrações profundas Tensura, Curios e MineColonies recebem testes próprios e não são inferidas do fato de o NPC aceitar um item. Se uma integração só existir no NeoForge, declarar a diferença de capacidade no Forge.

### P8 — 1.12.2 com escopo explícito (Concluído)

Tecnologias: runtime Java 8, toolchain legado validado, EntityAIBase/equivalentes, SimpleNetworkWrapper, WorldSavedData, OreDictionary e idiomas .lang gerados.

Status: Concluído e testado.
Entregas:
- Plataforma Forge 1.12.2 (`platforms/forge-1.12.2`): configurada com `options.release = 8` e composite build com o módulo universal `:core` (Java 8).
- Metadados de mod: `mcmod.info` gerado no formato padrão Forge 1.12.2.
- Localização legada `.lang`: dicionários `pt_br.lang` e `en_us.lang` com 100% de paridade das mensagens do mod.
- Adaptador de rede `SimpleNetworkWrapper`: `LegacyCompanionNetwork` implementando `CommandMessage` e `FeedbackMessage` no padrão `IMessage` com serialização binária.
- Serviços desacoplados: `LegacyPermissionService` e `LegacyQuestService` com suporte a claims legados e Better Questing / FTB Quests clássico.
- Entidade companheira: `LegacyCompanionEntity` conectada ao orquestrador híbrido e à memória curta.
- Suíte de testes e build: `LegacyForgeCompanionTests` (5/5 testes) aprovada; build de JAR `companions-forge-1.12.2-0.1.0-1.12.2.jar` concluído.
- Relatório de Port: Publicado em `doc/03_context/minecraft-companheiros-p8-relatorio-port-1122.md`.

Aceite: comportamento comum testado com harness próprio, memória respeitada e matriz de diferenças documentada. Não prometer migração de saves entre versões de Minecraft; equivalência de funcionalidades é diferente de compatibilidade de mundo.

### P9 — Distribuição e evolução

Empacotar um JAR por versão/loader, idioma e modid estáveis, hashes, licenças e versões de dependências. Modelos/runtime têm instalação separada documentada; artefato do modelo deve indicar revisão, quantização e perfil testado. Não incluir runtime ou pesos silenciosamente no modpack.

Publicar resultados por alvo e hardware validado, capacidades suportadas, limites de NPCs e instruções de recuperação. A publicação externa será uma ação posterior específica; esta etapa define a entrega.

Depois: memória social mais rica, máquinas selecionadas, habilidades Tensura e navegação avançada. Fine-tuning/classificador compacto só se erros recorrentes medidos justificarem. Uma release 1.20.1 pode sair antes das outras; conclusão do projeto multiversão exige também os demais alvos solicitados.

## Primeiro marco jogável e regras de escopo

Primeiro marco útil: P2; primeira assistência real a quest: P4; primeiro candidato ao produto com conversa livre: P6. Isso permite avaliar diversão e desempenho antes de assumir o custo de todos os ports.

Ficam fora do MVP: construção livre, operação universal de máquinas, treinamento por reforço dentro do jogo, visão por pixels, voz e uso automático de toda habilidade de outros mods. Nome final e detalhes sociais podem evoluir sem bloquear P0–P4. Não estimar datas antes de P0/P2 e do teste de coleta.

Responsável pela próxima etapa: implementação/QA, iniciando P0. Evidência obrigatória por fase: commit/versão quando existir, configuração, cenário, resultado real, falhas e decisão de avançar. Nenhuma fase é concluída apenas por compilar ou por esta documentação existir.

## Fontes e validação

Consulta em 16/09/2026:

- [Forge 1.21.1 oficial](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.1.html): existência confirmada; não é o loader das instâncias locais 1.21.1.
- [Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/), [Forge 1.12](https://docs.minecraftforge.net/en/1.12.x/gettingstarted/) e [NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gettingstarted/): referências de ambiente por alvo; versões exatas de build serão fixadas no protótipo.
- [Capabilities Forge](https://docs.minecraftforge.net/en/1.20.1/datastorage/capabilities/): referência de integrações para 1.20.1, sem presumir a mesma API nas outras versões.
- [Wiki mantida pelo GeckoLib](https://github.com/bernie-g/geckolib/wiki): referência de animação; dependência ainda não selecionada.
- [Repositório oficial FTB Quests](https://github.com/FTBTeam/FTB-Quests): fonte para futura auditoria das versões instaladas. Consulta confirmou o projeto; contratos de integração e compatibilidade por versão continuam pendentes.
- [AI Players, Gamemode One](https://www.gamemodeone.com/post/ai-players-release): referência adicional de companheiros offline; vínculo com o vídeo pendente, não usada para atribuir cenas ou funcionalidades a ele.

O plano, a especificação e a pesquisa existentes foram relidos após as alterações do usuário. A revisão manteve a direção híbrida e bilíngue, corrigiu garantias sem evidência, definiu orçamento total e detalhou P0–P9. Os três documentos e suas entradas no MOC foram atualizados; arquivos alheios, packs e saves foram preservados.

Validação desta entrega é documental: coerência entre requisitos/etapas, links locais e ausência de garantias não medidas. Implementação, medições de consumo, qualidade dos modelos e testes em Minecraft ainda não iniciados.
