package com.thyagotoledo.companions.core.skin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Catalogo de presets de skins populares (animes e personagens conhecidos)
 * contendo texturas validas codificadas em Base64 no padrao oficial da Mojang.
 * Compilavel em Java 8 para compatibilidade universal entre plataformas.
 */
public class SkinPresetCatalog {

    public static final class PresetSkin {
        private final String name;
        private final String textureUrl;
        private final boolean slim;
        private final String base64Value;

        public PresetSkin(String name, String textureUrl, boolean slim, String base64Value) {
            this.name = name;
            this.textureUrl = textureUrl;
            this.slim = slim;
            this.base64Value = base64Value;
        }

        public String getName() {
            return name;
        }

        public String getTextureUrl() {
            return textureUrl;
        }

        public boolean isSlim() {
            return slim;
        }

        public String getBase64Value() {
            return base64Value;
        }
    }

    private static final Map<String, PresetSkin> PRESETS = new HashMap<>();

    static {
        register("Rimuru", "http://textures.minecraft.net/texture/c698bbffddb2fcf757041a34d85203798991206d2ecdf1f074d257242c759085", true);
        register("Goku", "http://textures.minecraft.net/texture/4ab3243da8340d04db73c6a51d2fdfad25946fe5dafc5aa84138e0586e92751f", false);
        register("Luffy", "http://textures.minecraft.net/texture/f875b1c905ba2aa61dfcb56950fb21eb55fe795cfab72f53472fa9ce94a613f3", false);
        register("Naruto", "http://textures.minecraft.net/texture/4f4e782b544485521e14945aa63cb4be7e0e7a2b97f0f6227b3b3a6ef06e4088", false);
        register("Kirito", "http://textures.minecraft.net/texture/1908846c4fa103328ce7ca3a6771d9d40b5fa8bc42f1f50a9ee2b2512f45ec75", false);
        register("Zoro", "http://textures.minecraft.net/texture/4e08cf442cb91a457a41aa1d575c363d3be2d0cf3b1b9e27c191a27e7f6e4d58", false);
        register("Gojo", "http://textures.minecraft.net/texture/967d30d18f5ec47f3b89b14c330f55ff33d06b0098f98db3950efcf8438db02b", true);
        register("Tanjiro", "http://textures.minecraft.net/texture/3bcad6d66e744d03e30f4cf8021da6a7db1be5dcbc42784cf0d6d538e1b3d687", false);
    }

    private static void register(String name, String textureUrl, boolean slim) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"" + (slim ? ",\"metadata\":{\"model\":\"slim\"}" : "") + "}}}";
        String b64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        PRESETS.put(name.toLowerCase(Locale.ROOT), new PresetSkin(name, textureUrl, slim, b64));
    }

    public static PresetSkin getPreset(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return PRESETS.get(name.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean hasPreset(String name) {
        if (name == null) return false;
        return PRESETS.containsKey(name.trim().toLowerCase(Locale.ROOT));
    }

    public static Set<String> getAvailablePresetNames() {
        return Collections.unmodifiableSet(PRESETS.keySet());
    }

    public static String buildCustomTextureBase64(String textureUrl, boolean isSlim) {
        if (textureUrl == null || textureUrl.trim().isEmpty()) {
            return null;
        }
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl.trim() + "\"" + (isSlim ? ",\"metadata\":{\"model\":\"slim\"}" : "") + "}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
