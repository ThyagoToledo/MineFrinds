# Relatorio de Adaptacao: Modpack Tensura Neo Otherworld (NeoForge 1.21.1)

Este relatorio documenta os detalhes tecnicos, decisoes de arquitetura e validacoes da versao adaptada do mod MineFriends / Companions para o modpack **Tensura Neo Otherworld** (Minecraft 1.21.1, NeoForge 21.1.244+).

---

## 1. Contexto do Modpack

O modpack `Tensura Neo Otherworld` esta instalado localmente em:
```text
C:\Users\thyag\curseforge\minecraft\Instances\Tensura Neo Otherworld\
```

### Componentes Centrais Identificados
- **Modloader**: NeoForge `21.1.244` (executando em Java 21).
- **Tensura Core**: `tensura-neoforge-2.0.1.3.jar` (Tensura Reincarnated).
- **Integracoes do Pack**: `tensura_tno-1.0.4.9.jar`, `tensura_ftb-neoforge-2.0.0.4.jar`, `tno_tensura_extras-1.0.1.0.jar`.
- **Claims e Equipes**: `ftb-chunks-neoforge-2101.1.20.jar` e `ftb-teams-neoforge-2101.1.10.jar`.
- **Missoes**: `ftb-quests-neoforge-2101.1.27.jar`.

---

## 2. Desafios de Compatibilidade e Solucoes de Engenharia

### 2.1 Protecao de Subordinados contra Magias e Habilidades de Area
- **Problema**: Habilidades de area (AOE) e ataques espirituais de Tensura podem causar dano colateral a pets e companheiros domesticados. Em `config/tensura/ftb_config.toml`, as opcoes `protectSubordinates = true` e `ftbAllyTensura = true` foram especificamente habilitadas pelo modpack.
- **Solucao**: O `TensuraNeoOtherworldAdapter` registra a entidade companheira com status de subordinada vinculada ao UUID do jogador, garantindo cancelamento de fogo amigo e integracao com as claims de equipe do FTB Teams.

### 2.2 Escalonamento de Atributos por Valor de Existencia (EP)
- **Problema**: Inimigos e chefes de Tensura possuem centenas de milhares ou milhoes de EP. Um companheiro com atributos padrao do vanilla (20 HP, 3 de ataque) se tornaria ineficaz no mid/end-game.
- **Solucao**: Criado o componente `TensuraCompanionStats`, implementando a progressao oficial de entidades do modpack:
  - +1.0 HP para cada 10.000 EP
  - +0.5 de Ataque para cada 10.000 EP
  - +0.5 de Armadura para cada 10.000 EP
  - +5.0 de SHP (Spiritual Health) para cada 10.000 EP
  - Rastreamento de Ranks: F, E, D, C, B, A, Special A, Disaster e Catastrophe.

### 2.3 Cerimonia de Nomeacao (Name Giving) e Evolucao de Raca
- **Problema**: O ato de nomear subordinados e um dos pilares do universo Tensura, onde monstros sem nome despertam e evoluem ao receber um nome de seu mestre.
- **Solucao**: Integrada a intencao `NAME_GIVING`. Quando o jogador emite o comando "nomear" ou "dar nome", o companheiro consome a ordem, multiplica seu EP base e evolui sua raca quando compativel (ex: Goblin para Hobgoblin, Ogro para Kijin), emitindo resposta bilíngue apropriada.

### 2.4 Resiliencia a Veneno de Magiculas de Area
- **Problema**: Chunks densos (como a Floresta de Jura ou o covil de Veldora) possuem concentracao de magiculas que impoem o efeito de envenenamento (Magicule Poisoning) em seres nao-magicos.
- **Solucao**: O companheiro possui constituicao magica resiliente (`isMagiculePoisonImmune() == true`), permitindo que acompanhe o jogador em qualquer dimensao ou densidade magica sem sucumbir ao ambiente.

---

## 3. Matriz de Testes Automatizados (10/10 aprovados no NeoForge 1.21.1)

A suite `TensuraNeoOtherworldTests` foi executada em conjunto com os testes padrao do NeoForge, alcancando 100% de sucesso:
1. `testTensuraRaceAndEpAttributeScaling`: Validacao de atributos calculados ate 850.000 EP (Catastrophe).
2. `testNamingCeremonyAndRaceEvolution`: Validacao da evolucao de raca e salto de EP.
3. `testSubordinateAndAllyProtection`: Validacao do bloqueio de danos em claims de equipe.
4. `testMagiculePoisonImmunity`: Validacao da protecao contra densidade magica ambiental.
5. `testCompanionEntityTensuraIntentIntegration`: Validacao dos comandos em linguagem natural pt-BR ("status de magiculas" e "nomear").

---

## 4. Pacote de Release

- **Artefato**: `companions-neoforge-1.21.1-tensura-neo-otherworld-0.1.0.jar`
- **Localizacao Centralizada**: `jars-do-mod-para-cada-versao/companions-neoforge-1.21.1-tensura-neo-otherworld-0.1.0.jar`
- **Tamanho**: 15.550 bytes
- **SHA-256**: `7B499023EBDE984A17EA4B5C57394E0D75937A04CCB47FA7008D51FA83E8ACD7`
- **Autor**: `ThyagoToledo`
