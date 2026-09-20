package com.thyagotoledo.companions.core.ai;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.util.Properties;

/** Small local configuration; no model is downloaded by the mod. */
public final class InferenceSettings {
    private InferenceSettings() { }

    public static InferenceSupervisor load(Path file) {
        Properties settings = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                settings.load(input);
            } catch (java.io.IOException error) {
                System.err.println("Companions: AI configuration unreadable; inference disabled: " + error.getClass().getSimpleName());
                settings.setProperty("enabled", "false");
            }
        }
        boolean enabled = Boolean.parseBoolean(settings.getProperty("enabled",
                System.getProperty("companions.ai.enabled", "false")));
        String endpoint = settings.getProperty("endpoint", System.getProperty("companions.ai.endpoint",
                "http://127.0.0.1:8080/v1/chat/completions"));
        int timeout;
        try { timeout = Integer.parseInt(settings.getProperty("timeout_ms", "10000")); }
        catch (NumberFormatException invalid) { timeout = 10000; }
        return new InferenceSupervisor(enabled, endpoint, Math.max(1000, Math.min(30000, timeout)), 8);
    }
}
