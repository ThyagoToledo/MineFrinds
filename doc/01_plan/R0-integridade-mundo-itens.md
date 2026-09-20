---
tags: [minecraft, minefriends, r0, crafting, claims, inventario]
updated: 2026-09-19
status: parcialmente-concluido-r0-1-pendente
---

# R0 — Integridade do mundo e dos itens

## Resultado esperado

Toda ação do companheiro conserva itens, respeita proteção territorial e falha sem modificar parcialmente o mundo. Crafting, coleta, plantio, baús e recall usam uma única camada de execução validada.

## Estado atual

O commit `98046b0` removeu a duplicação direta da saída, rejeitou itens desconhecidos, adicionou pré-checagens, verificações locais de bloco e recall com cooldown/posição segura. R0 permanece parcial porque permissões reais de FTB Chunks/OPAC não estão integradas, mutações ainda usam chamadas diretas e receitas continuam codificadas manualmente.

## Entregas

### R0.1 — executor de ações

- Criar no core `ActionRequest`, `ActionResult`, `BlockAction`, `InventoryMutation` e motivos tipados de bloqueio.
- Criar `WorldActionExecutor` em cada plataforma.
- Quebra no NeoForge deve passar pelo fluxo do fake player/GameMode ou pelos hooks oficiais equivalentes, preservando ferramenta, durabilidade, drops, XP e eventos.
- Plantio e uso de baú passam pelo fluxo de interação e verificam claim, distância, chunk carregado e dono.
- Falha desconhecida é `DENIED`, nunca permissão presumida.

### R0.2 — crafting real e transacional

- Consultar `RecipeManager` depois de datapacks/KubeJS.
- Suportar inicialmente shaped/shapeless simples, quantidade de saída e itens restantes.
- Recusar receitas especiais, componentes dinâmicos e máquinas sem adaptador.
- `CraftTransaction` executa: snapshot → plano → simulação de destino → reserva → validação de versão → consumo → inserção única → resultado.
- Após crash/cancelamento, reconciliar pelo inventário real; nunca repetir cegamente.

### R0.3 — recall seguro comum

- Extrair política comum: mesmo dono, mesma dimensão por padrão, não em combate, cooldown, chunk carregado, piso sólido, volume livre e borda do mundo.
- Primeiro tentar navegação; teleporte é recuperação configurável.
- Não forçar chunks nem cruzar claim para procurar destino.

## Ordem de implementação

1. Escrever testes que reproduzam duplicação, retirada parcial e claim negado.
2. Introduzir contratos do core sem mudar comportamento.
3. Implementar executor NeoForge e migrar WOOD/MINE/FARM/baús.
4. Implementar adaptador de receitas e transação.
5. Portar o contrato ao Forge 1.20.1; documentar subset 1.12.2.
6. Executar GameTests e roteiro manual nos packs.

## Testes obrigatórios

- inventário cheio, saída com quantidade >1, recipiente restante e item com componentes/NBT;
- cancelamento antes e depois da reserva, morte e stop/start;
- baú removido ou alterado entre simulação e commit;
- claim permitido/negado em FTB Chunks e OPAC;
- árvore atravessando dois claims;
- receita KubeJS recarregada;
- recall em lava, vazio, parede, combate, outra dimensão e cooldown.

## Evidência para conclusão

Logs e GameTests com contagem antes/depois, versões dos mods de claim, IDs das receitas, hash do JAR e roteiro manual em cópia descartável. Teste unitário de `DefaultPermissionService` não comprova claim real.

## Critério de aceite

Zero duplicação/perda nos cenários suportados; claim negado não muda bloco/inventário; crafting usa receita efetiva; recall nunca força chunk nem cria destino perigoso. Só então remover a marca experimental das ações destrutivas.

## Dependências e saída

Não depende de R2. Fornece a camada de execução necessária para que intenções da IA nunca atuem diretamente no mundo. Responsável seguinte: R1.1 pode usar seus `ActionResult` no feedback.

## Progresso executado em 19/09/2026

- `executeAutonomousCraft` passou a contabilizar o saldo já retirado da bolsa/baú antes de calcular ingredientes; uma solicitação parcial não fabrica novamente a quantidade já entregue.
- O core recebeu `CraftTransaction`, plano imutável com `SUCCESS`, `INVALID`, `INSUFFICIENT_INPUT` e `OUTPUT_FULL`; o commit aplica consumo e saída somente depois de todas as pré-condições, preservando o inventário em falhas.

Isso fecha o contrato de quantidade exata e cria o primeiro adaptador puro para a futura integração com `RecipeManager`. A receita NeoForge ainda usa a tabela manual existente; a troca para receitas efetivas, itens restantes e rollback de `ItemStack` continua necessária para declarar R0 concluído.
