#!/usr/bin/env python3
"""Gera um corpus local e reproduzível para avaliar o adapter de intents.

O arquivo gerado é apenas material de desenvolvimento: as paráfrases sintéticas
precisam de revisão humana antes de serem usadas como teste de release.
"""

import argparse
import hashlib
import json
from pathlib import Path


TEMPLATES = {
    "FOLLOW_OWNER": {
        "pt-BR": ["me segue", "vem comigo", "acompanhe meu caminho"],
        "en-US": ["follow me", "come with me", "walk beside me"],
    },
    "STAY": {"pt-BR": ["fica aqui", "espere neste lugar", "não saia daqui"],
             "en-US": ["stay here", "wait at this spot", "do not leave this place"]},
    "DEFEND": {"pt-BR": ["defenda-me", "proteja nossa base", "fique atento aos monstros"],
               "en-US": ["defend me", "protect our base", "watch for hostile mobs"]},
    "COLLECT_ITEMS": {"pt-BR": ["recolha os itens", "pegue os drops", "junte os objetos no chão"],
                       "en-US": ["collect the items", "pick up the drops", "gather objects from the ground"]},
    "CHOP_WOOD": {"pt-BR": ["pegue madeira", "corte os troncos", "colete madeira da árvore"],
                   "en-US": ["gather wood", "cut the logs", "collect wood from the tree"]},
    "MINE_BLOCK": {"pt-BR": ["minere ferro", "procure diamantes", "cave este minério"],
                    "en-US": ["mine iron", "look for diamonds", "dig for this ore"]},
    "DEPOSIT_CHEST": {"pt-BR": ["guarde tudo no baú", "deposite os itens", "coloque isso no chest marcado"],
                       "en-US": ["store everything in the chest", "deposit the items", "put this in the marked chest"]},
    "ASSIST_SELECTED_QUEST": {"pt-BR": ["ajude com a missão", "veja o que falta na quest", "cumpra este objetivo"],
                               "en-US": ["help with the quest", "check what the quest needs", "complete this objective"]},
    "RECALL": {"pt-BR": ["venha para perto", "volte para mim", "aproxime-se agora"],
                "en-US": ["come closer", "return to me", "come over now"]},
    "REMOTE_VIEW": {"pt-BR": ["quero ver pela sua câmera", "mostre sua visão", "deixe-me observar"],
                     "en-US": ["show me your camera", "let me see your view", "let me observe"]},
    "OPEN_INVENTORY": {"pt-BR": ["abra sua mochila", "mostre o inventário", "quero ver seus itens"],
                        "en-US": ["open your backpack", "show the inventory", "let me see your items"]},
    "REPORT_STATUS": {"pt-BR": ["qual é o seu status", "dê um relatório", "como você está"],
                       "en-US": ["what is your status", "give me a report", "how are you doing"]},
    "CASUAL_CHAT": {"pt-BR": ["conte uma história", "o que podemos construir", "como está o mundo"],
                     "en-US": ["tell me a story", "what can we build", "how is the world"]},
    "UNKNOWN_OR_BLOCKED": {"pt-BR": ["faça alguma coisa", "pegue isso", "resolva o problema"],
                            "en-US": ["do something", "get that", "solve the problem"]},
}

PREFIXES = {
    "pt-BR": ["", "por favor ", "agora ", "você pode "],
    "en-US": ["", "please ", "now ", "can you "],
}
SUFFIXES = {"pt-BR": ["", " por favor", " agora", " para mim"],
            "en-US": ["", " please", " now", " for me"]}


def split_for(record_id):
    value = int(hashlib.sha256(record_id.encode("utf-8")).hexdigest()[:8], 16) % 10
    return "test" if value < 2 else "calibration" if value == 2 else "train"


def generate(count):
    records = []
    seen = set()
    rounds = 0
    while len(records) < count and rounds < 1000:
        for label, locales in TEMPLATES.items():
            for locale, phrases in locales.items():
                for phrase in phrases:
                    prefix = PREFIXES[locale][rounds % len(PREFIXES[locale])]
                    suffix = SUFFIXES[locale][(rounds // len(PREFIXES[locale])) % len(SUFFIXES[locale])]
                    text = (prefix + phrase + suffix).strip()
                    key = (locale, label, text)
                    if key in seen:
                        continue
                    seen.add(key)
                    record_id = f"{locale.lower()}-{label.lower()}-{len(records):04d}"
                    records.append({"id": record_id, "locale": locale, "family":
                                    "casual" if label == "CASUAL_CHAT" else
                                    "ambiguous" if label == "UNKNOWN_OR_BLOCKED" else "order",
                                    "text": text, "label": label,
                                    "expected_abstain": label in {"CASUAL_CHAT", "UNKNOWN_OR_BLOCKED"},
                                    "split": split_for(record_id)})
                    if len(records) >= count:
                        return records
        rounds += 1
    return records


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=600)
    parser.add_argument("--output", type=Path, default=Path("decision-corpus.generated.jsonl"))
    args = parser.parse_args()
    records = generate(max(1, args.count))
    args.output.write_text("".join(json.dumps(record, ensure_ascii=False) + "\n" for record in records), encoding="utf-8")
    print(f"generated={len(records)} output={args.output}")


if __name__ == "__main__":
    main()
