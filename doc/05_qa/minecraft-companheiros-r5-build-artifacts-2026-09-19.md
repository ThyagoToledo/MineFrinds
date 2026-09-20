---
tags: [minecraft, minefriends, r5, release, hashes, qa]
updated: 2026-09-19
status: candidato-build-validado-jvm
---

# R5 — artefatos candidatos e hashes

Os quatro arquivos abaixo foram gerados depois da regressão JVM das três plataformas e organizados em `jars-do-mod-para-cada-versao/release-candidates/0.2.0-alpha.1/`. O hash identifica exatamente o binário candidato; ele ainda não é homologação funcional em modpack.

| Artefato | Loader/Minecraft | Tamanho | SHA-256 |
|---|---|---:|---|
| `companions-forge-1.12.2-r5candidate.jar` | Forge 14.23.5.2860 / 1.12.2 | 12.399 bytes | `C33F317D33206BD8735C46E310B3D98D0143D511E26306EF05B71657F7696C8D` |
| `companions-forge-1.20.1-r5candidate.jar` | Forge 47.x / 1.20.1 | 131.531 bytes | `C3EE8F8E705CE4C4BD38F14AE1BD27696003AD4B52AEE7F584551F02A412DEE9` |
| `companions-neoforge-1.21.1-r5candidate.jar` | NeoForge 21.1.251 / 1.21.1 | 197.881 bytes | `6CE2D1F8649E82081B7DF185DD2E402CE692E8FB6E44D1655F7CC971A6CA0744` |
| `companions-neoforge-1.21.1-tensura-neo-otherworld-r5candidate.jar` | NeoForge 21.1.251 / 1.21.1 | 197.923 bytes | `5A6061C5F1597D81C9AA6696972F8D11E7602D0C38C30B2F32BF0AF2D446F96D` |

## Evidência de build

- `platforms/forge-1.20.1`: `build` aprovado; artefato reobfuscado veio de `build/reobfJar/output.jar`.
- `platforms/forge-1.12.2`: `build` aprovado.
- `platforms/neoforge-1.21.1`: `build` e `tensuraJar` aprovados.
- Testes JVM no ciclo: 74 aprovados, 0 falhas.

## Limite da evidência

O candidato deve ser instalado somente em cópias descartáveis das instâncias. Ainda faltam cliente/servidor dedicado, GameTests de mundo, claims reais, ciclo stop/start e medição de RAM/MSPT para fechar R5.
