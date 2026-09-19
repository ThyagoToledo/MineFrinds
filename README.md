<p align="center">
  <img src="docs/assets/banner.png" alt="MineFriends Banner" width="850px" style="border-radius: 12px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);" />
</p>

# MineFriends

<p align="center">
  <a href="https://github.com/ThyagoToledo/MineFrinds"><img src="https://img.shields.io/badge/MineFriends-v0.1.0-00b4d8?style=for-the-badge" alt="MineFriends Version" /></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-21%20%2F%2017%20%2F%208-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" /></a>
  <a href="https://files.minecraftforge.net/"><img src="https://img.shields.io/badge/Forge-1.20.1%20%7C%201.12.2-DFAD32?style=for-the-badge&logo=curseforge&logoColor=black" alt="Minecraft Forge" /></a>
  <a href="https://neoforged.net/"><img src="https://img.shields.io/badge/NeoForge-1.21.1-EA6C24?style=for-the-badge&logo=neoforge&logoColor=white" alt="NeoForge" /></a>
  <a href="https://gradle.org/"><img src="https://img.shields.io/badge/Gradle-8.8-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle" /></a>
  <a href="https://junit.org/"><img src="https://img.shields.io/badge/Tests-44%2F44%20Passing-2ea44f?style=for-the-badge&logo=junit5&logoColor=white" alt="Tests" /></a>
</p>

---

## Visao Geral

O **MineFriends** e um mod para Minecraft Java focado em adicionar companheiros artificiais verdadeiramente uteis para a sua jornada. Desenvolvido com engenharia limpa e foco em estabilidade, o projeto preenche a lacuna de solidao no modo um jogador e adiciona aliados eficientes para servidores cooperativos e modpacks pesados.

Ao contrario de bots convencionais que sobrecarregam o servidor ou quebram a imersao com acoes caoticas, o MineFriends adota uma arquitetura em camadas: as tomadas de decisao essenciais, o combate e o inventario sao processados de forma nativa e estavel na engine do jogo, enquanto a cognicao dialogica e adaptada de forma hibrida e leve para modelos pequenos (SLMs locais), garantindo respostas com 100% de precisao sem travar os 20 TPS do mundo.

---

## Principais Recursos

- **Companheirismo e Auxilio Real**: Aliados capazes de seguir, aguardar em locais marcados, defender o jogador de monstros hostis e carregar suprimentos.
- **Comunicacao Bilingue Nativa**: Interacao completa tanto em Portugues do Brasil (pt-BR) quanto em Ingles (en-US). Comandos por chat ou interface recebem respostas contextuais imediatas.
- **Chamada Segura (Recall)**: Mecanica para solicitar que o companheiro se aproxime ou se teleporte com seguranca ate voce. O sistema realiza checagem de colisao, busca piso solido, respeita cooldowns e impede teleportes durante combates ativos.
- **Visao Remota (Scouting Camera)**: Permite observar o mundo diretamente atraves dos olhos do seu companheiro enquanto o seu personagem permanece seguro. O modo conta com seguranca reativa, cancelando a visualizacao caso voce sofra qualquer tipo de dano.
- **Inventario Persistente e Seguro**: Compartimento de 27 slots sincronizado via NBT oficial, eliminando qualquer risco de duplicacao ou perda de itens ao descarregar chunks ou fechar o jogo.
- **Eficiencia de Memoria Rigorosa**: O mod opera com teto estrito de consumo: cerca de 1 GB em operacao continua e ate 2 GB em picos transientes, mantendo compatibilidade mesmo em computadores convencionais e placas graficas modernas com aceleracao Vulkan.
- **Suporte Multiversao Oficial**:
  - **Forge 1.20.1** (qualificado nos modpacks Cozy Zen 47.4.10 e Society Sunlit Valley 47.4.0)
  - **NeoForge 1.21.1** (compativel com Tempest Protocol e Tensura All The Slime 1)
  - **Forge 1.12.2** (runtime legado Java 8 com rede IMessage e dicionarios .lang)

