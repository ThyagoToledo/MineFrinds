# Avaliação local de intenções

`decision-corpus-seed.jsonl` é um conjunto inicial revisável para o R8. Cada
linha contém idioma, família semântica, texto, rótulo fechado e indicação de
abstenção esperada. Ele não deve ser usado como prova de precisão: paráfrases
da mesma família ainda precisam de revisão humana e divisão por jogador/família
antes de treino, calibração e teste.

O próximo passo é expandir este seed para pelo menos 600 exemplos equilibrados
em pt-BR/en-US, mantendo famílias de negação, conversa casual, ordem incompleta,
quantidade e itens de mods separadas entre treino, calibração e teste. Nenhum
exemplo deve conter UUID, estado privado do mundo ou segredo. O scorer Java deve
ser avaliado pelo mesmo JSONL sem chamar um modelo remoto; métricas mínimas são
macro-F1 por idioma, precisão das decisões aceitas, cobertura, taxa de
abstenção, matriz de confusão, latência e memória.

Para gerar uma base sintética reproduzível antes da revisão humana, use:

```text
python tools/generate-decision-corpus.py --count 600 --output decision-corpus.generated.jsonl
```

O gerador grava divisão determinística em `train`, `calibration` e `test`. O
arquivo gerado não deve ser versionado como evidência final sem revisão das
paráfrases e inclusão de casos de negação, modpacks e ordens incompletas.
