---
tags: [minecraft, distribuicao, release, hashes, sha256, p9]
updated: 2026-09-19
status: p9-concluido-aprovado
---

# Relatorio de Distribuicao e Release P9: MineFriends v0.1.0

Autor: ThyagoToledo
Escopo: Etapa P9 do Roadmap (minecraft-companheiros-plano.md)
Data da medicao: 19/09/2026

---

## 1. Identificacao e Resumo da Release

A Etapa P9 conclui o ciclo inicial de engenharia do mod **MineFriends** (v0.1.0), consolidando o empacotamento oficial e independente para os tres alvos de Minecraft Java definidos na arquitetura:
1. **Forge 1.20.1** (alvo intermediario de modpacks populares como Cozy Zen e Society)
2. **NeoForge 1.21.1** (alvo moderno para Tempest Protocol e Tensura All The Slime 1)
3. **Forge 1.12.2** (alvo legado em Java 8 puro)

Cada versao possui seu proprio arquivo binario JAR, isolando especificidades de rede, sistemas de componentes e carregadores de classes, enquanto compartilham 100% dos contratos e logica cognitiva do modulo universal `:core`.

---

## 2. Matriz de Artefatos e Hashes SHA-256

| Alvo | Arquivo JAR | Bytecode | Tamanho | Hash SHA-256 |
| :--- | :--- | :--- | :--- | :--- |
| **Forge 1.20.1** | `companions-0.1.0.jar` | Java 17 | 95.197 B | `59C7350B7B668F91F0F76560EBE01D3B6A318E746FFC62C54B518353FA5AC5B9` |
| **NeoForge 1.21.1** | `companions-neoforge-1.21.1-0.1.0-1.21.1.jar` | Java 21 | 8.697 B | `272D839A634D54BC512DE21636D704011DEAC97CE29CA3512B11772EF0FE67F0` |
| **Forge 1.12.2** | `companions-forge-1.12.2-0.1.0-1.12.2.jar` | Java 8 | 12.100 B | `A78FF301F4273B9B3FB28FDBE6196EBCA1956B8AE56D6E0DB477DA867A2B4E38` |

---

## 3. Instrucoes para Instalacao do Modelo de IA / Runtime

O mod foi projetado com resiliencia total:
- **Modo Standalone (Zero Setup)**: O mod opera de forma 100% autonoma e deterministica sem necessidade de instalar nenhum executavel adicional. Todas as acoes (seguir, esperar, defender, recall, visao remota, mochila, status, tarefas de madeira e deposito) funcionam com 0ms de latencia.
- **Modo Conversa Livre (Opcional)**: Para habilitar o dialogo aberto com personalidade e intencoes contextuais, configure um runtime local compatível com API OpenAI:

### Passo a Passo para Inferencia Local:
1. **Download do Modelo Homologado**:
   - Modelo: `Qwen2.5-0.5B-Instruct-Q4_K_M.gguf` (tamanho: ~398 MB).
2. **Iniciar o Runtime (`llama-server.exe` ou `Ollama`)**:
   ```bash
   llama-server.exe -m qwen2.5-0.5b-instruct-q4_k_m.gguf --port 8080 -c 2048 -np 1 --threads 4
   ```
3. **Parametros Recomendados de Seguranca**:
   - Porta: `8080` (endpoint `/v1/chat/completions`).
   - Contexto maximo: `2.048 tokens`.
   - Saida maxima: `160 tokens`.
   - Fila maxima aceita pelo mod: `8 requisicoes` (com descarte automatico caso sature).
   - Timeout de rede: `3.000 ms` (caindo para fala deterministica caso o processo demore).

---

## 4. Status Consolidado das Suites de Teste

| Modulo / Plataforma | Testes Executados | Status |
| :--- | :--- | :--- |
| `:core` (Java 8) | 19 testes unitarios e benchmarks | 100% Aprovado |
| `platforms/forge-1.20.1` | 15 testes de integracao e sidedness | 100% Aprovado |
| `platforms/neoforge-1.21.1` | 5 testes de payloads e claims Tensura | 100% Aprovado |
| `platforms/forge-1.12.2` | 5 testes de rede IMessage e formato .lang | 100% Aprovado |
| **Total do Projeto** | **44 testes automatizados** | **100% Aprovados** |

---

## 5. Conclusao e Encerramento do Roadmap P0–P9

Todas as etapas do roadmap mestre (`minecraft-companheiros-plano.md`) foram integralmente concluidas, testadas e documentadas com evidencias empiricas verificadas:
- **P0**: Benchmark SLM concluido (`Qwen2.5-0.5B-Instruct` selecionado).
- **P1**: Scaffold Forge 1.20.1 + Java 8 core funcional.
- **P2**: Ordens bilingues, recall seguro e scouting camera homologados.
- **P3**: WorkArea, limites de permissao (claims FTB/OPAC) e crafting planner.
- **P4**: Adaptacao a modpacks e assistencia a quests (FTB Quests / dependencias).
- **P5**: Inferencia assincrona segura, orquestrador hibrido e memoria curta.
- **P6**: Qualificacao nos modpacks Cozy Zen (47.4.10) e Society Sunlit Valley (47.4.0).
- **P7**: Ports NeoForge 21.1.248 (Tempest e Tensura) e Forge 1.21.1.
- **P8**: Port Forge 1.12.2 em Java 8 puro (rede IMessage e dicionarios .lang).
- **P9**: Distribuicao multiversao, hashes SHA-256 e documentacao de release.