---

<details>
<summary><b>Estrutura de Diretorios do Projeto</b></summary>

```text
MineFrinds/
|-- core/                               # Nucleo conceitual desacoplado (Java 8 universal)
|   |-- src/main/java/com/thyagotoledo/companions/core/
|   |   |-- ai/                         # Clientes de inferencia assincrona e memoria circular
|   |   |-- dialogue/                   # Orquestrador hibrido e provedor deterministico
|   |   |-- locale/                     # Servico de internacionalizacao pt-BR e en-US
|   |   |-- model/                      # DTOs, perfis, inventario e modos de postura
|   |   |-- permissions/                # Contratos agnosticos de verificacao de claims
|   |   |-- planner/                    # Catalogo de receitas e arvore de crafting
|   |   `-- quest/                      # Modelagem e planejador de missoes FTB Quests
|   `-- src/test/java/                  # 19 testes unitarios e QualificationHarness
|-- platforms/
|   |-- forge-1.20.1/                   # Plataforma Forge 1.20.1 (Java 17)
|   |-- neoforge-1.21.1/                # Plataforma NeoForge 1.21.1 (Java 21 / CustomPacketPayload)
|   `-- forge-1.12.2/                   # Plataforma Forge 1.12.2 legada (Java 8 / IMessage)
|-- doc/
|   |-- 00_spec/                        # Especificacao tecnica de arquitetura e contratos
|   |-- 01_plan/                        # Roadmap executavel de etapas (P0 a P9 concluidos)
|   |-- 02_design/                      # Design visual de skins 64x64, interface e assets
|   `-- 03_context/                     # Relatorios tecnicos oficiais (P0, P6, P7, P8 e P9)
|-- docs/
|   `-- assets/                         # Banners, diagramas e recursos visuais do repositorio
`-- README.md
```

</details>

---

<details>
<summary><b>Como Compilar e Executar Localmente</b></summary>

### Pre-requisitos
- JDK 17 / JDK 21 instalado e configurado no PATH.
- Git.

### 1. Clonar o Repositorio
```bash
git clone https://github.com/ThyagoToledo/MineFrinds.git
cd MineFrinds
```

### 2. Executar Todas as Suites de Teste
```bash
# Testes do modulo universal core (19 testes)
cd platforms/forge-1.20.1
./gradlew :minecraft-companheiros:core:test

# Testes da plataforma Forge 1.20.1 (15 testes)
./gradlew test

# Testes da plataforma NeoForge 1.21.1 (5 testes)
cd ../neoforge-1.21.1
./gradlew test

# Testes da plataforma Forge 1.12.2 (5 testes)
cd ../forge-1.12.2
./gradlew test
```

### 3. Gerar os Pacotes JAR de Release
```bash
# Forge 1.20.1
cd platforms/forge-1.20.1 && ./gradlew jar
# Saida: platforms/forge-1.20.1/build/libs/companions-0.1.0.jar

# NeoForge 1.21.1
cd platforms/neoforge-1.21.1 && ./gradlew jar
# Saida: platforms/neoforge-1.21.1/build/libs/companions-neoforge-1.21.1-0.1.0-1.21.1.jar

# Forge 1.12.2
cd platforms/forge-1.12.2 && ./gradlew jar
# Saida: platforms/forge-1.12.2/build/libs/companions-forge-1.12.2-0.1.0-1.12.2.jar
```

</details>

---

<details>
<summary><b>Comandos, Intencoes e Comunicacao</b></summary>

O companheiro compreende comandos falados diretamente no chat ou acionados via interface:

