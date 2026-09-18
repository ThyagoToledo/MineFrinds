---
tags: [minecraft, design, interface, skins, assets, npc, acessibilidade]
updated: 2026-09-16
status: especificacao-visual-proposta
---

# Companheiros Minecraft — interface, skins e assets

## Objetivo visual

O companheiro deve parecer parte do Minecraft, comunicar estado sem poluir a tela e continuar barato em packs pesados. O MVP usa o modelo humanoide nativo, skins 64×64 e animações vanilla. Isso evita uma dependência 3D no primeiro marco, permite armaduras e itens de qualquer mod e reduz diferenças entre Forge 1.12.2, Forge 1.20.1 e Forge/NeoForge 1.21.1.

Direção: aventura acolhedora e funcional. Grafite e pergaminho formam a base; turquesa identifica ajuda, âmbar indica bloqueio, vermelho indica perigo e verde confirma conclusão. Estado também aparece por ícone, texto e forma, nunca apenas por cor. Fontes, botões e tooltips seguem o estilo e a escala de GUI do jogo.

## Pacote inicial de personagens

Criar quatro identidades originais, cada uma com variante de braços clássicos e slim quando necessário:

| Arquétipo | Leitura visual | Papel inicial | Acento |
| --- | --- | --- | --- |
| Explorador | mochila, lenço e roupa de campo | seguir, buscar e reconhecer caminhos | turquesa |
| Guardião | couro reforçado e ombreiras pintadas na skin | defender e escoltar | vermelho queimado |
| Artesão | avental, luvas e bolsos | coletar, fabricar e organizar | âmbar |
| Guia | caderno, faixa e símbolos discretos | quests, receitas e explicações | violeta |

As roupas da skin não podem simular equipamento que o NPC não possui. A camada externa da skin serve para cabelo, jaqueta, bolsas planas e pequenos detalhes. Armadura real permanece renderizada pelo jogo. Cor de sotaque pode variar sem duplicar toda a textura.

### Fluxo de produção das skins

1. Gerar apenas concept art frontal/traseira para definir personalidade, roupa e paleta; geração de imagem não produz UV de skin confiável.
2. Desenhar a skin manualmente no editor de skins do Blockbench, em pixel art e com nearest-neighbor.
3. Conferir cabeça, braços, pernas, camada externa, espelhamento, variante slim/classic e transparência indevida.
4. Testar ao sol, à noite, com armadura completa, item nas duas mãos e escalas de GUI 2–4.
5. Guardar fonte editável em `assets-source/characters/<id>/`, PNG final e arquivo de licença/autoria.

## Inventário de assets do MVP

| Asset | Formato/alvo | Uso |
| --- | --- | --- |
| 4 skins originais | PNG 64×64 | personagens iniciais |
| retratos | recorte gerado da própria skin/modelo | lista de NPCs e cabeçalho |
| ícones de estado | PNG 16×16 | seguindo, esperando, trabalhando, bloqueado, perigo e offline |
| ícones de ordem | PNG 16×16 ou 32×32 | ações rápidas e roda de comandos |
| atlas de interface | PNG pequeno, nine-slice quando disponível | painéis, seleção e estados de botão |
| item de vínculo | textura 16×16 + modelo JSON vanilla | abrir painel/selecionar companheiro |
| marcadores | sprites pequenos | base, área de trabalho e destino |
| sons | OGG mono, curtos | ordem aceita, bloqueio, conclusão e alerta |
| idiomas | `pt_br` e `en_us` na mesma fonte de chaves | interface, tooltips e respostas determinísticas |

Meta de arte carregada: até 16 MB de texturas e sons próprios residentes no perfil comum. Não usar texturas 4K, vídeo, voz ou animações pré-renderizadas. O build valida dimensões, nomes, chaves de idioma e arquivos órfãos.

## Quando usar 3D e animação

O NPC não precisa de modelo 3D próprio no MVP. A geometria vanilla já oferece cabeça móvel, caminhada, ataque, uso de item, armadura e compatibilidade ampla. Blockbench continua sendo a ferramenta de autoria para skins e para um eventual item/bloco de comando de baixo número de polígonos.

GeckoLib entra somente depois de P6 se uma animação medida melhorar claramente a experiência, por exemplo gestos sociais ou uma criatura não humanoide. Cada alvo precisa de versão e licença verificadas; 1.12.2 pode manter animação vanilla. Não carregar GeckoLib e AzureLib juntos. Modelos Bedrock `.geo.json` e controladores Molang exigem adaptação e teste, não cópia automática.

## Painel do companheiro

Abrir pelo item de vínculo, tecla configurável ou interação no NPC. O servidor fornece um snapshot limitado; o cliente nunca decide ações. Em 1.20.1, usar `Menu`/`AbstractContainerScreen` e abrir pelo fluxo de menu do Forge; em 1.21.1 usar o equivalente do loader; em 1.12.2, `Container`/`GuiContainer` e rede própria. Toda ordem vira o mesmo `CommandIntent` usado pelo chat e recebe validação de dono, distância, estado e permissões no servidor.

