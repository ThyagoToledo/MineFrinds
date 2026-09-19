package com.thyagotoledo.companions.legacy.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Adaptador de mensagens de rede compativel com o padrao SimpleNetworkWrapper / IMessage do Forge 1.12.2.
 */
public final class LegacyCompanionNetwork {

    public static final String CHANNEL_NAME = "companions";

    public static class CommandMessage {
        private UUID companionUuid;
        private String command;
        private String locale;

        public CommandMessage() {
        }

        public CommandMessage(UUID companionUuid, String command, String locale) {
            this.companionUuid = companionUuid;
            this.command = command != null ? command : "";
            this.locale = locale != null ? locale : "pt_br";
        }

        public byte[] toBytes() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeLong(companionUuid != null ? companionUuid.getMostSignificantBits() : 0L);
            dos.writeLong(companionUuid != null ? companionUuid.getLeastSignificantBits() : 0L);
            dos.writeUTF(command);
            dos.writeUTF(locale);
            return baos.toByteArray();
        }

        public void fromBytes(byte[] bytes) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            long most = dis.readLong();
            long least = dis.readLong();
            this.companionUuid = new UUID(most, least);
            this.command = dis.readUTF();
            this.locale = dis.readUTF();
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
    }

    public static class FeedbackMessage {
        private UUID companionUuid;
        private String speech;
        private String locale;
        private boolean success;

        public FeedbackMessage() {
        }

        public FeedbackMessage(UUID companionUuid, String speech, String locale, boolean success) {
            this.companionUuid = companionUuid;
            this.speech = speech != null ? speech : "";
            this.locale = locale != null ? locale : "pt_br";
            this.success = success;
        }

        public byte[] toBytes() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeLong(companionUuid != null ? companionUuid.getMostSignificantBits() : 0L);
            dos.writeLong(companionUuid != null ? companionUuid.getLeastSignificantBits() : 0L);
            dos.writeUTF(speech);
            dos.writeUTF(locale);
            dos.writeBoolean(success);
            return baos.toByteArray();
        }

        public void fromBytes(byte[] bytes) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            long most = dis.readLong();
            long least = dis.readLong();
            this.companionUuid = new UUID(most, least);
            this.speech = dis.readUTF();
            this.locale = dis.readUTF();
            this.success = dis.readBoolean();
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
    }

    private LegacyCompanionNetwork() {
    }
}
