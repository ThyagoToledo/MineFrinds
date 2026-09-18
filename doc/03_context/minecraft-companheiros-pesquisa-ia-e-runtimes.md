---
tags: [minecraft, ia, pesquisa, conectoma, slm, hardware, runtimes]
updated: 2026-09-16
status: pesquisa-revisada-benchmarks-pendentes
---

# Pesquisa de IA, runtimes e agentes para Companheiros Minecraft

Autor: ThyagoToledo

Revisão técnica em 16/09/2026. Mantém a direção proposta pelo usuário: arquitetura híbrida, modelos pequenos, independência de fabricante de GPU e conversação en-US/pt-BR. Distingue evidência publicada, recomendação de engenharia e desempenho ainda não medido.

Documentos canônicos: [[../00_spec/minecraft-companheiros-arquitetura-tecnica]] e [[../01_plan/minecraft-companheiros-plano]]. Não houve download de pesos, instalação, treinamento ou benchmark durante esta pesquisa.

## 1. O que provavelmente é a “mosca do Google”

Há projetos diferentes, que não devem ser tratados como um único modelo:

| Projeto | Evidência primária | Relevância para o mod |
| --- | --- | --- |
| Conectoma masculino Google Research/HHMI Janelia, anunciado em 03/09/2026 | Mapa de cérebro e sistema nervoso central com mais de 166 mil neurônios e 125 milhões de conexões sinápticas | Recurso de neurociência; não é um chatbot nem uma política pronta para Minecraft |
| FlyWire | Projeto de conectoma da mosca, distinto do mapa masculino acima | Estrutura biológica não equivale a capacidade treinada de seguir quests |
| FlyBody | Corpo de mosca no MuJoCo e tarefas de aprendizado por reforço para locomoção | Inspiração para separar percepção, controle e objetivos; sem adoção do simulador |
| Adaptações comunitárias para jogos | Demonstrações externas dependem de sensores, controles, regras de aprendizado e ambiente próprios | Não comprovam generalização, idiomas ou consumo sub-2 GB para nosso uso |