```text
┌ Companheiros ───────┬ Nara · Artesã ───────────────────┬ Ordens rápidas ┐
│ retrato  Nara   18♥ │ [Visão] [Ordens] [Inventário]    │ Seguir         │
│ trabalhando   24 m │ [Quests] [Memória] [Aparência]   │ Esperar        │
│ retrato  Ivo    20♥ │                                   │ Defender        │
│ esperando      8 m │ tarefa: obter 12 toras  5/12      │ Parar           │
│                     │ bloqueio: ferramenta danificada   │ Voltar à base   │
│                     │ fila: entregar → guardar no baú   │ Chamar para cá  │
├─────────────────────┴───────────────────────────────────┴────────────────┤
│ Chat com Nara: [ escreva uma ordem ou mensagem…                  ] Enviar │
└──────────────────────────────────────────────────────────────────────────┘
```

Abas:

- **Visão geral:** vida, fome/energia se adotada, distância, dimensão, modo, equipamento, tarefa atual e motivo de bloqueio.
- **Ordens:** seguir, esperar, defender, recolher, entregar, trabalhar em área marcada, parar, voltar à base e chamar para perto.
- **Inventário:** slots reais, equipamento e transferências autorizadas; nenhuma cópia cliente.
- **Quests:** objetivo selecionado, itens restantes, dependências conhecidas, próximo passo e limitações.
- **Memória:** fatos recentes editáveis pelo dono, locais conhecidos e botão para limpar; sem mostrar raciocínio interno do modelo.
- **Aparência:** skin permitida, nome, pronome opcional, papel e cor de sotaque.
- **Configurações:** idioma da conversa, frequência de fala, combate, coleta, teleporte de recuperação e privacidade.

O painel mantém uma fila visível de no máximo cinco ordens; o executor pode limitar tarefas simultâneas. Cada botão mostra disponibilidade e motivo quando desativado. O chat aceita `@Nara me siga`, `@Nara follow me`, `@todos espere` e `@all stop`; aliases resolvem para IDs invariantes. Uma roda de ordens rápidas, acionada por tecla, reutiliza as mesmas intenções sem abrir o painel completo.

## Chamar para perto

“Chamar para perto” é uma solicitação ao servidor:

1. tentar navegar até o dono;
2. se travado, procurar posição segura no mesmo chunk ou em chunk já carregado;
3. teletransportar apenas se a opção estiver habilitada, o NPC não estiver em combate, o destino tiver piso e espaço, a proteção permitir e o cooldown tiver terminado;
4. caso contrário, explicar o bloqueio e oferecer voltar à base.

O MVP não força chunks nem teleporta entre dimensões. Travessia por portal pode virar tarefa posterior. A ordem é idempotente e não altera o inventário, o que evita duplicação durante reconexões.

## Ver pelos olhos do NPC

O modo padrão troca a câmera principal do cliente para o NPC enquanto o corpo do jogador permanece no mesmo lugar no servidor. Ele é apenas visual: o jogador não pilota o NPC. Um HUD compacto mostra nome, tarefa, vida, botão de retorno e aviso de que o corpo permanece vulnerável. `Esc`, a tecla de visão, morte/descarregamento do NPC, troca de dimensão ou negação do servidor encerram o modo.

Essa solução reutiliza a renderização principal e tem custo bem menor que desenhar dois mundos. Só funciona quando o cliente possui dados suficientes da área e nunca deve forçar chunks distantes. O servidor autoriza dono, dimensão, alcance configurado e política do pack.

Picture-in-picture é experimento posterior a P6: uma única janela de 320×180, 5–10 FPS, mesma dimensão e chunks já acompanhados pelo cliente, desligada por padrão. Ela precisa de segunda passagem para framebuffer, teste com shaders e orçamento transitório próprio; se causar regressão de FPS, RAM/VRAM ou compatibilidade, não entra na distribuição padrão.

## Mecânicas complementares leves

- base e área de trabalho marcadas pelo dono;
- perfis de função, como explorador, guarda, artesão e guia, sem modelo de IA separado;
- formação simples e distância de escolta;
- reserva de recurso por tarefa para dois NPCs não disputarem o mesmo item;
- pausa, cancelamento e botão de emergência sempre determinísticos;
- motivos tipados de bloqueio e sugestão de próximo passo;
- modo silencioso e limite de mensagens por minuto;
- afinidade social pequena, usada em falas e gestos, sem alterar permissões;
- status ping sob demanda em vez de varreduras contínuas;
- resumo compacto de sessão e memória com limites fixos.

## Critérios de aceite visual e funcional

- Todas as telas cabem em 16:9 e 4:3 nas escalas de GUI suportadas, com navegação por teclado e tooltips.
- Ícones permanecem compreensíveis sem depender de cor; pt-BR e en-US não cortam texto crítico.
- Abrir/fechar painel repetidamente não deixa menu, framebuffer ou listener retido.
- Ordem por botão e frase equivalente geram o mesmo `CommandIntent` e a mesma validação.
- Visão do NPC não move o jogador, não concede visão de chunk não enviado e sempre possui saída local.
- Skins funcionam com braços slim/classic, armaduras e itens; assets incluem autoria/licença.

## Referências técnicas

- [Blockbench oficial](https://blockbench.net/) e [repositório do Blockbench](https://github.com/JannisX11/blockbench).
- [Menus no Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gui/menus/) e [screens no Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gui/screens/).
- [Menus no NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gui/menus/).
- [Entidades no GeckoLib 4](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4)).

