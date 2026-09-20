package com.thyagotoledo.companions.core.model;

import com.thyagotoledo.companions.core.dialogue.IntentType;

import java.util.Locale;
import java.util.UUID;

/** Intenção limitada que cruza a fronteira C2S; texto livre nunca vira comando do servidor. */
public final class CompanionCommandRequest {
    private final UUID requestId;
    private final UUID companionId;
    private final IntentType intent;
    private final String target;
    private final int quantity;
    private final String locale;
    private final long expectedRevision;

    public CompanionCommandRequest(UUID requestId, UUID companionId, IntentType intent,
                                   String target, int quantity, String locale, long expectedRevision) {
        this.requestId = requestId;
        this.companionId = companionId;
        this.intent = intent != null ? intent : IntentType.UNKNOWN_OR_BLOCKED;
        this.target = limit(target, 128);
        this.quantity = Math.max(0, Math.min(64, quantity));
        this.locale = normalizeLocale(locale);
        this.expectedRevision = Math.max(0L, expectedRevision);
    }

    public static CompanionCommandRequest fromText(UUID requestId, UUID companionId, String raw,
                                                   String locale, long expectedRevision) {
        if (raw == null) return invalid(requestId, companionId, locale, expectedRevision);
        String text = raw.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/")) lower = lower.substring(1).trim();
        if (lower.startsWith("companion chat ")) {
            String speech = text.substring(Math.min(text.length(), text.toLowerCase(Locale.ROOT).indexOf("companion chat ") + 15)).trim();
            return new CompanionCommandRequest(requestId, companionId, IntentType.CASUAL_CHAT, speech, 0, locale, expectedRevision);
        }
        if (lower.equals("me segue") || lower.equals("follow") || lower.endsWith(" mode follow")) {
            return known(requestId, companionId, IntentType.FOLLOW_OWNER, locale, expectedRevision);
        }
        if (lower.equals("fica aqui") || lower.equals("stay") || lower.endsWith(" mode stay")) {
            return known(requestId, companionId, IntentType.STAY, locale, expectedRevision);
        }
        if (lower.equals("defend") || lower.contains("defenda") || lower.endsWith(" mode defend")) {
            return known(requestId, companionId, IntentType.DEFEND, locale, expectedRevision);
        }
        if (lower.endsWith(" recall") || lower.equals("recall")) {
            return known(requestId, companionId, IntentType.RECALL, locale, expectedRevision);
        }
        if (lower.endsWith(" view") || lower.equals("view")) {
            return known(requestId, companionId, IntentType.REMOTE_VIEW, locale, expectedRevision);
        }
        if (lower.endsWith(" inventory") || lower.equals("inventory")) {
            return known(requestId, companionId, IntentType.OPEN_INVENTORY, locale, expectedRevision);
        }
        if (lower.endsWith(" deposit") || lower.equals("deposit") || lower.contains("guardar")) {
            return known(requestId, companionId, IntentType.DEPOSIT_CHEST, locale, expectedRevision);
        }
        if (lower.endsWith(" action wood") || lower.equals("wood") || lower.contains("madeira")) {
            return known(requestId, companionId, IntentType.CHOP_WOOD, locale, expectedRevision);
        }
        if (lower.endsWith(" action mine") || lower.equals("mine") || lower.contains("minerar")) {
            return known(requestId, companionId, IntentType.MINE_BLOCK, locale, expectedRevision);
        }
        if (lower.endsWith(" action farm") || lower.equals("farm") || lower.contains("plantar")) {
            return known(requestId, companionId, IntentType.COLLECT_ITEMS, locale, expectedRevision);
        }
        return invalid(requestId, companionId, locale, expectedRevision);
    }

    private static CompanionCommandRequest known(UUID requestId, UUID companionId, IntentType intent,
                                                 String locale, long revision) {
        return new CompanionCommandRequest(requestId, companionId, intent, null, 0, locale, revision);
    }

    private static CompanionCommandRequest invalid(UUID requestId, UUID companionId, String locale, long revision) {
        return known(requestId, companionId, IntentType.UNKNOWN_OR_BLOCKED, locale, revision);
    }

    private static String limit(String value, int max) {
        if (value == null) return "";
        String clean = value.replaceAll("[\\p{Cntrl}&&[^\\t]]", "").trim();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    private static String normalizeLocale(String value) {
        if (value == null) return "pt_br";
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("en") ? "en_us" : "pt_br";
    }

    public boolean isValid() { return intent != IntentType.UNKNOWN_OR_BLOCKED; }
    public UUID getRequestId() { return requestId; }
    public UUID getCompanionId() { return companionId; }
    public IntentType getIntent() { return intent; }
    public String getTarget() { return target; }
    public int getQuantity() { return quantity; }
    public String getLocale() { return locale; }
    public long getExpectedRevision() { return expectedRevision; }
}
