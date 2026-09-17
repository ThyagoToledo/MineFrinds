package com.thyagotoledo.companions.core.locale;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocaleService {
    public static final String DEFAULT_LOCALE = "en_us";
    public static final String PT_BR = "pt_br";
    public static final String EN_US = "en_us";

    private final Map<String, Map<String, String>> bundles = new HashMap<>();

    public LocaleService() {
        loadBundle(EN_US, "/assets/companions/lang/en_us.json");
        loadBundle(PT_BR, "/assets/companions/lang/pt_br.json");
    }

    public void registerTranslation(String locale, String key, String text) {
        if (locale == null || key == null || text == null) return;
        String normLocale = locale.toLowerCase().trim();
        bundles.computeIfAbsent(normLocale, k -> new HashMap<>()).put(key, text);
    }

    public String translate(String locale, String key, Object... args) {
        if (key == null) return "";
        String normLocale = normalizeLocale(locale);

        Map<String, String> primary = bundles.get(normLocale);
        String template = primary != null ? primary.get(key) : null;

        if (template == null) {
            Map<String, String> fallback = bundles.get(DEFAULT_LOCALE);
            template = fallback != null ? fallback.get(key) : null;
        }

        if (template == null) {
            return key;
        }

        return format(template, args);
    }

    public String normalizeLocale(String rawLocale) {
        if (rawLocale == null) return DEFAULT_LOCALE;
        String lower = rawLocale.toLowerCase().replace('-', '_').trim();
        if (lower.startsWith("pt")) return PT_BR;
        return EN_US;
    }

    private String format(String template, Object... args) {
        if (args == null || args.length == 0) return template;
        String result = template;
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";
            String val = args[i] != null ? args[i].toString() : "null";
            result = result.replace(placeholder, val);
        }
        return result;
    }

    private void loadBundle(String locale, String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                parseFlatJson(locale, sb.toString());
            }
        } catch (Exception ignored) {
            // Em caso de falha de carregamento, bundle permanece vazia
        }
    }

    private void parseFlatJson(String locale, String json) {
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2);
            registerTranslation(locale, key, val);
        }
    }
}
