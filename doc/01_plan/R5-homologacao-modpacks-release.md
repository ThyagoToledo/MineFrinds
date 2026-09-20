---
tags: [minecraft, minefriends, r5, homologacao, modpacks, release]
updated: 2026-09-19
status: planejado-evidencia-pendente
---

# R5 — Homologação, modpacks e release

## Resultado esperado

Cada JAR publicado possui matriz de suporte, hash e evidência reproduzível no loader, versão, hardware e modpacks declarados.

## Ambientes

| Plataforma | Ambientes mínimos |
| --- | --- |
| NeoForge 1.21.1 | TesteMineFrinds, Tempest, Tensura All The Slime, Tensura Neo Otherworld |
| Forge 1.20.1 | instância limpa, Cozy Zen, Society Sunlit Valley |
| Forge 1.12.2 | instância limpa e pack legado quando identificado |

Modernos exigem cliente integrado e servidor dedicado. Usar cópias de teste e saves descartáveis.

## Roteiro funcional

1. instalar e validar dependências/loader;
2. spawn, vínculo, comandos, GUI e idiomas;
3. follow/stay/defend e combate;
4. trabalho e claims permitido/negado;
5. inventário cheio, baú, crafting e conservação;
6. recall e câmera;
7. skin, logout/relogin, morte, dimensão e stop/start;
8. IA desligada, ativa, timeout, fila cheia e runtime encerrado;
9. dois jogadores e isolamento de dono;
10. reload de receita e quest real suportada.

## Qualidade e desempenho

- Baseline A sem mod e B/C com mod/runtime no mesmo cenário.
- Perfis 1/4/8 NPCs; três repetições após aquecimento.
- Medir RAM incremental, pico, RSS do runtime, heap/GC, VRAM separada e MSPT.
- Meta: ≤1.000 MB comum, ≤2.000 MB pico e sem crescimento contínuo.
- Falha do pack baseline é registrada separadamente.

## Evidência por execução

- loader/Minecraft/Java/hardware;
- lista e versões de mods relevantes;
- hash do JAR, modelo e runtime;
- configuração e passos;
- logs, métricas e resultado por caso;
- limitações e regressões conhecidas.

## Versionamento e empacotamento

- Próximo candidato: `0.2.0-alpha.1` ou superior.
- Nunca substituir um JAR `0.1.0` mantendo nome com hash diferente.
- Um JAR por loader/versão; edição Tensura separada apenas se o conteúdo binário realmente diferir.
- Gerar checksums depois do teste final; o JAR testado é o JAR distribuído.
- Runtime/modelo têm instalação explícita e licença própria; não são embutidos silenciosamente.

## Critério de aceite

Zero duplicação/perda, nenhuma ação em claim negado, ciclos de save aprovados, recursos declarados funcionam no alvo, orçamento atendido e documentação corresponde ao binário. Relatórios anteriores P6–P9 permanecem históricos preliminares até esta matriz ser executada.

## Saída

Release candidate, matriz pública de capacidades, hashes, instalação, configuração da IA, backup/recuperação e problemas conhecidos. Publicação externa ocorre apenas quando solicitada.

## Progresso executado em 19/09/2026

- `tools/collect-r5-instance-inventory.ps1` gera inventário somente leitura do diretório CurseForge e registrou seis instâncias disponíveis, com versão do Minecraft, loader declarado no `minecraftinstance.json`, quantidade de mods, configuração, saves e metadata.
- Os testes JVM passaram sequencialmente em core (29 casos), Forge 1.20.1 (15), NeoForge 1.21.1 (25) e Forge 1.12.2 (5), totalizando 74 casos sem falhas no último ciclo.
- A presença das instâncias e a compilação não equivalem à homologação: ainda falta iniciar cópias descartáveis de cliente/servidor, medir RAM/MSPT e executar o roteiro funcional em cada matriz.
