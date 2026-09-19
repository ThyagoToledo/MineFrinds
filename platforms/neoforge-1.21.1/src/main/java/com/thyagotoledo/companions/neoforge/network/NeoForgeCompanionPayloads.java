package com.thyagotoledo.companions.neoforge.network;

import java.util.UUID;

/**
 * Adaptadores de payload de rede compativeis com a arquitetura CustomPacketPayload de Minecraft 1.21.1 / NeoForge.
 */
public final class NeoForgeCompanionPayloads {

    public static final String CHANNEL_COMMAND = "companions:command_payload";
    public static final String CHANNEL_FEEDBACK = "companions:feedback_payload";

    public static final class CommandPayload {
        private final UUID companionUuid;
        private final String command;
        private final String locale;

        public CommandPayload(UUID companionUuid, String command, String locale) {
            this.companionUuid = companionUuid;
            this.command = command != null ? command : "";
            this.locale = locale != null ? locale : "pt_br";
        }

        public UUID getCompanionUuid() {
            return companionUuid;
        }

        public String getCommand() {
            return command;
        }

        public String getLocale() {
            return locale;
        }

        public String getChannelName() {
            return CHANNEL_COMMAND;
        }
    }

    public static final class FeedbackPayload {
        private final UUID companionUuid;
        private final String speech;
        private final String locale;
        private final boolean success;

        public FeedbackPayload(UUID companionUuid, String speech, String locale, boolean success) {
            this.companionUuid = companionUuid;
            this.speech = speech != null ? speech : "";
            this.locale = locale != null ? locale : "pt_br";
            this.success = success;
        }

        public UUID getCompanionUuid() {
            return companionUuid;
        }

        public String getSpeech() {
            return speech;
        }

        public String getLocale() {
            return locale;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getChannelName() {
            return CHANNEL_FEEDBACK;
        }
    }

    private NeoForgeCompanionPayloads() {
    }
}
