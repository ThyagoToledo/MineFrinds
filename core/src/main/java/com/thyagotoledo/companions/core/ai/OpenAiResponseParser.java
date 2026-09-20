package com.thyagotoledo.companions.core.ai;

import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.Intent;
import com.thyagotoledo.companions.core.dialogue.IntentType;

/** Parser pequeno e sem dependencias para manter compatibilidade com Forge 1.12.2. */
public final class OpenAiResponseParser {
    private OpenAiResponseParser() { }

    public static DialogueResponse parse(String raw, String locale, DialogueResponse fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            String json = raw.trim();
            if (json.contains("\"choices\"")) {
                String content = extractStringField(json, "content");
                if (content == null) content = extractStringField(json, "text");
                if (content == null) return fallback;
                json = stripFence(content);
            }

            String intentName = extractStringField(json, "intent");
            String speech = extractStringField(json, "speech");
            if (intentName == null || speech == null) return fallback;

            IntentType type;
            try { type = IntentType.valueOf(intentName); }
            catch (Exception ignored) { return fallback; }

            if (speech.length() > 256) speech = speech.substring(0, 256);
            speech = removeControls(speech);
            if (speech.trim().isEmpty()) return fallback;
            return new DialogueResponse(locale, speech, new Intent(type));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String extractStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        int colon = json.indexOf(':', start + key.length());
        if (colon < 0) return null;
        int value = colon + 1;
        while (value < json.length() && Character.isWhitespace(json.charAt(value))) value++;
        if (value >= json.length() || json.charAt(value) != '"') return null;

        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = value + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    default: result.append(c); break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return result.toString();
            } else {
                result.append(c);
            }
        }
        return null;
    }

    private static String stripFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```") && text.endsWith("```")) {
            int newline = text.indexOf('\n');
            text = newline >= 0 ? text.substring(newline + 1, text.length() - 3) : text.substring(3, text.length() - 3);
        }
        return text.trim();
    }

    private static String removeControls(String value) {
        StringBuilder clean = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || !Character.isISOControl(c)) clean.append(c);
        }
        return clean.toString();
    }
}