Fontes: [anúncio do Google](https://research.google/blog/a-connectomics-milestone-mapping-the-complete-male-fruit-fly-brain/), [FlyWire](https://flywire.ai/), [FlyBody dos mantenedores](https://github.com/TuragaLab/flybody) e [artigo de locomoção](https://www.nature.com/articles/s41586-025-09029-4).

A identificação do anúncio de setembro como referência do usuário é provável, não confirmada por um link dele. A notícia recente combina temporalmente com a descrição de pessoas ensinando jogos à mosca. Não atribuir ao Google todos os experimentos comunitários nem usar o número de neurônios para estimar RAM de uma simulação.

Conclusão de engenharia: não incorporar conectoma, MuJoCo ou treinamento por reforço ao MVP. O mod já tem acesso a entidades, inventários e eventos; navegação e tarefas em Java são a primeira implementação proposta. A arquitetura híbrida é uma escolha apoiada nas necessidades do produto, não uma consequência cientificamente “comprovada” pela biologia.

## 2. Modelos pequenos: seleção por função

Nenhum candidato foi medido nesta máquina. As fichas demonstram tamanho e intenção de uso; não demonstram fluência pt-BR, acerto de quests ou consumo final deste mod. Quantização é uma configuração experimental a fixar por arquivo, revisão e checksum.

| Candidato | Papel proposto | Limite e decisão |
| --- | --- | --- |
| Qwen3-0.6B, quantização Q4 compatível | Primeiro concorrente para intenção e conversa breve | Desativar thinking no template/runtime, contexto curto; precisa passar em pt-BR/en-US e RAM |
| Qwen2.5-0.5B-Instruct, Q4 compatível | Controle de comparação simples e pequeno | Família documenta português; qualidade brasileira e aderência a ações precisam de teste |
| Qwen2.5-1.5B-Instruct, Q4 compatível | Comparação de qualidade opcional | Só promover se couber no orçamento total; não é padrão previamente aprovado |
| Gemma 3 270M IT | Experimento de linguagem pequeno | Avaliar comandos curtos; pequeno tamanho não assegura conversa útil |
| FunctionGemma 270M | Roteamento de pedidos para funções específicas | Google indica que não é destinado a diálogo direto; possível especialização futura, não substituto automático do chat |
| SmolLM2-360M-Instruct | Comparação de baixo custo em inglês | Ficha indica predomínio de inglês; não atende sozinho ao requisito bilíngue sem evidência adicional |
| LFM2-700M | Comparação de arquitetura eficiente | Português não está entre os oito idiomas declarados na ficha consultada; não escolher como padrão bilíngue |
| Biatron-345M | Referência comunitária/acadêmica brasileira | Trabalho focado em pt-BR; inglês, instrução, ferramentas e runtime quantizado ainda não foram verificados |

Fontes primárias: [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B), [Qwen2.5-0.5B](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct), [Qwen2.5-1.5B](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct), [Gemma 270M IT](https://huggingface.co/google/gemma-3-270m-it), [FunctionGemma](https://ai.google.dev/gemma/docs/functiongemma/model_card), [SmolLM2](https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct), [LFM2](https://huggingface.co/LiquidAI/LFM2-700M) e [artigo Biatron](https://aclanthology.org/2026.propor-1.86/).

A lista é uma seleção orientada ao requisito, não ranking universal nem levantamento exaustivo dos modelos disponíveis. Prioridade de teste: Qwen3-0.6B e Qwen2.5-0.5B. Só abrir a avaliação dos demais se os dois falharem em qualidade ou se houver folga para um teste comparativo.

### Peso em disco não é RAM de inferência

Estimativa aritmética de referência: 0,6 bilhão de parâmetros × 4 bits / 8 corresponde a cerca de 300 MB de pesos idealizados. Isso exclui escalas de quantização, tensores em outra precisão, tokenizer, KV cache, buffers, bibliotecas e contexto. Não usar esse número como especificação de um GGUF real ou medição de RAM.

Memória e velocidade variam com runtime, CPU, GPU, driver, contexto, batch, paralelismo, cache e offload. Arquivos mapeados em memória e memória compartilhada da GPU também precisam ser observados. GPU offload não elimina automaticamente o uso de RAM.

## 3. Arquitetura híbrida recomendada

- Camada 0: prioridades, sobrevivência, locomoção e ações concretas em Java, dentro das regras do servidor.
- Camada 1: catálogo do pack, grafo de receitas, objetivos FTB e decomposição determinística de tarefas, com índices e snapshots limitados.
- Camada 2: modelo pequeno opcional para interpretar linguagem e dar personalidade à conversa. Intenção sai estruturada e passa pelo validador antes de virar plano.

Não usar o modelo para descobrir IDs por memória de treinamento, navegar por pixels ou decidir se uma quest está concluída. O resultado do mundo determina essas respostas. Uma instância de modelo e uma inferência ativa atendem todos os NPCs do perfil inicial.

O modo sem modelo mantém comandos suportados, tarefas e falas contextuais nos dois idiomas, mas não oferece a mesma liberdade de conversa de um modelo generativo. Ele também utiliza memória e CPU.

## 4. Runtimes e controle de recursos

Preferência para a primeira prova de orçamento: llama.cpp/llama-server como processo separado, GGUF fixado, CPU como referência e Vulkan/CUDA como perfis opcionais. O mod fala HTTP/JSON. Ollama é alternativa de instalação/operação a comparar, com o mesmo contrato e orçamento; não iniciar ambos simultaneamente.

Documentação: [llama-server](https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md), [builds e backends](https://github.com/ggml-org/llama.cpp/blob/master/docs/build.md), [gramáticas e subset de JSON Schema](https://github.com/ggml-org/llama.cpp/blob/master/grammars/README.md), [Ollama structured outputs](https://docs.ollama.com/capabilities/structured-outputs) e [hardware Ollama](https://docs.ollama.com/gpu).

Constrained decoding restringe a forma quando configurado e suportado; resposta pode ser interrompida/truncada e valores podem ser inválidos para o jogo. Validar parse, schema e semântica no servidor, sempre. O suporte é a um subconjunto do JSON Schema; testar o schema exato no runtime fixado.

Parâmetros iniciais propostos: um modelo residente, um slot, contexto total de 2.048 tokens incluindo saída, saída máxima de 160 tokens, no máximo oito pedidos na fila, até uma pendência por NPC e sem cache persistente de KV por NPC. Contexto grande deve ser resumido/selecionado, sem truncar silenciosamente o contrato das ações. Limites são hipóteses de configuração para o benchmark.

HTTP facilita a troca de backend, mas não garante compatibilidade com toda GPU/CPU. Registrar sistema, runtime, instruções CPU, driver e backend realmente usados. O perfil padrão deve funcionar sem GPU dedicada se passar nos testes CPU; não prometer tokens/s por fabricante. O orçamento está definido na especificação e inclui o mod, não só o processo de IA.

## 5. O que aprender da comunidade Minecraft

| Projeto | O que a fonte mostra | Aproveitamento proposto |
| --- | --- | --- |
| Voyager | Agente com currículo, biblioteca de habilidades e feedback de execução, usando LLM e infraestrutura Minecraft/Mineflayer | Biblioteca de tarefas verificadas e replanejamento por observação; sem execução de código gerado em runtime |
| Mindcraft | Agentes baseados em LLM e Mineflayer | Organização de diálogo, contexto e habilidades; integração Forge/NeoForge não presumida |
| STEVE-1 | Política de comportamento condicionada por texto/imagem, com VPT/MineCLIP e ambiente MineRL | Referência de pesquisa; não é o mesmo tipo de sistema Mineflayer e não é chatbot pronto para quests |
| Baritone | Automação e busca de caminhos para Minecraft | Estudar custos de movimento/perigos se navegação vanilla se mostrar insuficiente; sem incorporar como dependência inicial |
| AltoClef | Tarefas automatizadas apoiadas em Baritone | Decomposição e interrupção de tarefas; não assumir uma implementação acadêmica HTN específica |
| Touhou Little Maid | Companheiros, tarefas e exemplo de extensão | Referência mais próxima para UX/inventário e estudo de integrações nativas |

Fontes: [Voyager](https://github.com/MineDojo/Voyager), [Mindcraft](https://github.com/mindcraft-bots/mindcraft), [STEVE-1](https://github.com/Shalev-Lifshitz/STEVE-1), [Baritone](https://github.com/cabaletta/baritone), [AltoClef](https://github.com/gaucho-matrero/altoclef) e [Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid).

Não há evidência aqui para latência universal de 100–500 ms nem para declarar todos esses projetos impossíveis de adaptar a modpacks. A decisão de implementar entidade nativa decorre de acesso aos eventos/APIs do pack e do escopo, com menos componentes externos obrigatórios. Não copiar código ou assets sem verificar a licença aplicável.

Caso seja necessário pathfinding adicional: ler/copiar dados do mundo na thread do servidor; calcular sobre snapshot imutável no worker; revalidar e aplicar na thread do servidor. “Assíncrono na thread do servidor” não descreve esse fluxo corretamente. Antes disso, usar navegação vanilla com trabalho escalonado.

## 6. Fine-tuning, destilação e aprendizado

Preservar essas opções como pesquisa posterior. Nenhuma redução fixa de VRAM, tamanho de LoRA ou superioridade de método foi medida para este projeto.

Primeiro: modelo pronto + catálogo atualizado + exemplos curtos + validação. Se existir erro recorrente medido, criar conjunto de desenvolvimento e teste separados, avaliar ajuste supervisionado/LoRA ou destilação de intenções e comparar com o baseline. DPO/GRPO não são requisitos do MVP. Treinamento ocorre fora do jogo; seus recursos não se confundem com os 2 GB de execução.

Adaptação ao pack é atualização de fatos e planos. Guardar resultados/preferências em memória não significa retreinar pesos. Manter contratos de tarefa independentes do modelo permite futuramente trocar por classificador menor ou regras melhores sem reescrever a entidade.

## 7. Registro da revisão e evidência pendente

As versões anteriores deste documento e da especificação misturavam proposta com resultado. Esta revisão substitui:

- “20 TPS garantidos” por medição de MSPT com baseline e metas por perfil.
- “0 MB/0 ms sem IA” por modo sem inferência, com custo Java a medir.
- Faixas fixas de tokens/s, RAM/VRAM e ranking de fluência por candidatos e benchmark reproduzível.
- “100% JSON válido” por decoding restrito com validação e tratamento de interrupção.
- “proibido distribuir pesos pelo CurseForge” por decisão de produto de manter download separado; nenhuma regra dessa plataforma foi comprovada nesta pesquisa.
- Identificação única FlyWire/FlyBody por distinção entre conectomas, corpo simulado e experimentos comunitários.
- Generalização STEVE-1/Mineflayer por descrição separada dos projetos.
- Estimativa de LoRA de 15 MB e redução de 80% por decisões condicionadas a medição futura.

Essas correções preservam o objetivo das alterações do usuário: leveza, funcionamento bilíngue e arquitetura híbrida. Status atual: pesquisa documental revisada; integração, qualidade linguística e desempenho permanecem pendentes. Próxima evidência: relatório da fase P0 com perfil exato, memória de pico e taxa de acerto por idioma.
