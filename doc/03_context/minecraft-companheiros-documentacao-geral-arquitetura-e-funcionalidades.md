# Documentacao Geral de Arquitetura, Implementacao e Tecnologias: MineFriends (Companions)

Este documento consolidado descreve em detalhes tecnicos tudo o que foi implementado no projeto MineFriends, como cada subsistema foi desenvolvido e a stack completa de tecnologias, dependencias, frameworks e padroes utilizados.

---

## 1. Visao Geral do Projeto

O **MineFriends** e um mod para Minecraft Java Edition focado em introduzir companheiros controlados por computador que se comportam e interagem no servidor como verdadeiros jogadores. Diferente de entidades passivas ou bots externos baseados em socket (como Node.js/Mineflayer), o MineFriends roda de forma nativa e integrada na thread do servidor Minecraft (20 TPS), eliminando atrasos de rede, dessincronias e dependencias de processos externos.

O autor e desenvolvedor exclusivo do projeto e **ThyagoToledo**.

---

## 2. O Que Ja Fizemos (Funcionalidades Concluidas)

### 2.1. Invocacao Oficial como Jogador (Fake Player Carpet-Style)
- O companheiro e instanciado como uma extensao oficial de `ServerPlayer`.
- Ao entrar, emite a mensagem oficial `<Nome> entrou no jogo` para todos os clientes.
- Aparece na lista de jogadores do **Tab** com icone de cabeca e skin renderizada.
- Possui modelo humanoide 3D completo no mundo com suporte a capas, modelos Steve (Classic 4px) e Alex (Slim 3px).
- Suporte a abertura do mundo para LAN (`/companion lan [nome]`) para cooperativo local.

### 2.2. Inteligencia Motora e Modos de Comportamento
O companheiro conta com modos de operacao selecionaveis via comandos, GUI ou ordens de voz/chat:
1. **`FOLLOW` (Seguir)**:
   - Acompanha o jogador mantendo distancia de conforto.
   - Se afastar mais de 36 blocos ou mudar de dimensao, realiza teleporte seguro para perto do dono em bloco solido.
2. **`STAY` (Ficar Aqui)**:
   - Para no local e observa o jogador ou arredores com movimentacao desacelerada.
3. **`DEFEND` (Postura Defensiva)**:
   - Monitora agressoes sofridas pelo dono ou ameacas hostis em raio de 12 blocos.
   - Ataca monstros com salto, mira e temporizador de forca de ataque oficial (`resetAttackStrengthTicker`).
4. **`WOOD` (Coleta de Madeira com Felling em Cascata)**:
   - Escaneia troncos em ate 12 blocos priorizando blocos com menor coordenada Y (corta a arvore pela base).
   - Ao quebrar o tronco base, ativa quebra em cascata vertical (ate 16 blocos acima), derrubando a arvore inteira de uma so vez.
   - Corrige definitivamente o problema do bot travar olhando para cima.
5. **`MINE` (Mineracao de Minerios e Pedras)**:
   - Localiza minerios expostos (carvao, ferro, ouro, diamante, redstone, cobre) e pedras.
   - Caminha ate eles, extrai os blocos e armazena os recursos na bolsa.
6. **`FARM` (Agricultura e Plantio)**:
   - Escaneia terras aradas em raio de 10 blocos.
   - Se houver plantacao madura (`CropBlock.isMaxAge(state)`: trigo, cenoura, batata, beterraba), colhe os alimentos.
   - Se houver terra arada vazia (`Blocks.FARMLAND` com ar acima) e o bot tiver sementes, replanta imediatamente consumindo a semente e reproduzindo o som de plantio.
7. **Coleta Automatica de Chao**:
   - Caixa de colisao inflada em 3 blocos aspira automaticamente itens jogados no chao diretamente para o inventario.

