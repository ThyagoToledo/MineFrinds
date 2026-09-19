---
tags: [minecraft, qualificacao, forge, 1.20.1, memoria, mspt, p6]
updated: 2026-09-18
status: p6-concluido-aprovado
---

# Relatorio de Qualificacao e Homologacao P6: Versao Forge 1.20.1

Autor: ThyagoToledo
Escopo: Etapa P6 do Roadmap (minecraft-companheiros-plano.md)
Data da medicao: 18/09/2026

---

## 1. Ambientes de Teste e Configuracao de Ensaio

A qualificacao empirica da versao 1.20.1 do mod MineFriends / Companions foi realizada com base no protocolo pareado definido na secao 1.1 da especificacao tecnica (`doc/00_spec/minecraft-companheiros-arquitetura-tecnica.md`), cobrindo os dois ambientes locais reais:

1. **Perfil Cozy Zen (Forge 1.20.1-47.4.10)**:
   - Modpack focado em experiencia rural, decoracao e agricultura.
   - Caracteristica essencial: Ausencia completa de mods da suite FTB (`ftbquests`, `ftbchunks`).
   - Objetivo: Provar operacao autonoma limpa, fallback gracioso em memoria e ausencia de falhas de carregamento de classes (`ClassNotFoundException` / `NoClassDefFoundError`).

2. **Perfil Society Sunlit Valley (Forge 1.20.1-47.4.0)**:
   - Modpack focado em progressao cooperativa e automacao.
   - Caracteristica essencial: Presenca ativa da suite FTB (`ftb-chunks-forge-2001.3.6`, `ftb-quests-forge-2001.4.17` e `ftb-teams-forge-2001.3.2`).
   - Objetivo: Provar respeito rigoroso a claims de terceiros (bloqueio de colheita/mineracao nao autorizada) e compatibilidade com assistencia a missoes.

### Hardware de Ensaio:
- Processador: AMD Ryzen (multi-core)
- Memoria RAM do Sistema: 16 GB DDR4
- Placa Grafica: AMD Radeon RX 6650 XT (8 GB VRAM)
- Sistema Operacional: Windows 11 64-bit
- Java Runtimes: Java 17 (Forge 1.20.1), Java 8 (Core desacoplado) e Java 25 (Ambiente Gradle Wrapper)

---

## 2. Metricas Coletadas e Criterios de Aceite

| Metrica / Ensaio | Meta / Criterio P6 | Medido: Cozy Zen (47.4.10) | Medido: Society (47.4.0) | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Pico Incremental de RAM (4 NPCs + SLM)** | <= 2.000 MB | 846.2 MB (245 MB mod + 601.2 MB SLM) | 858.4 MB (257 MB mod + 601.2 MB SLM) | **Aprovado** (>1.140 MB folga) |
| **Consumo Continuo de RAM** | <= 1.000 MB | 780.0 MB | 795.0 MB | **Aprovado** |
| **Delta MSPT p95 (1 NPC)** | < 1.0 ms | 0.04 ms | 0.04 ms | **Aprovado** |
| **Delta MSPT p95 (4 NPCs ativos)** | <= 5.0 ms | 0.38 ms | 0.42 ms | **Aprovado** |
| **Comportamento sob Estresse (8 NPCs)** | Recusa controlada sob fila >= 8 | Descarte com fallback instantaneo | Descarte com fallback instantaneo | **Aprovado** |
| **Respeito a Claims de Protecao** | Zero acoes negadas executadas | N/A (protecao vanilla respeitada) | Bloqueio 100% via EventBus | **Aprovado** |
| **Integridade de Inventario** | Zero duplicacao ou perda | 100% integro | 100% integro | **Aprovado** |
| **Atomicidade no `/reload`** | Descarte e rebuild sem vazamento | Descarte imediato (0.12 ms) | Descarte imediato (0.15 ms) | **Aprovado** |
| **Degradacao Offline (sem SLM)** | Fallback 0ms para deterministico | 100% funcional | 100% funcional | **Aprovado** |

---

## 3. Analise Detalhada dos Ensaios

### 3.1 Orcamento e Consumo de Memoria
- O mod em si (entidades, renderers, orquestrador hibrido, memoria circular de 6 turnos e catalogo de receitas) consumiu aproximadamente 245 MB a 257 MB de heap incremental sob carga continua com 4 companheiros ativos.
- O runtime de inferencia SLM (`Qwen2.5-0.5B-Instruct` quantizado em Q4_K_M) manteve-se em 601.2 MB de pico no working set de CPU.
- O custo incremental total combinado atingiu ~858 MB, operando confortavelmente abaixo da meta continua de 1.000 MB e com mais de 1.140 MB de margem em relacao ao teto de pico de 2.000 MB.

### 3.2 Impacto no Tick do Servidor (MSPT)
- O orquestrador hibrido despacha intencoes deterministicas (seguir, esperar, defender, inventario, status, recall) em 0 ms na propria thread de tick sem qualquer bloqueio de I/O.
- Requisicoes generativas complexas operam de forma 100% assincrona em threadpool dedicada com fila fixa de 8 posicoes. O tick do servidor nunca espera rede ou inferencia.
- O delta medido de MSPT no percentil 95 com 4 entidades ativas executando calculos de inventario e caminhos foi de 0.42 ms, amplamente inferior ao teto de 5.0 ms.

### 3.3 Estresse, Fila e Protecao contra Vazamentos
- Com 8 entidades simuladas disparando dialogos simultaneos, o cliente de inferencia saturou com seguranca a capacidade maxima da fila (`capacity = 8`).
- As requisicoes excedentes foram imediatamente rejeitadas pela politica `AbortPolicy`, acionando o fallback gracioso com fala deterministica ("Nao entendi muito bem. Pode repetir?").
- A memoria circular de cada entidade manteve estritamente o limite de 6 entradas, descartando os turnos mais antigos sem acumulacao no heap.

### 3.4 Validacao de Claims e Compatibilidade FTB
- No perfil Cozy Zen (sem FTB instalado), `ForgeQuestService` operou em modo standalone em memoria, garantindo que o jogo nao apresente falhas ou incompatibilidades.
- No perfil Society Sunlit Valley (com FTB Chunks e FTB Quests instalados), `ForgePermissionService` interceptou com sucesso tentativas de quebra em blocos pertencentes a claims de terceiros, impedindo a mineracao indevida e mantendo a integridade absoluta dos itens.

---

## 4. Conclusao e Decisao

- **Status da Etapa P6**: **Concluida e Homologada**.
- A plataforma Forge 1.20.1 esta oficialmente qualificada para producao, atendendo a todos os criterios de orcamento de memoria (<= 2.000 MB pico / <= 1.000 MB continuo), desempenho de ticks, estabilidade de recarga e isolamento seguro de permissoes.
- **Proximo Passo**: Avanco para a **Etapa P7 — Ports NeoForge e Forge 1.21.1**.
