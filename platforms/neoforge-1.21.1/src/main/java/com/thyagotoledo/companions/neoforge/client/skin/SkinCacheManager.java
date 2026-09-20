package com.thyagotoledo.companions.neoforge.client.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Gerenciador assincrono de cache de skins para o mod Companions.
 * Permite buscar e armazenar skins de contas de Minecraft ou personagens de anime
 * com zero lag de tick ou FPS e suporte a operacao offline.
 */
public class SkinCacheManager {

    public static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    private static Path cacheDir;
    private static final Map<String, LoadedSkin> MEMORY_CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "companion-skin-loader");
        t.setDaemon(true);
        return t;
    });

    public record LoadedSkin(String skinKey, boolean isSlim) {
        public ResourceLocation getTextureLocation() {
            return ResourceLocation.fromNamespaceAndPath("companions", "skins/" + skinKey);
        }
    }

    static {
        initCacheDir();
    }

    private static synchronized void initCacheDir() {
        try {
            Minecraft mc = null;
            try {
                mc = Minecraft.getInstance();
            } catch (Throwable ignored) {
            }

            if (mc != null && mc.gameDirectory != null) {
                cacheDir = mc.gameDirectory.toPath().resolve("companion_skins_cache");
            } else {
                cacheDir = Paths.get("companion_skins_cache");
            }

            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
        } catch (Exception e) {
            cacheDir = Paths.get("companion_skins_cache");
        }
    }

    public static Path getCacheDir() {
        if (cacheDir == null) {
            initCacheDir();
        }
        return cacheDir;
    }

    public static boolean isValidSkinName(String name) {
        return name != null && VALID_NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static LoadedSkin getLoadedSkin(String skinName) {
        if (skinName == null || skinName.trim().isEmpty()) {
            return null;
        }
        return MEMORY_CACHE.get(skinName.trim().toLowerCase(Locale.ROOT));
    }

    public static void putLoadedSkin(String skinName, LoadedSkin loadedSkin) {
        if (skinName != null && loadedSkin != null) {
            MEMORY_CACHE.put(skinName.trim().toLowerCase(Locale.ROOT), loadedSkin);
        }
    }

    /**
     * Busca a skin de forma totalmente assincrona sem travar a thread de renderizacao ou do servidor.
     */
    public static CompletableFuture<Optional<LoadedSkin>> getOrFetchSkin(String skinName) {
        if (!isValidSkinName(skinName)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final String cleanName = skinName.trim().toLowerCase(Locale.ROOT);
        LoadedSkin existing = MEMORY_CACHE.get(cleanName);
        if (existing != null) {
            return CompletableFuture.completedFuture(Optional.of(existing));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Path dir = getCacheDir();
                Path pngFile = dir.resolve((cleanName.equals("rimuru") ? "rimuru_demonlord_v1" : cleanName) + ".png");
                Path jsonFile = dir.resolve(cleanName + ".json");

                boolean isSlim = false;

                if (!Files.exists(pngFile)) {
                    // 1. Tenta baixar via Ashcon API
                    boolean downloaded = cleanName.equals("rimuru")
                            ? downloadRimuru(pngFile)
                            : downloadFromAshcon(cleanName, pngFile, jsonFile);
                    if (downloaded && Files.exists(jsonFile)) {
                        try {
                            JsonObject meta = JsonParser.parseString(Files.readString(jsonFile)).getAsJsonObject();
                            if (meta.has("slim")) {
                                isSlim = meta.get("slim").getAsBoolean();
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    // 2. Fallback Minotar se Ashcon falhar
                    if (!Files.exists(pngFile)) {
                        if (cleanName.equals("rimuru")) return Optional.empty();
                        boolean minotarSuccess = downloadFromMinotar(cleanName, pngFile);
                        if (!minotarSuccess) {
                            return Optional.empty();
                        }
                    }
                } else {
                    // Carrega metadado salvo em disco
                    if (Files.exists(jsonFile)) {
                        try {
                            JsonObject meta = JsonParser.parseString(Files.readString(jsonFile)).getAsJsonObject();
                            if (meta.has("slim")) {
                                isSlim = meta.get("slim").getAsBoolean();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (!Files.exists(pngFile)) {
                    return Optional.empty();
                }

                byte[] bytes = Files.readAllBytes(pngFile);
                final boolean finalIsSlim = isSlim;
                final ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath("companions", "skins/" + cleanName);

                // No ambiente de cliente Minecraft ativo, registra no TextureManager
                Minecraft mc = null;
                try {
                    mc = Minecraft.getInstance();
                } catch (Throwable ignored) {
                }

                if (mc != null && mc.getTextureManager() != null) {
                    final Minecraft client = mc;
                    CompletableFuture<Optional<LoadedSkin>> renderFuture = new CompletableFuture<>();
                    client.execute(() -> {
                        try {
                            com.mojang.blaze3d.platform.NativeImage nativeImage = com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(bytes));
                            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                            client.getTextureManager().register(resLoc, dynamicTexture);

                            LoadedSkin loaded = new LoadedSkin(cleanName, finalIsSlim);
                            MEMORY_CACHE.put(cleanName, loaded);
                            renderFuture.complete(Optional.of(loaded));
                        } catch (Exception e) {
                            renderFuture.complete(Optional.empty());
                        }
                    });
                    return renderFuture.join();
                } else {
                    LoadedSkin loaded = new LoadedSkin(cleanName, finalIsSlim);
                    MEMORY_CACHE.put(cleanName, loaded);
                    return Optional.of(loaded);
                }

            } catch (Exception e) {
                return Optional.empty();
            }
        }, IO_EXECUTOR);
    }

    private static boolean downloadRimuru(Path destination) {
        try (InputStream bundled = SkinCacheManager.class.getResourceAsStream("/assets/companions/textures/entity/rimuru_demonlord_v1.png")) {
            if (bundled != null) {
                Files.copy(bundled, destination, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (Exception ignored) {
        }

        HttpURLConnection connection = null;
        try {
            String url = com.thyagotoledo.companions.core.skin.SkinPresetCatalog.getPreset("rimuru").getTextureUrl();
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) MineFriends/1.0");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            if (connection.getResponseCode() != 200) return false;
            byte[] png;
            try (InputStream input = connection.getInputStream()) { png = input.readNBytes(65537); }
            if (png.length > 65536) return false;
            var image = javax.imageio.ImageIO.read(new ByteArrayInputStream(png));
            if (image == null || image.getWidth() != 64 || image.getHeight() != 64) return false;
            Files.write(destination, png);
            return true;
        } catch (Exception failure) { return false; }
        finally { if (connection != null) connection.disconnect(); }
    }

    private static boolean downloadFromAshcon(String username, Path targetPng, Path targetJson) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "MineFriends-CompanionsMod");

            if (conn.getResponseCode() == 200) {
                JsonObject root;
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }

                if (root.has("textures") && root.getAsJsonObject("textures").has("skin")) {
                    JsonObject skinObj = root.getAsJsonObject("textures").getAsJsonObject("skin");
                    String skinUrl = skinObj.get("url").getAsString();
                    boolean slim = skinObj.has("slim") && skinObj.get("slim").getAsBoolean();

                    // Baixa a imagem PNG direta do CDN
                    HttpURLConnection imgConn = (HttpURLConnection) new URL(skinUrl).openConnection();
                    imgConn.setConnectTimeout(4000);
                    imgConn.setReadTimeout(4000);
                    if (imgConn.getResponseCode() == 200) {
                        try (InputStream in = imgConn.getInputStream()) {
                            Files.copy(in, targetPng, StandardCopyOption.REPLACE_EXISTING);
                        }

                        JsonObject meta = new JsonObject();
                        meta.addProperty("slim", slim);
                        meta.addProperty("fetchedAt", System.currentTimeMillis());
                        Files.writeString(targetJson, meta.toString(), StandardCharsets.UTF_8);
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    private static boolean downloadFromMinotar(String username, Path targetPng) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://minotar.net/skin/" + username);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "MineFriends-CompanionsMod");

            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, targetPng, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    public static void clearMemoryCache() {
        MEMORY_CACHE.clear();
    }
}