### 2.3. Inventario Completo com Armaduras, Offhand e Mao Principal
- **Container de 54 Slots (`ChestMenu.sixRows`)**:
  - **Slots 0 a 3**: Slots dedicados de armadura (Cabeca/Elmo, Peito/Peitoral, Pernas/Calcas, Pes/Botas).
  - **Slot 4**: Mao Secundaria / Offhand (para escudos, tochas, totens da imortalidade).
  - **Slot 5**: Mao Principal (para armas e ferramentas selecionadas).
  - **Slots 9 a 44 (36 slots)**: Todo o armazenamento do companheiro (hotbar de 9 slots + 27 slots de mochila).
  - **Slots 45 a 53**: Slots complementares de acoes.
- **Sincronizacao 3D Imediata**: Qualquer alteracao nos slots 0 a 5 invoca `setItemSlot`, disparando o pacote `ClientboundSetEquipmentPacket` que atualiza instantaneamente a armadura e o item empunhado no modelo 3D do companheiro.

### 2.4. Inteligencia de Auto-Tool, Auto-Armor e Auto-Eat
- **Auto-Tool**: Varre o inventario e equipa na mao principal a melhor ferramenta correspondente ao modo ativo:
  - No modo `WOOD`: equipa o melhor machado (Netherite > Diamante > Ferro > Pedra > Ouro > Madeira).
  - No modo `MINE`: equipa a melhor picareta.
  - No modo `DEFEND`: equipa a melhor espada.
  - No modo `FARM`: equipa a melhor enxada.
- **Auto-Armor**: Avalia periodicamente as pecas de armadura na bolsa por valor de defesa e tenacidade; se os slots de armadura estiverem vazios ou possuirem itens inferiores, equipa automaticamente os melhores.
- **Auto-Eat**: Se a vida estiver abaixo de 18 coracoes (90%), varre a bolsa por itens com componente de comida (`DataComponents.FOOD`), consome 1 unidade, recupera saude e reproduz o som de mastigacao do jogador.

### 2.5. Vulnerabilidade e Dano Real em Modo Sobrevivencia
- Entidade configurada oficialmente com `GameType.SURVIVAL`.
- Sobrescritos `isInvulnerable()` e `isInvulnerableTo(DamageSource)` para retornar `false`.
- O companheiro recebe dano fisico de monstros, flechas, quedas, fogo e lava, ficando vermelho (flash de dano) e emitindo sons oficiais de ferimento.
- Em caso de morte, emite mensagem no chat e pode ser invocado novamente.
- Atributo `Attributes.STEP_HEIGHT` configurado para `1.06`, permitindo subir relevos de 1 bloco sem engasgar.

### 2.6. Sistema de Skins com Assinaturas Oficiais da Mojang
- O Minecraft 1.20.2+ e 1.21.1 exige assinaturas criptograficas RSA validas emitidas pela Mojang na propriedade `textures`; propriedades sem assinatura sao descartadas pelo cliente, fazendo o modelo reverter para Steve ou Alex.
- Extraimos e integramos payloads Base64 oficiais com assinaturas criptograficas autenticas para 8 presets populares de anime:
  - `Rimuru`, `Goku`, `Luffy`, `Naruto`, `Kirito`, `Zoro`, `Gojo` e `Tanjiro`.
- O comando `/skin <nome>` ou `/companion skin <nome>` aplica o novo perfil com novo UUID e assinatura digital, atualizando a textura no cliente sem fallback.
- O comando `/skin reset` restaura para a skin oficial do jogador proprietario.

### 2.7. Crafting Autonomo com Bau Designado e Coleta de Materiais
- **Bau Designado (`/companion chest` ou `/companion bau`)**: Registra o bau mais proximo como o estoque e deposito do bot.
- **Crafting Autonomo (`/companion craft <item> [qtd]` ou ordens faladas)**:
  1. Verifica se o item ja existe na bolsa do companheiro; se sim, entrega ao dono.
  2. Verifica se o bau designado contem o item pronto; se sim, retira e entrega.
  3. Verifica os materiais necessarios via `RecipeCatalog`.
  4. Se faltar madeira ou pedra, o bot avisa no chat, entra automaticamente no modo `WOOD` ou `MINE`, colhe o necessario na natureza, transforma os insumos, fabrica o item final, entrega nas maos do jogador e volta a segui-lo.

