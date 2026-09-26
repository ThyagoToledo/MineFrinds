# Pacotes de Release do Mod Companions por Versao

Este diretorio centraliza os pacotes binarios compilados (.jar) do mod MineFriends / Companions para todas as plataformas, loaders e modpacks suportados pelo projeto.

---

## Localizacao do Diretorio

```text
C:\Users\thyag\Projects\minecraft-companheiros\jars-do-mod-para-cada-versao\
```

---

## Matriz de Artefatos e Hashes Criptograficos (SHA-256)

| Pacote JAR | Loader | Minecraft | Edicao / Alvo | Tamanho | SHA-256 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `companions-neoforge-1.21.1-0.2.0-alpha.2-1.21.1.jar` | NeoForge 21.1+ (21.1.244+) | 1.21.1 | Padrao / Alpha.2 com suporte universal NeoForge 21.1+, compilado em 2026-09-26 | 308.569 bytes | `4D89261EFCD7837EC0CCE136CB551CE42154AE0CA59F669C9852D2989AEE15F4` |
| `companions-neoforge-1.21.1-tensura-neo-otherworld-0.2.0-alpha.2-1.21.1.jar` | NeoForge 21.1+ (21.1.244+) | 1.21.1 | Tensura Neo Otherworld / Alpha.2 com suporte universal NeoForge 21.1+, compilado em 2026-09-26 | 308.612 bytes | `040BDBFDAF62D3AC287C7C708F076FA65C54C4EE5D9909A9EF58EB13DB1242DF` |
| `companions-forge-1.20.1-0.1.0.jar` | Forge 47.4.0 | 1.20.1 | Padrao / `reobfJar` compilado em 2026-09-21 | 172.929 bytes | `3518CC71DCE01E547663BFBC44CD0FC5249F913EDD16616BDFE005E4244BE5BC` |
| `companions-forge-1.12.2-0.1.0.jar` | Forge 14.23.5.2860 | 1.12.2 | Legada / compilado em 2026-09-21 | 14.235 bytes | `E8FA84D3E86E825EE6D80698AAFE57BE88DF50296A5D05C37BD5347831D74A4B` |

---

## Estrutura do Diretorio

Para facilitar a localizacao e a integracao com inicializadores e launchers, os arquivos estao disponibilizados tanto na raiz deste diretorio quanto organizados em subpastas por versao:

```text
jars-do-mod-para-cada-versao/
|-- companions-neoforge-1.21.1-0.2.0-alpha.2-1.21.1.jar
|-- companions-neoforge-1.21.1-tensura-neo-otherworld-0.2.0-alpha.2-1.21.1.jar
|-- companions-forge-1.20.1-0.1.0.jar
|-- companions-forge-1.12.2-0.1.0.jar
|-- LEIAME-E-HASHES-SHA256.md
|-- neoforge-1.21.1-padrao/
|   `-- companions-neoforge-1.21.1-0.2.0-alpha.2-1.21.1.jar
|-- neoforge-1.21.1-tensura-neo-otherworld/
|   `-- companions-neoforge-1.21.1-tensura-neo-otherworld-0.2.0-alpha.2-1.21.1.jar
|-- release-candidates/
|   `-- 0.2.0-alpha.2/
|       |-- companions-neoforge-1.21.1-0.2.0-alpha.2-1.21.1.jar
|       `-- companions-neoforge-1.21.1-tensura-neo-otherworld-0.2.0-alpha.2-1.21.1.jar
|-- forge-1.20.1/
|   `-- companions-forge-1.20.1-0.1.0.jar
`-- forge-1.12.2/
    `-- companions-forge-1.12.2-0.1.0.jar
