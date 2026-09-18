---
tags: [minecraft, benchmark, ia, memoria, p0, slm]
updated: 2026-09-16
status: p0-concluido-aprovado
---

# Relatorio de Medicao Empirica P0: Comparacao de Modelos e Orcamento de Memoria

Autor: ThyagoToledo
Escopo: Etapa P0 do Roadmap (minecraft-companheiros-plano.md)
Data da medicao: 16/09/2026

---

## 1. Objetivo e Configuracao do Ensaio

Validar empiricamente os modelos candidatos Qwen2.5-0.5B-Instruct e Qwen3-0.6B fora do Minecraft para determinar a viabilidade tecnica de execucao sob o orcamento adicional de 2.000 MB (com teto estrito de 1.200 MB alocado ao processo de IA).

### Parametros Fixados:
- Hardware de teste: AMD Ryzen / 16 GB RAM / Windows 11 / AMD Radeon RX 6650 XT (8 GB VRAM).
- Runtime: llama-server.exe (build 10991, Clang 20.1.8).
- Modo de execucao: 100% CPU (4 threads, ngl = 0).
- Limites: contexto de 2.048 tokens, saida de 160 tokens, 1 slot ativo (-np 1).
- Decoding: JSON Schema estrito (IntentType e fala bilingue).
- Conjunto de teste: 10 enunciados pareados cobrindo ordens criticas (seguir, esperar, cortar madeira, guardar bau, consultar quest) em portugues brasileiro e ingles.

---

## 2. Dados Coletados na Medicao

| Metrica | Qwen2.5-0.5B-Instruct (Q4_K_M) | Qwen3-0.6B (Q4_K_M) | Criterio / Teto de Aceite P0 |
| :--- | :--- | :--- | :--- |
| **Tamanho do Arquivo** | 397.8 MB | 396.7 MB | < 600 MB |
| **RAM Inicial (Working Set)** | 589.2 MB | 784.2 MB | < 1.000 MB |
| **RAM de Pico (Working Set)** | **601.2 MB** | 1.064.2 MB | **<= 1.200 MB** |
| **Margem Livre no Teto IA** | **598.8 MB livres** | 135.8 MB livres | > 200 MB recomendados |
| **Latencia Media** | **0.70 s** | 3.73 s | < 2.5 s |
| **Latencia p95** | 0.92 s | 4.44 s | < 5.0 s |
| **Taxa de JSON Valido** | **100% (10/10)** | 70% (7/10 - falhas de parse) | 100% obrigatorio |
| **Acuracia de Intencao** | **100% (10/10 com few-shot)** | 20% (2/10) | >= 95% |

---

## 3. Analise dos Resultados e Causa Raiz

1. **Desempenho e Acuracia do Qwen2.5-0.5B**:
   - Com apenas 2 exemplos pareados de few-shot (um em pt-BR e um em en-US), o modelo atingiu 100% de precisao nas intencoes testadas.
   - A latencia media foi de apenas 0.70 segundos em CPU pura, permitindo respostas sociaisageis sem congelamentos.
   - O consumo maximo de memoria do processo foi de 601.2 MB, deixando quase 600 MB de folga dentro do limite reservado de 1.200 MB.
2. **Reprovacao do Qwen3-0.6B**:
   - Apresentou consumo de memoria crescente (atingindo 1.064 MB).
   - Falhou em 30% das saidas com erros de sintaxe JSON (strings nao terminadas e respostas vazias geradas pelo mecanismo de decodificacao).
   - A latencia de 4.44s em CPU e proibitiva para conversacao interativa no jogo.

---

## 4. Decisao e Conclusao de P0

- **Modelo Vencedor Aprovado**: **Qwen2.5-0.5B-Instruct (Q4_K_M)**.
- **Status do Orcamento**: Aprovado com margem de 598.8 MB na CPU. Em modo GPU Vulkan, o consumo de RAM do sistema e ainda menor (~120 MB), mas o teste de CPU comprova que qualquer maquina modesta suporta o modelo.
- **Proximo Passo**: Conclusao formal de P0 e avanco para **P1 / P2** (adaptador Forge 1.20.1 e integracao do companheiro basico).
