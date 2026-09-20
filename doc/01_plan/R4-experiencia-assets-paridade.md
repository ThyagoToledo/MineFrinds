---
tags: [minecraft, minefriends, r4, interface, skins, assets, acessibilidade]
updated: 2026-09-19
status: prototipo-parcial-planejado
---

# R4 — Experiência, assets e paridade

## Resultado esperado

O jogador administra o companheiro por painel ou chat em pt-BR/en-US, entende estado e bloqueios e usa assets leves e licenciados em todas as versões suportadas.

## Painel final

- Lista de companheiros com retrato, vida, modo, tarefa, distância e disponibilidade.
- Abas: visão geral, ordens, inventário, quests, memória, aparência e configurações.
- Fila visível de até cinco ordens e cancelamento determinístico.
- Botões enviam `CompanionCommandRequest`; feedback só muda para sucesso após resposta do servidor.
- Motivos desativados aparecem em tooltip/texto.
- Chat e interface usam o mesmo `IntentType`.

## Câmera

- Modo padrão troca a câmera principal para o NPC; corpo do jogador permanece vulnerável.
- HUD mostra NPC, vida, tarefa e tecla de retorno.
- Sair em `Esc`, tecla configurável, morte, unload, dimensão ou negação.
- Não forçar chunks nem fornecer visão de dados que o cliente não recebeu.
- Picture-in-picture fica experimental, uma janela 320×180/5–10 FPS, desligada por padrão e condicionada a benchmark/shaders.

## Assets

- Quatro skins originais 64×64: explorador, guardião, artesão e guia.
- Modelo humanoide vanilla no MVP; GeckoLib apenas com benefício medido posterior.
- Ícones 16/32 px, atlas pequeno, sons OGG curtos e item de vínculo vanilla JSON.
- Fontes editáveis em `assets-source`, autoria/licença e checklist UV/slim/classic.
- Presets/nicknames de terceiros são opcionais e sujeitos a licença/termos; não são identidade visual oficial.

## Idiomas e acessibilidade

- Paridade de chaves `pt_br`/`en_us`, placeholders validados e `.lang` gerado no legado.
- Estado comunicado por texto/forma/ícone, não só cor.
- Testar teclado, foco, tooltips, 16:9, 4:3 e escalas GUI 2–4.
- Não cortar texto crítico nem misturar idiomas na mesma fala.

## Paridade

- NeoForge 1.21.1 é referência funcional.
- Forge 1.20.1 recebe comandos, snapshot, painel e capacidades selecionadas.
- Forge 1.12.2 publica subset explícito; não simular recurso ausente.
- A GUI mostra capability por servidor/plataforma e oculta ação indisponível.

## Testes

- snapshots atrasados, feedback de erro e servidor sem integração;
- dois jogadores em idiomas diferentes;
- câmera com morte, dano, dimensão, shaders e fechamento de tela;
- skins com armadura, mãos, slim/classic e reload;
- comparação visual em resoluções e escala.

## Critério de aceite

Nenhum dado falso, todas as ações têm feedback confirmado, câmera sempre tem saída, assets são licenciados e leves, idiomas completos e matriz de recursos publicada.

## Dependências

Depende do protocolo R1 e resultados R0; integra status de R2 e limites medidos em R3.

## Progresso executado em 19/09/2026

- A GUI NeoForge passou a enviar ordens de modo, trabalho e conversa pelo `CommandPayload` tipado, usando o UUID e a revisão do snapshot ativo.
- Ordens administrativas que ainda não têm `IntentType` próprio mantêm fallback para o comando vanilla, evitando bloquear recursos existentes durante a migração.
