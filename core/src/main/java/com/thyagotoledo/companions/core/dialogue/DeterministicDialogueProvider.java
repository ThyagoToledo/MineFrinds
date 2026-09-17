package com.thyagotoledo.companions.core.dialogue;

import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionProfile;
import com.thyagotoledo.companions.core.model.InventorySnapshot;

import java.util.regex.Pattern;

public final class DeterministicDialogueProvider {
    private static final Pattern PATTERN_FOLLOW_PT = Pattern.compile(".*(me\\s*segue|vem\\s*c[aá]|anda\\s*comigo|me\\s*acompanha).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FOLLOW_EN = Pattern.compile(".*(follow\\s*me|come\\s*here|walk\\s*with\\s*me).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_STAY_PT = Pattern.compile(".*(fica\\s*aqui|espera|para|fica\\s*parado|senta).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_STAY_EN = Pattern.compile(".*(stay\\s*here|wait|stop|sit).*", Pattern.CASE_INSENSITIVE);

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
