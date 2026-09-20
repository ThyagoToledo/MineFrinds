---
tags: [minecraft, minefriends, r2, ia, qwen, inferencia, bilingue]
updated: 2026-09-19
status: parcialmente-implementado
---

# R2 — IA assíncrona real e bilíngue

## Resultado esperado

Conversa pt-BR/en-US e interpretação ambígua usam um modelo pequeno sem bloquear ticks. O sistema funciona integralmente em modo determinístico quando o runtime está ausente.

## Arquitetura

- Um `InferenceSupervisor` por servidor, criado no startup e encerrado no server stopping.
- `InferenceTransport` executa uma requisição HTTP; não possui segunda fila própria.
- Supervisor mantém uma geração ativa, fila máxima 8, timeout, cancelamento lógico, métricas e circuit breaker fechado/aberto/meio-aberto.
- Configuração padrão `enabled=false`, endpoint loopback, contexto 2048 e saída 160.
- `OpenAiResponseParser` sem dependência externa aceita envelope OpenAI-compatible ou JSON direto, limita corpo/fala e rejeita intenção desconhecida.
- Unicode pt-BR é preservado; emoji é preferência visual configurável.

## Fluxo

1. Parser determinístico tenta resolver o pedido.
2. Pedido claro executa pelo protocolo R1, sem inferência.
3. Conversa/ambiguidade gera contexto mínimo e `InferenceRequest` com IDs, revisão, idioma e deadline.
4. Worker chama o runtime.
5. Resultado é desserializado e validado fora da thread do jogo.
6. Adaptador agenda conclusão no servidor, revalida sessão/revisão e descarta resposta obsoleta.
7. Intenção proposta passa pelo mesmo validador R1 e executor R0.
8. Fala de sucesso é produzida apenas a partir de `ActionResult` confirmado.

## Alterações por módulo

- Core: `InferenceSupervisor`, `InferenceTransport`, transporte HTTP, parser e testes de contrato foram adicionados. `InferenceRequest`/`InferenceResult` e clock injetável continuam pendentes.
- Forge 1.20.1: `CompanionEntity` usa `processAsync` e aplica efeitos no executor do servidor.
- NeoForge 1.21.1: provider compartilhado e `handleCommandAsync` foram integrados; respostas de chat retornam ao executor do servidor.
- Forge 1.12.2: generativo permanece desligado até dispatcher seguro; determinístico continua.
- Config: endpoint, enable, timeout, fila, idioma, limites e política remota.

## Segurança e privacidade

- Endpoint remoto exige opt-in; tokens ficam em configuração local fora de saves/pacotes.
- Não enviar inventário completo, coordenadas exatas ou quests ocultas sem necessidade.
- Limitar corpo HTTP e logar métricas/IDs, não conversa completa por padrão.
- Modelo nunca recebe objeto mutável do mundo.

## Progresso executado em 19/09/2026

- Supervisor compartilhado por servidor, fila máxima oito, uma geração ativa, timeout, circuit breaker e shutdown em `ServerStoppingEvent`.
- IA desligada por padrão (`companions.ai.enabled=false`) para respeitar o orçamento de memória; endpoint loopback é opt-in.
- Parser sem Gson mantém compatibilidade com Forge 1.12.2, que não garante Gson disponível no runtime.
- Suítes limpas aprovadas sequencialmente: Forge 1.12.2, Forge 1.20.1 e NeoForge 1.21.1.

R2 permanece **parcialmente concluída** até haver avaliação opt-in com o modelo local e intents de ação enviadas sem o dispatcher textual.

O payload NeoForge já encaminha conversa livre validada diretamente ao provider; ao concluir, a resposta só é reaplicada se a revisão do snapshot ainda for a mesma. O identificador de requisição continua protegido pela janela anti-replay por dono. Modos e ações ainda usam o dispatcher vanilla durante a migração, mas passam pela enumeração validada antes de chegar a ele.

## Testes

- transporte fake: sucesso, timeout, fila cheia, shutdown e cancelamento;
- circuit breaker com relógio injetável;
- HTTP não-200, corpo excessivo, JSON truncado, markdown, fala vazia e enum inválido;
- resposta após morte/dismiss/dimensão/revisão nova;
- 50 frases reservadas por idioma, separadas do conjunto de desenvolvimento;
- avaliação Qwen opt-in com GGUF/hash, prompt, saídas, latência e memória.

## Critério de aceite

Nenhuma thread de tick aguarda I/O; ≥95% de intenção em cada idioma; modo offline mantém ações; resposta velha não altera estado; nenhuma fala inventa conclusão; perfil integrado respeita 1 GB comum/2 GB pico.

## Dependências

O núcleo pode ser implementado enquanto R0.1/R1.1 avançam. Ativar intenções capazes de agir exige ambos concluídos.
