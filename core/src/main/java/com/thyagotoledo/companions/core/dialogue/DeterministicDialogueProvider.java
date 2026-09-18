package com.thyagotoledo.companions.core.dialogue;

import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionProfile;
import com.thyagotoledo.companions.core.model.InventorySnapshot;

import java.util.regex.Pattern;

public final class DeterministicDialogueProvider {
    private static final Pattern PATTERN_FOLLOW_PT = Pattern.compile(".*(me\\s*segue|vem\\s*comigo|anda\\s*comigo|me\\s*acompanha).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FOLLOW_EN = Pattern.compile(".*(follow\\s*me|walk\\s*with\\s*me).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_STAY_PT = Pattern.compile(".*(fica\\s*aqui|espera|para|fica\\s*parado|senta).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_STAY_EN = Pattern.compile(".*(stay\\s*here|wait|stop|sit).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_DEFEND_PT = Pattern.compile(".*(defenda|proteja|protege|fique\\s*atento|guarda).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DEFEND_EN = Pattern.compile(".*(defend|protect|guard|watch\\s*out).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_RECALL_PT = Pattern.compile(".*(vem\\s*c[aá]|venha\\s*aqui|vem\\s*aqui|puxar|me\\s*alcance|chamar).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_RECALL_EN = Pattern.compile(".*(come\\s*here|come\\s*to\\s*me|recall|pull\\s*near).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_VIEW_PT = Pattern.compile(".*(vis[aã]o|olhar|olhe|ver\\s*pelos\\s*olhos|c[aâ]mera).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_VIEW_EN = Pattern.compile(".*(view|remote\\s*view|scout|look\\s*through|camera).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_INVENTORY_PT = Pattern.compile(".*(invent[aá]rio|mochila|bolsa|abrir\\s*itens).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_INVENTORY_EN = Pattern.compile(".*(inventory|backpack|open\\s*bag|items).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_STATUS_PT = Pattern.compile(".*(status|relat[oó]rio|como\\s*voc[eê]\\s*est[aá]|situa[cç][aã]o).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_STATUS_EN = Pattern.compile(".*(status|report|how\\s*are\\s*you|condition).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_WOOD_PT = Pattern.compile(".*(pega\\s*madeira|corta\\s*madeira|coleta\\s*madeira).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_WOOD_EN = Pattern.compile(".*(chop\\s*wood|gather\\s*wood|get\\s*wood|cut\\s*trees).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_DEPOSIT_PT = Pattern.compile(".*(guarda\\s*os\\s*itens|deposita|descarrega|guardar\\s*ba[uú]).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DEPOSIT_EN = Pattern.compile(".*(deposit|store\\s*items|put\\s*in\\s*chest).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_QUEST_PT = Pattern.compile(".*(ajuda\\s*com\\s*(a\\s*)?quest|miss[aã]o|o\\s*que\\s*falta).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_QUEST_EN = Pattern.compile(".*(help\\s*with\\s*quest|quest\\s*help|what\\s*is\\s*missing).*", Pattern.CASE_INSENSITIVE);

    private final LocaleService localeService;

    public DeterministicDialogueProvider(LocaleService localeService) {
        this.localeService = localeService != null ? localeService : new LocaleService();
    }

    public DialogueResponse process(String input, String preferredLocale, CompanionProfile profile, InventorySnapshot inventory) {
        String locale = localeService.normalizeLocale(preferredLocale);
        if (input == null || input.trim().isEmpty()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.empty"), new Intent(IntentType.CASUAL_CHAT));
        }

        String trimmed = input.trim();

        if (PATTERN_FOLLOW_PT.matcher(trimmed).matches() || PATTERN_FOLLOW_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.follow_ack"), new Intent(IntentType.FOLLOW_OWNER));
        }

        if (PATTERN_STAY_PT.matcher(trimmed).matches() || PATTERN_STAY_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.stay_ack"), new Intent(IntentType.STAY));
        }

        if (PATTERN_DEFEND_PT.matcher(trimmed).matches() || PATTERN_DEFEND_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.defend_ack"), new Intent(IntentType.DEFEND));
        }

        if (PATTERN_RECALL_PT.matcher(trimmed).matches() || PATTERN_RECALL_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.recall_ack"), new Intent(IntentType.RECALL));
        }

        if (PATTERN_VIEW_PT.matcher(trimmed).matches() || PATTERN_VIEW_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.remote_view_start"), new Intent(IntentType.REMOTE_VIEW));
        }

        if (PATTERN_INVENTORY_PT.matcher(trimmed).matches() || PATTERN_INVENTORY_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.inventory_open"), new Intent(IntentType.OPEN_INVENTORY));
        }

        if (PATTERN_STATUS_PT.matcher(trimmed).matches() || PATTERN_STATUS_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.status_report"), new Intent(IntentType.REPORT_STATUS));
        }

        if (PATTERN_WOOD_PT.matcher(trimmed).matches() || PATTERN_WOOD_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.wood_ack"), new Intent(IntentType.CHOP_WOOD, "minecraft:oak_log", 16));
        }

        if (PATTERN_DEPOSIT_PT.matcher(trimmed).matches() || PATTERN_DEPOSIT_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.deposit_ack"), new Intent(IntentType.DEPOSIT_CHEST));
        }

        if (PATTERN_QUEST_PT.matcher(trimmed).matches() || PATTERN_QUEST_EN.matcher(trimmed).matches()) {
            return new DialogueResponse(locale, localeService.translate(locale, "dialogue.quest_ack"), new Intent(IntentType.ASSIST_SELECTED_QUEST));
        }

        return new DialogueResponse(locale, localeService.translate(locale, "dialogue.unknown_ack"), new Intent(IntentType.UNKNOWN_OR_BLOCKED));
    }
}
