---
tags: [minecraft, minefriends, r3, desempenho, arquitetura, observabilidade]
updated: 2026-09-19
status: parcialmente-iniciado
---

# R3 — Arquitetura, desempenho e operação

## Resultado esperado

Quatro NPCs operam normalmente e oito degradam de forma controlada, sem crescimento contínuo, travamentos ou classes monolíticas difíceis de testar.

## Decomposição

Dividir `CompanionServerPlayer` em serviços:

- `CompanionSensors`: observações limitadas e cacheadas;
- `MovementController`: seguir, permanecer, rota e recuperação;
- `CombatController`: alvos, defesa e cooldowns;
- `WorkController`: coleta, agricultura e plano atual;
- `InventoryController`: equipamento, transferências e transações;
- `CompanionLifecycle`: spawn, dismiss, morte, dimensão e shutdown;
- `CompanionPresenter`: mensagens e snapshots, sem lógica de mundo.

Cada serviço recebe interfaces pequenas; estado mutável do mundo permanece na thread do servidor.

## Orçamento de ticks

- Escalonar scans entre NPCs e impor limite global por tick.
- Não procurar madeira/minério/fazenda em cubo completo todo tick.
- Cache curto de alvo com invalidação por mudança/falha.
- Replanejar por evento ou intervalo configurável.
- Limitar pathfinding concorrente e nunca acessar mundo mutável em worker.

## I/O e recursos

- Tornar busca de skin assíncrona, limitada, cacheada e cancelável.
- Um runtime de IA, uma geração ativa e sem contexto KV por NPC.
- Fechar worker, conexão, cache e fake players no shutdown.
- Configurar limites de NPC, raios, frequências e filas.

## Observabilidade

Registrar contadores e histogramas limitados:

- duração/quantidade de scans e replanejamentos;
- ações permitidas/negadas e motivos;
- fila/latência/falhas de IA;
- snapshots enviados e descartados;
- heap estimado do mod, RSS do runtime e delta MSPT.

Logs usam request/task IDs e não guardam prompts completos por padrão.

## Build e CI

Os adaptadores compartilham `core/build`; dois `clean` paralelos podem apagar classes um do outro. Serializar jobs ou publicar o core versionado em repositório temporário antes dos builds independentes. Fixar JDK/toolchain por alvo e manter caches separados.

## Testes e benchmark

- cenários 1/4/8 NPCs com idle, follow, trabalho e chat;
- 5 minutos de aquecimento + 30 de observação, três repetições;
- startup, reload, dimensão, save e shutdown;
- profiler apenas em desenvolvimento; produção mantém telemetria leve;
- comparar baseline pareado do mesmo pack/save/configuração.

## Critério de aceite

Perfil comum adicional ≤1.000 MB, pico ≤2.000 MB, ausência de crescimento contínuo, delta p95 MSPT dentro da meta aprovada, fila limitada e nenhum worker/fake player órfão após shutdown.

## Dependências

Começa depois que contratos R0/R1/R2 estabilizarem. Otimização medida substitui mudanças especulativas.

## Progresso executado em 19/09/2026

- A coleta de drops do `CompanionServerPlayer` NeoForge foi limitada a quatro avaliações por segundo, removendo uma busca de entidades a cada tick por NPC.
- O supervisor de inferência já usa uma fila global de oito e uma geração ativa por servidor; o encerramento é ligado ao evento de parada.
- Buscas de árvore, minério e fazenda agora têm intervalo mínimo de 20 ticks quando não há alvo; a busca de monstros do modo DEFEND ocorre a cada 10 ticks quando não existe alvo prioritário do dono.
- `CompanionMetrics` registra requisições, sucessos, falhas e rejeições do supervisor sem guardar o conteúdo da conversa.

Ainda pendente: orçamento global de scans/caminhos, decomposição dos controladores, métricas de MSPT/heap e benchmark com quatro/oito NPCs. O cooldown atual é local por companheiro e não substitui o orçamento global.

Em 20/09/2026, a coleta foi fortalecida sem aumentar a frequência global: somente áreas de trabalho recém-quebradas entram numa janela curta de revarredura. Isso resolve drops atrasados/espalhados sem transformar a coleta em busca ampla a cada tick.