### 2.8. Visao Remota / Camera de Vigia
- Comando `/companion view`: Alterna a visao do jogador para os olhos do companheiro.
- Suporta saida facil executando o comando novamente (toggle) ou via ordens de chat (*"sair da camera"* / *"voltar visao"*).

### 2.9. Interface Grafica com 3 Abas Modulares (`CompanionScreen`)
- Aberta via tecla `C`, icone `[C]` no inventario (`E`) ou comando `/companion gui`.
- **Aba 1 (Acoes & Ordens)**: Botoes de Me Seguir, Ficar Aqui, Defender, Puxar para Ca, Visao Remota, Pega Madeira, Minerar, Plantar, Marcar Bau, Mochila, Guardar e Dispensar. Caixa de chat para digitar ordens.
- **Aba 2 (Skins & Aparencia)**: Campo de texto para skins customizadas, botao Minha Skin, selecao de bracos Classic/Slim e botoes rapidos para os 8 presets de anime.
- **Aba 3 (Modpacks & Integracoes)**: Toggle manual ou auto-deteccao de Tensura, exibindo EP, Magiculas, Rank e botoes de Status e Cerimonia de Nomear quando o mod Tensura estiver ativo.

---

## 3. Como Fizemos (Arquitetura e Implementacao Tecnica)

### 3.1. Arquitetura Modular Desacoplada
O projeto adota separacao estrita de responsabilidades:
- **`:core`**: Codigo Java 8 universal e puro, sem vinculo direto com bibliotecas pesadas de engine. Contem os contratos de IA, maquina de estados de modos (`CompanionMode`), catalogo de presets de skins (`SkinPresetCatalog`), receitas e catalogo de craft (`RecipeCatalog` e `CraftingPlanner`), internacionalizacao (`LocaleService`) e adaptadores de claims de terceiros.
- **`platforms/neoforge-1.21.1`**: Implementacao nativa para NeoForge 21.1.251 (Minecraft 1.21.1) contendo `CompanionServerPlayer`, conexoes simuladas, container de 54 slots, menus, comandos Brigadier e telas JavaFX/Screen.
- **`platforms/forge-1.20.1`**: Implementacao nativa para Minecraft Forge 47.4.0 (Minecraft 1.20.1).
- **`platforms/forge-1.12.2`**: Implementacao para a versao legada Java 8.

### 3.2. Mecanismo de Fake Player (Carpet-Style)
Para evitar sockets de rede reais (que exigiriam porta aberta, consumo de CPU com criptografia TLS/Netty e risco de timeout), utilizamos a arquitetura de servidor fake:
1. `FakeClientConnection`: Estende `Connection` com fluxo `SERVERBOUND` e sobrescreve `send()`, `disconnect()` e `handleDisconnection()` para operacao 100% em memoria sem socket fisico.
2. `CompanionServerPlayer`: Instanciado com `MinecraftServer`, `ServerLevel`, `GameProfile` customizado e `ClientInformation.createDefault()`.
3. Adicionado a `PlayerList` do servidor via `playerList.placeNewPlayer(fakeConn, fakePlayer, cookie)`. Isso faz o proprio motor do Minecraft enviar pacotes de spawn, mensagens de chat e atualizacoes no Tab para todos os jogadores reais conectados.

### 3.3. Algoritmo de Corte em Cascata de Arvores
- **Localizacao da Base**: A busca em `findNearestLog()` calcula o score com base em `(yDiff * 500.0) + horizDist`. Troncos inferiores sempre tem score menor, forçando o bot a se deslocar para a base da arvore.
- **Navegacao Tridimensional Segura**: O bot mede a distancia horizontal `dXZ = Math.hypot(diff.x, diff.z)` e o alcance ocular `getEyePosition().distanceToSqr(targetCenter)`. Caso o tronco esteja acima da cabeca, ele nao tenta andar para frente se a distancia horizontal ja for proxima de zero.
- **Timber Cascade**: Ao quebrar o bloco base com `destroyBlock(pos, true, this)`, o metodo itera imediatamente para cima ate 16 blocos (`currentAbove = targetWorkPos.above()`), quebrando blocos conectados de madeira e fazendo toda a arvore cair como drops.