| Intencao | Comando em Portugues | Comando em Ingles | Acao Executada |
|---|---|---|---|
| **Seguir** | `@Nome me segue` / `vem comigo` | `@Name follow me` / `walk with me` | O companheiro passa a seguir o dono mantendo distancia de conforto. |
| **Aguardar** | `@Nome espera` / `fica aqui` / `para` | `@Name stay here` / `wait` / `sit` | O companheiro senta e permanece fixo no local. |
| **Defender** | `@Nome defenda` / `fique alerta` | `@Name defend` / `protect` / `guard` | Postura de guarda, protegendo ativamente o dono contra ameacas hostis. |
| **Recall** | `@Nome vem ca` / `chamar` / `puxar` | `@Name come here` / `recall` | Verifica o terreno ao redor e se aproxima com seguranca do jogador. |
| **Visao Remota** | `@Nome visao` / `olhar` | `@Name remote view` / `scout` | Alterna a camera para os olhos do companheiro; aperte ESC para sair. |
| **Status** | `@Nome status` / `relatorio` | `@Name status` / `report` | Informa os pontos de vida atuais, vida maxima e modo ativo. |
| **Mochila** | `@Nome abrir mochila` / `inventario` | `@Name open backpack` / `inventory` | Disponibiliza o inventario compartilhado de 27 slots do companheiro. |

Tambem e possivel alternar rapidamente entre os modos de postura clicando com o botao direito enquanto agachado (Shift + Clique Direito).

</details>

---

<details>
<summary><b>Hub de Documentacao e Engenharia</b></summary>

Toda a especificacao tecnica, os estudos de hardware, relatorios de qualificacao e os planos estao estruturados na pasta [`doc/`](doc/):

- **[Plano Mestre Executavel (P0 a P9)](doc/01_plan/minecraft-companheiros-plano.md)**: Roadmap do projeto detalhando marcos P0 a P9 com todos os criterios de aceite homologados.
- **[Especificacao Tecnica de Arquitetura](doc/00_spec/minecraft-companheiros-arquitetura-tecnica.md)**: Contratos Java, orcamento de RAM e isolamento de sidedness.
- **[Design Visual, Interface e Assets](doc/02_design/minecraft-companheiros-interface-assets.md)**: Padroes de skins 64x64, paletas de cores, layout da GUI e camera de vigia.
- **[Relatorio de Benchmark P0](doc/03_context/minecraft-companheiros-p0-relatorio-benchmark.md)**: Medicoes empiricas de RAM/VRAM e aprovacao do Qwen2.5-0.5B.
- **[Pesquisa de IA e Runtimes](doc/03_context/minecraft-companheiros-pesquisa-ia-e-runtimes.md)**: Analise de runtimes locais e integracao Vulkan.
- **[Relatorio de Qualificacao P6 (Forge 1.20.1)](doc/03_context/minecraft-companheiros-p6-relatorio-qualificacao.md)**: Homologacao nos modpacks Cozy Zen e Society Sunlit Valley.
- **[Relatorio de Port P7 (NeoForge 1.21.1)](doc/03_context/minecraft-companheiros-p7-relatorio-port-1211.md)**: Homologacao em Tempest Protocol e Tensura All The Slime 1.
- **[Relatorio de Port P8 (Forge 1.12.2)](doc/03_context/minecraft-companheiros-p8-relatorio-port-1122.md)**: Homologacao do runtime legado em Java 8 puro.
- **[Relatorio de Distribuicao e Hashes P9](doc/03_context/minecraft-companheiros-p9-relatorio-distribuicao.md)**: Hashes SHA-256 e instrucoes para inferencia local.

</details>

---

## Autor

<p align="center">
  <a href="https://github.com/ThyagoToledo">
    <img src="https://github.com/ThyagoToledo.png" width="110px" alt="ThyagoToledo" style="border: 2px solid #00f0ff; border-radius: 8px; box-shadow: 0 0 15px rgba(0, 240, 255, 0.4);" />
    <br />
    <sub><b>ThyagoToledo</b></sub>
  </a>
</p>

<p align="center">
  Desenvolvido com dedicacao por <b>ThyagoToledo</b>.
</p>

---

## Licenca

Este projeto e disponibilizado sob os termos da licenca MIT. Consulte o arquivo [LICENSE.txt](platforms/forge-1.20.1/LICENSE.txt) para mais informacoes.
