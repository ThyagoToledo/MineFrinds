---
tags: [minecraft, arquitetura, forge, neoforge, ia, internacionalizacao]
updated: 2026-09-16
status: especificacao-revisada-p0-provisorio
---

# Companheiros Minecraft — especificação técnica

## 1. Contrato do produto

Requisitos confirmados: NPCs para Java 1.20.1, 1.21.1 e 1.12.2; ajuda prática antes da expansão social; conversar em inglês e português brasileiro; adaptar a assistência às receitas, quests e progressão do modpack. Um companheiro deve explicar o que está fazendo e as limitações da tarefa. Conversa livre faz parte do produto planejado; o provedor generativo é uma opção de execução, não uma dependência para movimentação ou combate.

Requisito adicional: alvo operacional de 1 GB e limite de pico de 2 GB de RAM, tratados como custo incremental de mod + inferência local, além do baseline do pack. O contrato de medição abaixo substitui estimativas isoladas.

Este documento especifica implementação futura. APIs identificadas em documentação/código público não comprovam compatibilidade com todos os JARs instalados. Existe um relatório P0 com medidas declaradas de dois modelos, mas ainda sem artefatos brutos reproduzíveis no vault e sem o mod integrado. O roadmap e o inventário local estão em [[../01_plan/minecraft-companheiros-plano]].

## 1.1. Orçamento de RAM e perfil suportado

Premissa explícita de projeto: 2 GB = 2.000.000.000 bytes adicionais no computador em teste, incluindo mod e inferência local. Não significa configurar o heap total do Minecraft com -Xmx2G. Singleplayer inclui cliente e servidor integrado na mesma JVM; servidor dedicado com cliente na mesma máquina soma os dois processos. Em máquinas distintas, relatar orçamento por host e soma do sistema, sem ocultar o custo remoto.

| Parte | Meta comum, MB decimais | Limite de pico, MB decimais | Inclui |
| --- | --- | --- |
| Entidade, execução, rede, UI e arte | 180 | 300 | Estado, navegação, buffers, menus, câmera e assets próprios |
| Conhecimento e memória | 70 | 150 | Índices, snapshots, planos, traduções, resumos e reconstrução de catálogo |
| Inferência local | 650 | 1.200 | Processo/filhos, pesos residentes, KV cache, bibliotecas e buffers |
| Reserva transitória | 100 | 350 | Carregamento, reload, variação do ambiente e incerteza da medição |
| Total | **1.000** | **2.000** | Meta contínua e teto absoluto do perfil aprovado |

Tetos de projeto não são quotas automaticamente impostas pela JVM. Um mod compartilha o heap com os demais; não é possível atribuir um -Xmx independente a ele. Limitar estruturas próprias e medir comparação com baseline é obrigatório. O limite final só pode ser declarado para combinações de ambiente/configuração testadas.

Configuração inicial: quatro NPCs ativos no servidor, uma instância de modelo, um slot de geração, oito pedidos em fila no máximo, uma pendência por NPC, contexto total 2.048 tokens e saída máxima 160 tokens. Não carregar um contexto KV permanente por NPC. Oito NPCs são teste de estresse; admissão de novas tarefas pode ser recusada para manter o mesmo teto. Mais jogadores/NPCs simultâneos exigem novo perfil, não multiplicação automática de processos.

Limitar os índices por custo estimado e validar estimador com heap profiling; compartilhar IDs, consultar receitas sob demanda, usar cache com descarte e snapshots pequenos. Na reconstrução após reload, considerar simultaneamente versão anterior e nova dentro do teto; se necessário pausar planejamento, liberar a anterior e reconstruir em lotes. Memórias podem partir de limites de 32 eventos recentes por NPC, resumo de 2 KB e cache global de 128 respostas, sempre subordinados ao orçamento global.

Monitorar ocupação estimada própria e processo de IA. Usar sinal de pressão do host apenas para reduzir serviço, não para afirmar que toda memória Java pertence ao mod. Em 850 MB adicionais sustentados, limpar caches, reduzir contexto futuro e suspender novas inferências; em 1.000 MB sustentados, descarregar o modelo dedicado após concluir/cancelar com segurança e voltar a diálogos determinísticos. Um pico transitório pode chegar a 2.000 MB somente nos cenários aprovados de partida, reload ou troca de perfil. Nunca encerrar runtime compartilhado do usuário: controlar apenas processo dedicado iniciado pelo mod; para runtime externo, suspender envios.