### 3.4. Container de 54 Slots com Sincronizacao de Equipamento
- O container estende `Container` com tamanho fixo de 54 slots aberto via `ChestMenu.sixRows`.
- Slots 0 a 3 mapeiam para `getItemBySlot(EquipmentSlot.HEAD/CHEST/LEGS/FEET)`.
- Slot 4 mapeia para `getItemBySlot(EquipmentSlot.OFFHAND)`.
- Slot 5 mapeia para `getItemBySlot(EquipmentSlot.MAINHAND)`.
- Slots 9 a 44 mapeiam para `getInventory().getItem(slot - 9)`.
- Ao chamar `setItem(slot, stack)` nos slots 0 a 5, `setItemSlot(EquipmentSlot, stack)` e acionado nativamente, o que faz o servidor Minecraft despachar o pacote `ClientboundSetEquipmentPacket` para todos os clientes, atualizando o modelo 3D no mesmo tick.

### 3.5. Assinaturas Criptograficas de Skins da Mojang
- Consultamos a API de sessoes da Mojang (`https://sessionserver.mojang.com/session/minecraft/profile/<uuid>?unsigned=false`) para obter os pares oficiais de `value` e `signature`.
- Armazenamos as assinaturas em `core/src/main/resources/preset_skins.json` e as registramos estaticamente na classe `SkinPresetCatalog`.
- No `CompanionManager`, a propriedade e aplicada com a assinatura RSA valida:
  `profile.getProperties().put("textures", new Property("textures", preset.getBase64Value(), preset.getSignature()));`
- Isso garante que qualquer cliente que validar as texturas reconheca a chave publica da Mojang e exiba a skin customizada sem recorrer a Steve ou Alex.

---

## 4. O Que Estamos Utilizando (Stack Tecnologica Completa)

### 4.1. Linguagens de Programacao e JVMs
- **Java 21**: Utilizado na plataforma principal `neoforge-1.21.1` com toolchain Gradle configurada.
- **Java 17**: Utilizado na plataforma `forge-1.20.1`.
- **Java 8 (Bytecode 52)**: Utilizado no modulo universal `:core` e na plataforma `forge-1.12.2` para compatibilidade retroativa total.

### 4.2. Sistemas de Build e Automacao
- **Gradle 8.8**: Gerenciador de dependencias, compilacao e empacotamento com wrappers executaveis (`gradlew`, `gradlew.bat`).
- **Plugins Gradle**:
  - `java-library` e `maven-publish`.
  - Tarefas customizadas de empacotamento `jar` e `tensuraJar`.
  - Controle de estrategia de duplicatas `DuplicatesStrategy.EXCLUDE`.

### 4.3. Mod Loaders e Runtimes de Minecraft
- **NeoForge 21.1.251 / 21.1.248**: Loader moderno para Minecraft 1.21.1, utilizando FancyModLoader 4.0.44 e NeoForged Event Bus 8.0.5.
- **Minecraft Forge 47.4.0 / 47.4.10**: Loader estavel para Minecraft 1.20.1.
- **Minecraft Forge 14.23.5.2860**: Loader para Minecraft 1.12.2.

### 4.4. Bibliotecas e Componentes Internos
- **DataComponents (Minecraft 1.21.1)**:
  - `DataComponents.FOOD` e `FoodProperties` para analise de nutricao e alimentacao automatica.
