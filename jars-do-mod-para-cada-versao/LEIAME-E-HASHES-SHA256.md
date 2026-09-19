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
| `companions-forge-1.20.1-0.1.0.jar` | Forge 47.4.0+ | 1.20.1 | Padrao / Modpacks Vanilla-like e FTB | 95.811 bytes | `430CF2EA847B81E2A76F7138FCF63440E7ED6B08ADB7B696FBD25B6E6EA38230` |
| `companions-neoforge-1.21.1-0.1.0.jar` | NeoForge 21.1.248+ | 1.21.1 | Padrao / ServerPlayer Fake, LAN, Abas, Skins e IA | 125.756 bytes | `ED5C2D84AAF06982BF819878E8E95DDC4E190E5AFDC8647FC43B7AB291321FD0` |
| `companions-neoforge-1.21.1-tensura-neo-otherworld-0.1.0.jar` | NeoForge 21.1.244+ | 1.21.1 | Adaptada Tensura / ServerPlayer Fake, LAN, Abas, Skins e IA | 125.798 bytes | `C5FA6EFC031D2F4E661C215615E4C263586D30624561AA6D99DA4229539F04D3` |
| `companions-forge-1.12.2-0.1.0.jar` | Forge 14.23.5.2860 | 1.12.2 | Legada / Java 8 Puro | 12.398 bytes | `F8FCA6016E7D8A2E7CBB56997AB7D05EBFF13E15DC04F27AB2F1EC1136473530` |

---

## Estrutura do Diretorio

Para facilitar a localizacao e a integracao com inicializadores e launchers, os arquivos estao disponibilizados tanto na raiz deste diretorio quanto organizados em subpastas por versao:

```text
jars-do-mod-para-cada-versao/
|-- companions-forge-1.20.1-0.1.0.jar
|-- companions-neoforge-1.21.1-0.1.0.jar
|-- companions-neoforge-1.21.1-tensura-neo-otherworld-0.1.0.jar
|-- companions-forge-1.12.2-0.1.0.jar
|-- LEIAME-E-HASHES-SHA256.md
|-- forge-1.20.1/
|   `-- companions-forge-1.20.1-0.1.0.jar
|-- neoforge-1.21.1-padrao/
|   `-- companions-neoforge-1.21.1-0.1.0.jar
|-- neoforge-1.21.1-tensura-neo-otherworld/
|   `-- companions-neoforge-1.21.1-tensura-neo-otherworld-0.1.0.jar
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
- `/companion mode <follow|stay|defend>`: Altera o comportamento (Seguir, Ficar aqui, Defender).
- `/companion action <wood|mine>`: Ordena coleta de madeira ou mineracao de minerios proximos.
- `/companion inventory`: Abre a mochila/inventario de itens do companheiro.
- `/companion deposit`: Despeja itens coletados no bau ou barril mais proximo.
- `/companion recall`: Chama o companheiro para perto com teleporte seguro.
- `/companion view`: Visao remota pelos olhos do companheiro (camera).
- `/companion dismiss`: Dispensa o companheiro do servidor com mensagem de saida.
- `/skin <nome>` ou `/companion skin <nome>`: Altera a skin em tempo real (ex: `Rimuru`, `Goku`, `Luffy`, `Naruto`, `Kirito`).
- `/skin reset`: Restaura para a sua propria skin original.
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

## Como Instalar em Qualquer Modpack ou Launcher

1. Escolha o arquivo `.jar` correspondente a versao do seu Minecraft e mod loader.
2. Copie o arquivo `.jar` para a pasta `mods/` da sua instancia (exemplo: CurseForge, Prism Launcher, Modrinth App ou servidor dedicado).
3. Inicie o jogo normalmente.