O modo sem modelo mantém tarefas e mensagens predefinidas bilíngues, com consumo próprio a medir; conversa generativa fica indisponível. API remota não será acionada automaticamente por falta de memória. GPU offload é otimização opcional: VRAM dedicada é relatada à parte, RAM compartilhada continua relevante e nenhuma estimativa assume que a RAM desaparece.

### Protocolo de comprovação

1. Baseline A: pack sem o mod, mesmo save de teste, cena, duração, distância de renderização/simulação, Java, heap, mods, cliente/servidor e hardware.
2. B: pack com mod, sem provedor, nos mesmos cenários com 1/4/8 NPCs. C: mod e inferência local sob carga de pedidos limitada.
3. Após cinco minutos de aquecimento, observar trinta minutos e repetir três vezes por perfil representativo. Medir também inicialização fria, carregamento do modelo, reload, dimensão e retomada; não descartar picos da abertura.
4. Registrar working set/RSS dos processos (incluindo páginas residentes do modelo), memória privada/commit, heap/GC, RAM compartilhada e VRAM dedicada separadamente. No Windows, não usar apenas PrivateMemorySize para ignorar arquivos mapeados; páginas compartilhadas podem exigir ajuste documentado para evitar dupla contagem.
5. Estimar custo incremental de forma conservadora por máximos: soma dos deltas não negativos de pico de processos de jogo contra A, mais pico dos processos exclusivos de IA/supervisão. Guardar curvas, método e incerteza do A/B; variação do mundo não é uma medida exata da autoria de cada alocação.
6. Aprovar somente se o perfil comum incremental ficar ≤1.000 MB, o pico transitório observado ficar ≤2.000 MB e não houver crescimento contínuo. Perfil comum significa quatro NPCs, modelo já carregado, painel aberto alternadamente e uma geração curta por vez. Arquivo GGUF pequeno ou heap médio pequeno não é evidência suficiente.

Ferramentas de medição: amostrador do sistema operacional para processos/RAM e telemetria de ticks no mod. Heap/GC por ferramentas da JVM disponíveis em cada Java; não tornar um profiler específico dependência de produção. Revalidar ao mudar modelo, quantização, runtime, driver, contexto, catálogo ou número de NPCs.

## 2. Matriz de compilação e distribuição

| Alvo | Runtime/bytecode do mod | Build proposto | Validação prevista |
| --- | --- | --- | --- |
| Forge 1.20.1 | Java 17 | MDK oficial + ForgeGradle e wrapper fornecidos pelo MDK, versões fixas | Forge 47.4.0 e 47.4.10, Society e Cozy |
| NeoForge 1.21.1 | Java 21 | Template oficial + ModDevGradle, confirmar e fixar revisão do template | NeoForge 21.1.248, Tempest e Tensura |
| Forge 1.21.1 | Java 21 | MDK Forge específico, build independente do NeoForge | Instância limpa; nenhuma instância Forge 1.21.1 local encontrada |
| Forge 1.12.2 | Java 8 | Avaliar ForgeDevEnv/RetroFuturaGradle específico para 1.12.2, fixar revisão após smoke test | Forge 14.23.5.x; build exato e pack a definir |