- **Mojang Brigadier 1.3.10**: Motor oficial de arvore de comandos tipados, argumentos com autocompletar e sugestoes (`CommandDispatcher`, `Commands`, `StringArgumentType`, `IntegerArgumentType`).
- **Mojang AuthLib 6.0.54**: Manipulacao de `GameProfile` e propriedades de textura com assinaturas criptograficas (`Property`).
- **Google Gson 2.10.1**: Serializacao e desserializacao JSON leve para cache de skins e configuracoes.
- **Google Guava 32.1.2-jre**: Estruturas de dados concorrentes e utilitarios.
- **FastUtil 8.5.12**: Colecoes de alta performance para tipos primitivos.
- **Netty 4.1.97.Final**: Infraestrutura de rede e buffers de pacotes.
- **SLF4J 2.0.9**: Registro estruturado de logs do servidor e mod.
- **JOML 1.10.5**: Calculos de matrizes e geometria 3D.
- **LWJGL 3.3.3 e GLFW**: Renderizacao grafica OpenGL, leitura de teclado e eventos de interface no cliente.
- **JUnit 5 (Jupiter 5.10.2)**: Framework de testes automatizados unitarios e integrados.

### 4.5. Modpacks Integrados
1. **TesteMineFrinds**: Instancia dedicada no CurseForge para validacao de jogabilidade limpa (Vanilla) no NeoForge 1.21.1.
2. **Tensura Neo Otherworld**: Modpack complexo no NeoForge 1.21.1 com integracao de Valor de Existencia (EP), magiculas, protecao de claims FTB Teams e ceremonias de nomeacao.
3. **Tensura All The Slime 1**: Modpack alternativo com o mod Tensura no NeoForge 1.21.1.

---

## 5. Matriz de Artefatos e Hashes SHA-256

| Pacote JAR | Loader | Minecraft | Edicao / Destino | Tamanho | SHA-256 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `companions-neoforge-1.21.1-0.1.0.jar` | NeoForge 21.1.251 | 1.21.1 | Padrao / TesteMineFrinds | 151.108 bytes | `9255137EA5D8CD22F149CBD11A38C1CFC2D5417A5E237FC0BF49184C89F1A184` |
| `companions-neoforge-1.21.1-tensura-neo-otherworld-0.1.0.jar` | NeoForge 21.1.251 | 1.21.1 | Adaptada Tensura Neo Otherworld | 151.150 bytes | `F670E14E9D1A7838E010EE1DA6AB46A97B8CAAA3F2F1834819ADA67F8893D3DF` |
| `companions-forge-1.20.1-0.1.0.jar` | Forge 47.4.0+ | 1.20.1 | Padrao / Forge 1.20.1 | 112.884 bytes | `3A904FAC38392684C5F27F578754FF22CFAAA71519878BD651788770856E6CA4` |
| `companions-forge-1.12.2-0.1.0.jar` | Forge 14.23.5 | 1.12.2 | Legada / Java 8 | 12.398 bytes | `F8FCA6016E7D8A2E7CBB56997AB7D05EBFF13E15DC04F27AB2F1EC1136473530` |

---

## 6. Como Executar e Testar

### 6.1. Abrir a Interface Grafica (GUI)
- Pressione a tecla **`C`** em jogo com o companheiro ativo.
- Ou clique no icone **`[C]`** localizado na lateral da tela de inventario (tecla `E`).
- Ou digite no chat: `/companion gui`.

### 6.2. Comandos Principais
- Invocacao: `/companion spawn [nome]` ou `/companion lan [nome]`.
- Modos de Postura: `/companion mode <follow|stay|defend>`.
- Modos de Trabalho: `/companion action <wood|mine|farm>`.
- Bau Designado: `/companion chest` (ou `/companion bau`).
- Crafting Autonomo: `/companion craft <item> [qtd]`.
- Mochila e Equipamentos: `/companion inventory`.
- Descarregar Recursos: `/companion deposit`.
- Visao Remota da Camera: `/companion view`.
- Troca de Skin: `/skin <nome>` (ex: `Rimuru`, `Goku`, `Luffy`, `Naruto`, `Kirito`, `Zoro`, `Gojo`, `Tanjiro`) e `/skin reset`.
- Dispensar Companheiro: `/companion dismiss`.