```

---

## Caracteristicas da Edicao Tensura Neo Otherworld

O arquivo `companions-neoforge-1.21.1-tensura-neo-otherworld-0.1.0.jar` inclui:
1. **Protecao de Subordinados FTB**: Ativacao automatica da compatibilidade com `tensura_ftb` e `ftb_teams`, assegurando que ataques de area, habilidades Tensura e danos espirituais nao causem dano acidental ao companheiro em claims de equipe.
2. **Escalonamento de EP (Valor de Existencia)**: Acompanha o crescimento de EP, concedendo bonus de vida, ataque, armadura e SHP de acordo com os multiplicadores oficiais do modpack (+1 HP, +0.5 ATK, +0.5 Armadura, +5 SHP por cada 10.000 EP).
3. **Cerimonia de Nomeacao (Name Giving)**: O jogador pode nomear o companheiro via chat ("nomear <nome>" ou "name companion <name>"), gerando salto de EP e evolucao de raca (ex: Goblin para Hobgoblin, Ogro para Kijin).
4. **Imunidade a Veneno de Magiculas**: Protege o companheiro contra efeitos nocivos ao transitar por chunks com alta concentracao de magiculas (como na Floresta de Jura).
5. **Relatorio de Poder e Status Tensura**: Comando de voz/chat "status de magiculas" ou "power report" emitindo diagnostico completo de raca, rank, EP, magiculas e aura.

---

## Interface Grafica com Abas Modulares

A interface do mod (aberta via tecla `C`, icone `[C]` no inventario ou comando `/companion gui`) possui 3 abas especializadas:

1. **Aba 1: Acoes & Ordens (Vanilla Limpo)**:
   - Focada exclusivamente em sobrevivencia tradicional, sem mencao a mods de anime ou fantasia.
   - Painel com vida e modo atual (Seguindo, Ficar, Defender).
   - Acesso a mochila de 27 slots e botao para despejar drops no bau proximo.
   - Botoes de ordens rapidas: Me Seguir, Ficar Aqui, Defender, Puxar para Ca (Recall), Visao Remota e Pega Madeira.
   - Chat integrado para enviar ordens por escrito em linguagem natural.
2. **Aba 2: Aparencia & Skins (Jogador e Personagens de Anime)**:
   - Campo para digitar o nome da skin desejada (ex: `Rimuru`, `Goku`, `Luffy`, `Naruto`, `Kirito`, ou o proprio nickname).
   - Botao **Aplicar** (baixa automaticamente da internet e salva em cache).
   - Botao **Minha Skin** (restaura para a sua aparencia de jogador).
   - Botoes rapidos com presets de personagens populares.
   - Seletor de modelo de bracos: `Classic (4px / Steve)` vs `Slim (3px / Alex)`.
3. **Aba 3: Modpacks & Integracoes (Tensura Neo Otherworld)**:
   - Interruptor `Integracao Tensura: [ ATIVADA / DESATIVADA ]`.
   - Se estiver jogando no modpack Tensura, ativa automaticamente; se estiver jogando no Vanilla, permanece desativada por padrao.
   - Quando ativada, exibe Valor de Existencia (EP), Magiculas, Ranks (F a Catastrophe), Protecao FTB Teams e os botoes de `Status de Magiculas` e `Cerimonia de Nomear`.

---

## Comandos Disponiveis no Jogo

- `/companion spawn [nome]`: Invoca o companheiro no servidor como jogador oficial.
- `/companion lan [nome]`: Abre o mundo para LAN e convoca o companheiro.
- `/companion action <wood|mine|farm>`: Ordena corte de madeira (com felling em cascata), mineracao realista ou agricultura.
- `/companion mine [prioridade]`: Mineracao estilo jogador (escadas 1x2, tuneis retos, exploracao de cavernas e veios de diamante, ferro, carvao, ouro, redstone, lapis, netherite ou todos).
- `/companion farm [padrao|arar]`: Agricultura conservadora (apenas canteiros existentes) ou expansiva (ara terra proxima a agua com enxada e planta).
- `/companion chest` ou `/companion bau`: Registra o bau mais proximo como deposito e estoque principal do companheiro.
- `/companion craft <item> [qtd]`: Fabrica itens autonomamente (busca materiais no bau designado e vai coletar na natureza se faltar madeira/pedra).
- `/companion inventory`: Abre o inventario completo de 54 slots com armaduras (elmo, peitoral, calcas, botas), offhand, mao principal e mochila.
- `/companion deposit`: Despeja itens coletados no bau designado ou no bau mais proximo.
- `/companion view`: Alterna visao remota pelos olhos do companheiro (toggle: execute novamente ou agache para sair).
- `/companion recall`: Chama o companheiro para perto com teleporte seguro.
- `/companion dismiss`: Dispensa o companheiro do servidor com mensagem de saida.
- `/skin <nome>` ou `/companion skin <nome>`: Altera a skin em tempo real com assinaturas criptograficas oficiais (ex: `Rimuru`, `Goku`, `Luffy`, `Naruto`, `Kirito`, `Zoro`, `Gojo`, `Tanjiro`).
- `/skin reset`: Restaura para a sua propria skin original de jogador.
- `/companion help` ou `/help companions`: Envia o guia interativo de ajuda no chat.
- `/companion gui`: Abre a interface grafica com abas (ou tecla `C`).

---

## Formas de Abertura da Interface Grafica (GUI)

O mod disponibiliza 3 formas para abrir o painel de controle do companheiro dentro do jogo:

1. **Tecla de Atalho (Padrao: Tecla C)**:
   - Pressione `C` a qualquer momento durante a partida com o companheiro proximo.
   - O atalho pode ser reconfigurado em: `Opcoes > Controles > Atribuicao de Teclas > Companions > Abrir Painel do Companheiro`.
2. **Botao de Atalho no Inventario (Estilo FTB)**:
   - Ao abrir o inventario (`E`) em modo Sobrevivencia ou o Inventario Criativo, um botao quadrado `[C]` e exibido na lateral do painel.
   - Clicar nesse botao abre imediatamente o painel de controle do companheiro.
3. **Comando no Chat**:
   - Digite qualquer um dos comandos no chat do jogo:
     - `/companion`
     - `/companion gui`
     - `/companions`

---

## Release candidate 0.2.0-alpha.2

Os artefatos foram copiados para as pastas de release candidate correspondentes. O JAR NeoForge padrao foi instalado na instancia de teste abaixo.

### Instancia de teste atualizada

- Instancia: `C:\Users\thyag\curseforge\minecraft\Instances\TesteMineFrinds`
- Loader: NeoForge 21.1.251 / Minecraft 1.21.1
- JAR instalado: `mods/companions-neoforge-1.21.1-0.2.0-alpha.2-1.21.1.jar`
- Backup do JAR anterior: `backups/minefriends-mining-m2-20260922-215620/`
- Hash SHA-256 instalado: `637EB0BADFE043C9DA3EEF8B7DC31CBDE7DE4AAEBCE356C988625A4B619DA34D`
- Hash SHA-256 do complemento Tensura instalado: `2E41090A86FC5428F82C537E45B4C5E32CBBC8904CCF5BC62937AC7A8B1B3575`
- Diagnóstico: `/companion debug mining` exibe alvo, objetivo, scanner incremental, fila de veio e watchdog.

O JAR anterior foi movido para o backup antes da substituicao. Nenhum JAR de outro loader foi colocado nessa instancia.

## Release candidate 0.2.0-alpha.1 (historico)

Os binários atualizados ficam separados dos JARs históricos `0.1.0` em:

```text
jars-do-mod-para-cada-versao/release-candidates/0.2.0-alpha.1/
```

| Artefato | Loader / Minecraft | SHA-256 |
| :--- | :--- | :--- |
| `companions-forge-1.12.2-r5candidate.jar` | Forge 1.12.2 | `C33F317D33206BD8735C46E310B3D98D0143D511E26306EF05B71657F7696C8D` |
| `companions-forge-1.20.1-r5candidate.jar` | Forge 1.20.1 | `C3EE8F8E705CE4C4BD38F14AE1BD27696003AD4B52AEE7F584551F02A412DEE9` |
| `companions-neoforge-1.21.1-r5candidate.jar` | NeoForge 1.21.1 | `6CE2D1F8649E82081B7DF185DD2E402CE692E8FB6E44D1655F7CC971A6CA0744` |
| `companions-neoforge-1.21.1-tensura-neo-otherworld-r5candidate.jar` | NeoForge 1.21.1 / Tensura | `5A6061C5F1597D81C9AA6696972F8D11E7602D0C38C30B2F32BF0AF2D446F96D` |

Os candidatos alpha.1 permanecem preservados para reproducibilidade. A instalacao de teste usa exclusivamente o artefato alpha.2 descrito na secao anterior.

---

## Como Instalar em Qualquer Modpack ou Launcher

1. Escolha o arquivo `.jar` correspondente a versao do seu Minecraft e mod loader.
2. Copie o arquivo `.jar` para a pasta `mods/` da sua instancia (exemplo: CurseForge, Prism Launcher, Modrinth App ou servidor dedicado).
3. Inicie o jogo normalmente.