Fontes de Java e ambiente: [Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/), [Forge 1.21](https://docs.minecraftforge.net/en/1.21.x/gettingstarted/) e [NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gettingstarted/). Não atualizar os packs para acomodar o ambiente de desenvolvimento. A compatibilidade mínima 47.4.0 só poderá ser declarada após testes nas duas versões.

Uma descoberta do legado: o [ForgeDevEnv atual](https://github.com/CleanroomMC/ForgeDevEnv) declara Java 25 para executar Gradle 9.7.0 e RFG 2.0.3, enquanto seu build configura Java 8 para compilação padrão/testes do mod. JVM de build e runtime do Minecraft são distintos. O template cita Forge 14.23.5.2847; não adotá-lo silenciosamente como versão do pack. O README geral do RFG enfatiza 1.7.10: usar o template específico 1.12.2 e validar o JAR no Forge comum. Versões observadas são evidência da consulta, não ambiente já validado.

IDE: IntelliJ IDEA com integração Gradle; plugin Minecraft Development opcional e compatível com a ferramenta escolhida. Usar Git, wrappers versionados, dependências fixas e checksums dos artefatos finais. Build/execução fora do OneDrive, por exemplo `C:/Users/thyag/Projects/minecraft-companheiros` (caminho proposto, não criado). Documentação interna permanece no Brain.

## 3. Organização do código

Estrutura proposta, criada progressivamente:

```text
minecraft-companheiros/
  core/                         # Java 8 puro: domínio, tarefas, planos e contratos
  platforms/forge-1.20.1/        # primeiro adaptador completo, wrapper próprio
  platforms/neoforge-1.21.1/     # segundo adaptador, wrapper próprio
  platforms/forge-1.21.1/        # alvo Forge solicitado, separado
  platforms/forge-1.12.2/        # toolchain legado isolado
  assets-source/                # traduções e arte próprias, fonte comum
  test-fixtures/                # pequenos catálogos/quests de teste, sem saves pessoais
```

Cada plataforma empacota o core no próprio JAR. O core não importa Minecraft, Forge, FTB, cliente HTTP ou renderização. Compilá-lo com alvo Java 8 e testes sob Java 8/17/21 evita records, APIs recentes e dependências incompatíveis com o legado. Os adaptadores modernos podem usar recursos de Java 17/21. Não colocar todos os plugins Gradle incompatíveis num único build; consumir o core por artefato local versionado ou composição de build quando comprovadamente suportada. Evitar Architectury como dependência inicial: não elimina o salto para 1.12.2 nem existe necessidade de Fabric neste escopo.

Contratos propostos:

| Contrato | Responsabilidade |
| --- | --- |
| `CompanionState` | UUID, dono, modo, personalidade, tarefa e versão de estado |
| `WorldSnapshot` | Cópia imutável e limitada do mundo conhecido, inventários autorizados e riscos |
| `RecipeCatalog` | Receitas suportadas, alternativas, ingredientes e estações |
| `QuestAccess` | Objetivos visíveis, dependências e progresso atual da equipe |
| `PermissionService` | Responder permitir/negar/desconhecido para uma ação concreta |
| `TaskPlanner` | Produzir passos com pré-condições e resultado esperado |
| `ActionExecutor` | Executar um passo na thread do servidor e retornar resultado observado |
| `DialogueProvider` | Resposta textual e proposta de intenção, nunca acesso direto ao mundo |
| `MemoryStore` | Fatos tipados e resumos limitados por mundo/dono/NPC |
| `LocaleService` | Idioma da conversa, catálogos e resolução de rótulos |

Fluxo: pedido do jogador → contexto autorizado → intenção → plano verificável → ação no servidor → observação → progresso e resposta. Respostas assíncronas carregam `requestId`, versão de contexto, NPC e dono; resultados antigos são descartados depois de cancelamento, morte, desconexão ou troca de dimensão.

## 4. Entidade, movimento e decisões

Primeiro alvo: entidade própria baseada em `PathfinderMob`, usando navegação e `GoalSelector` do jogo. Adaptar para as classes equivalentes de IA do 1.12.2, como `EntityCreature`/`EntityAIBase`, sem vazá-las para o core. Usar UUID de dono, alianças e regras explícitas de alvo; NPC não entra como conta real e não ocupa sessão autenticada de jogador.

Combinar prioridades determinísticas com máquina de estados por tarefa: `QUEUED`, `RUNNING`, `BLOCKED`, `COMPLETED`, `CANCELLED`. Prioridade inicial: perigo imediato → sobreviver → ordem do dono → tarefa → atividade ociosa. Planejador de tarefas hierárquicas próprio e pequeno; evitar biblioteca de IA adicional até medir uma necessidade concreta. A personalidade altera pesos e falas, sem liberar ações proibidas.

Navegação com destinos próximos, detecção de falta de progresso, limite de tentativas e retorno explicado ao dono. Não prometer pathfinding global nem descoberta de minérios ocultos. Observações vêm de alcance configurado, linha de visão e locais previamente conhecidos. Travessia complexa, barcos, voo e portais são tarefas futuras. Teleporte de recuperação só em modo configurado e em destino validado; não atravessar proteção de terreno.

Agendar observação por grupos de NPCs e replanejar por evento. Ponto de partida experimental: percepção a cada 10 ticks, replanejamento no máximo a cada 20 ticks por NPC e limite global de buscas de caminho. Movimento/combate continuam no mecanismo de tick do jogo. Esses valores são configurações iniciais para profiling, não metas comprovadas.

## 5. Ações e inventário

Inventário próprio em NBT, com adaptadores de item handlers/capabilities por loader. Primeiro implementar recolher itens, equipar, comer, entregar e depositar em baú explicitamente autorizado. Consultar inserção/extração simulada quando a API permitir, recalcular capacidade e aplicar mudanças na thread do servidor. Não armazenar um segundo inventário completo na memória social.

Minerar e cortar madeira exigem distância, ferramenta, tempo de quebra, desgaste, evento de proteção, drops e espaço de inventário. Um adaptador de FakePlayer pode ser necessário para interações que exigem jogador; sua identidade e relação com o dono precisam de testes com cada proteção, sem privilégios de operador. Eventos não garantem por si só que todos os mods de claims tratem o NPC corretamente.

Crafting inicial só para famílias implementadas: bancada shaped/shapeless e depois forno. Considerar quantidade produzida, ingredientes alternativos, recipientes restantes, NBT/componentes, combustível, estação e regras de desbloqueio. Receitas especiais, saídas dinâmicas e máquinas usam adaptadores; reconhecer uma receita não autoriza fabricar por simples criação do item.

Cada comando tem ID para suprimir execução duplicada; cada passo revalida recursos antes de agir. Interrupção e reinício exigem reconciliação pelo estado real. Não declarar atomicidade de transferências entre chunks no caso de queda abrupta: testar crashes e documentar limites do sistema de save, sem recriar itens por repetição cega de um plano salvo.

## 6. Conhecimento adaptativo do modpack

Usar registros, tags e receitas efetivas do servidor. Em 1.20.1, extrair dados via `RecipeManager`/tipos de receita e converter para DTOs do core. KubeJS modifica receitas através de eventos; seu efeito carregado é o que interessa ao NPC. [Documentação KubeJS](https://kubejs.com/wiki/tutorials/recipes).

Não inferir o estado do jogo apenas lendo scripts: eventos podem depender de jogador, horário, estágio ou condições de execução. Adaptadores registram condições conhecidas; quando uma condição é desconhecida, o plano deve parar ou orientar o jogador. No legado, usar registros de receitas e OreDictionary através de outro adaptador, sem pressupor datapacks/tags modernos.

Índice inicial em memória, por ID de saída, tags e nomes/aliases nos dois idiomas; expansão limitada do grafo de receitas, com detecção de ciclos e preferência por materiais/estações disponíveis. Não começar com banco vetorial, embeddings, LangChain ou treinamento de modelos. Recuperação de fatos relevantes antes do prompt é suficiente para testar a primeira versão.

Identificar o catálogo por versões de mods, revisão de receitas/configurações relevantes e versão do adaptador. Após reload, reconstruir snapshot e invalidar planos afetados. Conhecimento observado guarda origem, dimensão, posição, instante e validade. Atualizações de pack não podem preservar receitas antigas como verdades.

## 7. Integração com quests

Prioridade: FTB Quests, presente em três packs inspecionados. Integração opcional, carregada apenas com dependência compatível. Usar APIs/classes do mod no servidor; SNBT serve para auditoria e fixtures, não para reescrever progresso de um save aberto.

Código-fonte consultado em 16/09/2026 identificou `TeamData.get(Player)`, `getProgress(Task)`, `canStartTasks(Quest)`, `Quest.isVisible(TeamData)`, `Quest.getTasks()` e `ItemTask.getMaxProgress()/getItemStack()`. São pontos candidatos, a compilar e testar contra os JARs instalados: FTB Quests 2001.4.17, 2101.1.33 e 2101.1.34. `ServerQuestFile.INSTANCE` existe nas duas linhas consultadas; 1.21.1 também oferece `getInstance()`. Não assumir API estável entre versões.

Referências de implementação: [linha 1.20.1](https://github.com/FTBTeam/FTB-Quests/tree/a74ee04bf80e7fba2c8475112a07ace4a4abd0a6) e [linha 1.21.1](https://github.com/FTBTeam/FTB-Quests/tree/8c53f35a97d8897c861b097f1e39f4fc3beb3a15). Esses commits identificam o código consultado, não necessariamente o código dos JARs locais.

Tratar separadamente visibilidade, desbloqueio, condição de início e conclusão. Respeitar progresso em equipe e não revelar quests ocultas no prompt. Primeira cobertura: tarefas de item reconhecidas. Checkmarks são confirmação humana; estatísticas, kills, localização, crafting e tarefas customizadas exigem regras próprias de crédito. O NPC não recebe automaticamente os eventos atribuídos ao jogador.

Cenário real encontrado no Society: quest `5E5E6E09E6C6DF02`, tarefa `6062F75BE9F93639`, exige `botania:apothecary_default`; `botania:lexicon` é recompensa, não requisito. Usar inicialmente como fixture de interpretação, não como promessa de primeira quest executável. Escolher em jogo uma quest acessível e uma receita suportada para a demonstração de coleta.

O NPC ajuda a obter/entregar recursos e consulta o progresso novamente. Nunca chama comandos de completar quest ou emitir recompensas. Cancelamento do jogador interrompe as subtarefas, sem apagar progresso legítimo.

## 8. Inglês e português brasileiro

Idiomas obrigatórios: `en_us` e `pt_br`. Interface acompanha o idioma do cliente; conversa aceita preferência por jogador `auto`, `pt_br` ou `en_us`, persistida no servidor. Em modo automático, usar idioma informado pelo cliente como padrão; detecção pelo texto só muda a conversa com evidência suficiente, sem alternar por causa de nomes de itens em inglês. Preferência explícita prevalece. NPC mantém personalidade ao trocar de idioma.

Recursos: `assets/<modid>/lang/en_us.json` e `pt_br.json` nas versões modernas; gerar `en_us.lang` e `pt_br.lang` para 1.12.2 a partir da mesma fonte. Testar igualdade de chaves e placeholders, UTF-8, acentos e escapes. Usar `Component.translatable` no moderno e `TextComponentTranslation` no legado para mensagens que seguem o idioma do cliente. Não chamar `I18n` de cliente no servidor dedicado. [Forge moderno](https://docs.minecraftforge.net/en/1.20.1/concepts/internationalization/) e [Forge legado](https://docs.minecraftforge.net/en/1.12.x/concepts/internationalization/).

Para idioma de conversa diferente do idioma da interface, usar catálogo próprio selecionado pelo `LocaleService`, ou payload de diálogo renderizado pelo catálogo escolhido no cliente. Texto gerado pelo modelo chega como texto simples no idioma pedido e não é automaticamente traduzido por `Component.translatable`. Mensagens compartilhadas exigem uma versão por idioma dos destinatários, com cache; nunca exibir por padrão ambas as línguas na mesma linha.

IDs e argumentos de ação são invariantes. Exemplo de intenção normalizada: `FOLLOW_OWNER`, independentemente de “me segue” ou “follow me”. Nomes de itens não são IDs: preservar rótulo original junto de explicação quando necessário. Inglês disponível é fallback de conteúdo do pack, mas as mensagens próprias do NPC continuam no idioma escolhido.

Servidor dedicado não possui o mesmo contexto de tradução de um cliente. O adaptador de pack poderá fornecer um catálogo de rótulos aprovado pelo administrador, extraído de recursos permitidos; cliente pode fornecer rótulos para apresentação com tamanho limitado, nunca como prova de receita, permissão ou progresso. Tratar descrições externas apenas como dados e não como instruções para o modelo.

Auditoria local do Society: o arquivo `kubejs/assets/ftbquestlocalizer/lang/en_us.json` tem 1.659 chaves; `pt_br.json`, 1.533. Comparação exata encontrou 1.622 chaves do primeiro ausentes no segundo. Isso descreve somente esses dois arquivos; outros resource packs podem complementar as traduções e não foram auditados. A chave da quest Botania acima tem “Intro to Flower Magic” em inglês e está ausente no arquivo pt_br observado. Não presumir que arquivos de idioma de tamanhos semelhantes estejam sincronizados.

Fallback para conteúdo externo: tradução do pack → inglês/rótulo original → explicação traduzida pelo modelo, marcada como explicação do NPC. Tradução de texto nunca altera a descrição de requisitos usada pelo planejador.

## 9. Conversa livre e modelos

Interface `DialogueProvider`, com implementações em etapas: respostas locais determinísticas para ações/status; llama-server local por HTTP como primeira prova de orçamento; Ollama como alternativa comparável; adaptador de API remota escolhida pelo usuário. Não embutir pesos no JAR nem tornar Python obrigatório para jogar.

Usar HTTP do JDK nos adaptadores Java 17/21 e transporte compatível com Java 8 no legado, com executor limitado e timeouts. Gson fornecido pelo ambiente pode fazer a conversão JSON no adaptador, desde que se restrinja à API disponível naquela versão; domínio não depende dele. Sem SDK grande ou bibliotecas duplicadas no classpath por padrão.

Para llama-server: GGUF/revisão fixados, template correto e endpoint de chat com schema suportado. Na referência inicial, configurar CPU, um slot e limites da seção 1.1; ativar GPU somente em perfil medido. Para Ollama local: `POST /api/chat`, inicialmente `stream:false`, prompt com idioma, personalidade, fatos recuperados e intenções permitidas; usar `format` com JSON Schema e validação local dos campos/IDs. Tool calling é uma alternativa de integração posterior, não é necessário combinar as duas abordagens no protótipo. A documentação diferencia esse suporte do Ollama Cloud, que na consulta não oferece structured outputs. [Structured outputs](https://docs.ollama.com/capabilities/structured-outputs), [tool calling](https://docs.ollama.com/capabilities/tool-calling).

Contrato de resposta proposto:

```json
{
  "schemaVersion": 1,
  "locale": "pt_br",
  "speech": "Vou verificar o que falta para essa missão.",
  "intent": {"type": "ASSIST_SELECTED_QUEST"}
}
```

Quest selecionada, dono e permissões vêm da sessão no servidor, não de UUIDs sugeridos pelo modelo. O interpretador só aceita enums e parâmetros limitados. Depois de validar, o planejador consulta novamente o mundo. Separar fala social de relatório de ação: “concluí” só pode ser anunciado a partir de resultado confirmado pelo executor, com template; o modelo não decide o status da tarefa.

Não fazer inferência por tick. Chamadas por mensagem dirigida ao NPC ou evento social significativo, fila limitada, cancelamento e descarte de respostas antigas. Falha ou JSON inválido causa fallback no idioma escolhido. Retornar aviso local imediatamente enquanto a resposta está em processamento. Chat dos demais jogadores não é enviado por padrão; API/chave fica no servidor, fora de saves exportáveis e logs.

Candidatos prioritários: Qwen3-0.6B com thinking desativado e Qwen2.5-0.5B-Instruct, em quantização compatível a fixar. Qwen2.5-1.5B é comparação opcional se a memória completa couber; os 2 GB não são uma cota exclusiva para seus pesos. Não há vencedor medido nem garantia de fluência pt-BR. [Qwen3](https://huggingface.co/Qwen/Qwen3-0.6B) e [Qwen2.5](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct).

FunctionGemma pode ser avaliado futuramente como roteador especializado; sua ficha não o destina a diálogo direto. SmolLM2 tem foco em inglês; LFM2-700M não declara português entre os idiomas suportados. Gemma 270M IT e Biatron-345M são alternativas de pesquisa, sem aprovação de qualidade/memória para este produto. Matriz e fontes em [[../03_context/minecraft-companheiros-pesquisa-ia-e-runtimes]].

A pesquisa da mosca distingue mapa do conectoma masculino Google/Janelia, FlyWire e simulação FlyBody. Não incorporar essas estruturas ao runtime do mod. Adotar três camadas por decisão de engenharia: controle Java, planejamento sobre fatos e linguagem assíncrona. Trabalho assíncrono evita esperar HTTP no tick, mas ainda pode disputar CPU/RAM com o jogo; por isso não assegura 20 TPS ou o teto de memória sozinho.

A interface HTTP permite escolher runtime/backend, sem vincular o mod a fabricante de GPU. CPU, CUDA e Vulkan são perfis a validar; não afirmar suporte a DirectML, ROCm ou toda GPU sem confirmar o runtime e driver escolhidos. Hardware local observado: 16 GB de RAM e RX 6650 XT; desempenho e memória livre não medidos. Sem runtime, mantém-se o modo determinístico de capacidade linguística limitada e custo Java real. Uma instância de modelo atende todos os NPCs.

## 10. Memória, persistência e multiplayer

NBT da entidade: equipamento, inventário, dono, modo e estado mínimo da tarefa. `SavedData` nas versões modernas e `WorldSavedData` no legado: preferências, vínculos e índice de companheiros por mundo. Usar armazenamento global do mundo principal para dados entre dimensões; não salvar inventário em duas fontes. [SavedData Forge](https://docs.minecraftforge.net/en/1.20.1/datastorage/saveddata/).

Memória tipada: base autorizada, preferência de idioma, tarefa recente, receita conhecida/revisão e pequeno resumo social. Limites configuráveis de eventos e texto, expiração de localizações e ação de limpar memória. Fatos novos não podem sobrescrever silenciosamente identidade/dono. Migrações com `schemaVersion` e backup de teste; dados de versão futura devem ser preservados e gerar erro claro.

Rede: `SimpleChannel` no Forge 1.20.1; `CustomPacketPayload`, `StreamCodec` e registro de payloads no NeoForge 1.21.1; `SimpleNetworkWrapper` no legado. Forge 1.21.1 terá adaptador próprio validado no MDK escolhido. [Forge SimpleImpl](https://docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/), [NeoForge payloads](https://docs.neoforged.net/docs/1.21.1/networking/payload/).

Pacotes propostos: preferência de idioma, ordem ao NPC, seleção de quest, estado resumido de tarefa e resposta de chat. Validar tamanho, frequência, dono e acesso no servidor. Inventários abrem por menus do jogo. HTTP e cálculos sobre snapshots podem executar fora da thread principal; acesso e mutação de mundo retornam à thread do servidor.

Mod instalado no cliente e no servidor, inclusive quando singleplayer usa servidor integrado. Não prometer clientes vanilla. Conversa dirigida por tela do companheiro, comando ou canal identificado; NPC envia mensagem identificada como NPC/sistema, sem simular assinatura de chat de jogador humano.

## 11. Integrações, visual e escolhas adiadas

Proteções: adaptadores para FTB Chunks e Open Parties and Claims; compor todas as proteções aplicáveis com negação prevalecendo. Desconhecido bloqueia ação destrutiva e explica motivo. Validar construção, quebra, baús, ataque e recuperação/teleporte separadamente. [Código FTB Chunks](https://github.com/FTBTeam/FTB-Chunks/tree/1.20.1/main) e [Javadoc OPAC](https://thexaero.github.io/open-parties-and-claims/javadoc/). Javadoc pode representar versão mais recente: contrato precisa ser conferido no 0.30.3 local.

Curios, Sophisticated Storage, MineColonies e Tensura são expansões. Começar com slots comuns e inventários expostos, sem assumir que usar um item ativa habilidade de jogador. Integração MineColonies deve respeitar colônia/permissões; a proposta não substitui trabalhadores. Progresso de skills Tensura requer API e testes próprios.

Visual: modelo humanoide e renderer nativos, skins próprias e animações simples no MVP. Blockbench para autoria de modelos. GeckoLib apenas quando animações esqueléticas justificarem a dependência; versões já instaladas diferem entre packs. Não introduzir AzureLib junto, nem prometer o mesmo runtime GeckoLib para 1.12.2. [Wiki GeckoLib](https://github.com/bernie-g/geckolib/wiki).

Referência comunitária primária: [Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) apresenta companheiros com tarefas e múltiplas versões; há [exemplo de extensão Forge 1.20.1](https://github.com/TartaricAcid/TLMAdditionExample). Comparar custo de uma extensão com entidade própria durante a fase zero. Recomendação atual: mod próprio pelo controle sobre UX, idiomas e contratos, usando a referência para estudar padrões. Código e assets possuem licenças distintas; não copiar recursos visuais como se fossem código MIT. Nenhuma dependência nesse mod foi adotada.

Referências de automação: estudar custos de movimentos do [Baritone](https://github.com/cabaletta/baritone) e decomposição/interrupção de tarefas do [AltoClef](https://github.com/gaucho-matrero/altoclef). Começar com navegação vanilla; só implementar busca própria se falhas medidas justificarem. Se houver worker de pathfinding, ele calcula sobre snapshot imutável obtido na thread do servidor; resultado é revalidado e aplicado nessa thread. Não acessar mundo mutável no worker nem descrever o cálculo como assíncrono na mesma thread. Reutilização de código exige revisão de licença.

## 12. Verificação e metas propostas

JUnit 5 em versão compatível com Java 8 para core/contratos; fixar versão antes do build. A linha 5 mantém baseline diferente da 6, conforme [documentação do JUnit](https://github.com/junit-team/junit-framework/wiki/JUnit-Java-Baselines). GameTest para cenários de mundo modernos, cliente/servidor dedicado para integração, e harness/comandos de teste em mundo controlado no legado, que não tem o mesmo GameTest moderno. [GameTest Forge](https://docs.minecraftforge.net/en/1.20.1/misc/gametest/).

Casos obrigatórios: caminho impossível; inventário cheio; drops e ferramentas; ordem duplicada; cancelamento durante resposta de IA; reload alterando receita; quest oculta/equipe; negar claim; morte/desconexão/dimensão/save/reabertura; dependência opcional ausente; dois jogadores falando idiomas diferentes; provedor indisponível. Testes de crafting incluem recipientes e saídas variáveis que devem ser rejeitadas quando não suportadas.

Dataset bilíngue proposto: 50 enunciados por idioma em pares equivalentes, total de 100 enunciados de avaliação reservados, além dos exemplos de desenvolvimento separados. Incluir acentos ausentes, erros comuns, nomes de itens em inglês e pedidos ambíguos. Meta inicial ≥95% de intenção correta nas ações suportadas em cada idioma, com denominador e falhas registrados; todas as tentativas inválidas da suíte devem ser rejeitadas pelo servidor. Chaves e placeholders dos textos próprios: 100% pareados. Esses são critérios propostos, não resultados medidos.

Desempenho: comparar mesma cena e configurações com 0, 1, 4 e 8 NPCs; coletar p50/p95 de MSPT, heap, GC, tempo de planejamento e fila do chat, com/sem inferência. Meta preliminar de engenharia: acréscimo p95 de até 5 ms com quatro NPCs em cenário controlado, sem I/O bloqueante no tick. Avaliar latência de chat separadamente; escolher modelo só depois do teste com pack aberto. Meta provisória de resposta curta: p95 até 10 s com um pedido ativo e sem fila; esse critério pode reprovar o modelo sem afetar as ações determinísticas. Registrar espera em fila separada. O teto de 2.000 MB da seção 1.1 é requisito, não deve ser relaxado automaticamente se um pack/modelo falhar.

Release exige matriz de builds, JAR correto por loader/versão, teste de servidor dedicado, changelog de capacidades realmente suportadas, instruções de backup e incompatibilidades conhecidas. Não redistribuir saves, chaves, modpacks completos ou pesos como parte do mod.

## 13. Estado e próximos passos

Pesquisa realizada em 16/09/2026 com documentação oficial, repositórios dos mantenedores e leitura local. Fontes web de capabilities NeoForge não carregaram nesta consulta; contratos exatos desse adaptador continuam pendentes, sem bloquear o desenho por interfaces. Não foram instalados JDKs, modelos, bibliotecas ou mods.

Revisão posterior às alterações do usuário: preservados arquitetura híbrida e modelos pequenos; removidas garantias não medidas de RAM/TPS e corrigido o fluxo de threads. Orçamento, modelos e critérios estão alinhados com a pesquisa revisada.

Próximo responsável: assistente, quando iniciada a implementação, executa P0 do plano, mede candidatos com orçamento reservado para o mod, fixa ferramentas e verifica APIs contra versões reais. Usuário ainda precisa identificar o pack 1.12.2 antes dos testes de integração desse alvo; isso não impede o protótipo 1.20.1. Arquivos de desenvolvimento podem ser criados no checkout isolado proposto; instâncias originais e saves pessoais não são ambientes de teste.
