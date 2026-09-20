package com.thyagotoledo.companions.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot imutavel do estado atual do companheiro no servidor.
 * Utilizado para comunicacao de rede C2S / S2C e sincronizacao da interface grafica.
 */
public final class CompanionSnapshot {

    private final UUID companionUuid;
    private final UUID ownerUuid;
    private final String name;
    private final CompanionMode mode;
    private final float currentHealth;
    private final float maxHealth;
    private final boolean spawned;
    private final boolean fakePlayer;
    private final String skinName;
    private final boolean hasDesignatedChest;
    private final int designatedChestX;
    private final int designatedChestY;
    private final int designatedChestZ;
    private final boolean tensuraActive;
    private final String tensuraRace;
    private final String tensuraRank;
    private final long tensuraEp;
    private final String lastMessage;
    private final long revision;

    public CompanionSnapshot(
            UUID companionUuid,
            UUID ownerUuid,
            String name,
            CompanionMode mode,
            float currentHealth,
            float maxHealth,
            boolean spawned,
            boolean fakePlayer,
            String skinName,
            boolean hasDesignatedChest,
            int designatedChestX,
            int designatedChestY,
            int designatedChestZ,
            boolean tensuraActive,
            String tensuraRace,
            String tensuraRank,
            long tensuraEp,
            String lastMessage
    ) {
        this(companionUuid, ownerUuid, name, mode, currentHealth, maxHealth, spawned, fakePlayer,
                skinName, hasDesignatedChest, designatedChestX, designatedChestY, designatedChestZ,
                tensuraActive, tensuraRace, tensuraRank, tensuraEp, lastMessage, 0L);
    }

    public CompanionSnapshot(
            UUID companionUuid,
            UUID ownerUuid,
            String name,
            CompanionMode mode,
            float currentHealth,
            float maxHealth,
            boolean spawned,
            boolean fakePlayer,
            String skinName,
            boolean hasDesignatedChest,
            int designatedChestX,
            int designatedChestY,
            int designatedChestZ,
            boolean tensuraActive,
            String tensuraRace,
            String tensuraRank,
            long tensuraEp,
            String lastMessage,
            long revision
    ) {
        this.companionUuid = companionUuid;
        this.ownerUuid = ownerUuid;
        this.name = name != null ? name : "Companheiro";
        this.mode = mode != null ? mode : CompanionMode.FOLLOW;
        this.currentHealth = Math.max(0.0f, currentHealth);
        this.maxHealth = Math.max(1.0f, maxHealth);
        this.spawned = spawned;
        this.fakePlayer = fakePlayer;
        this.skinName = skinName != null ? skinName : "";
        this.hasDesignatedChest = hasDesignatedChest;
        this.designatedChestX = designatedChestX;
        this.designatedChestY = designatedChestY;
        this.designatedChestZ = designatedChestZ;
        this.tensuraActive = tensuraActive;
        this.tensuraRace = tensuraRace != null ? tensuraRace : "Humano";
        this.tensuraRank = tensuraRank != null ? tensuraRank : "F";
        this.tensuraEp = Math.max(0L, tensuraEp);
        this.lastMessage = lastMessage != null ? lastMessage : "";
        this.revision = Math.max(0L, revision);
    }

    public static CompanionSnapshot empty(UUID ownerUuid) {
        return empty(ownerUuid, 0L);
    }

    public static CompanionSnapshot empty(UUID ownerUuid, long revision) {
        return new CompanionSnapshot(
                null,
                ownerUuid,
                "Companheiro",
                CompanionMode.IDLE,
                0.0f,
                20.0f,
                false,
                false,
                "",
                false,
                0,
                0,
                0,
                false,
                "N/A",
                "N/A",
                0L,
                "Nenhum companheiro ativo no momento.",
                revision
        );
    }

    public UUID getCompanionUuid() {
        return companionUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getName() {
        return name;
    }

    public CompanionMode getMode() {
        return mode;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public boolean isFakePlayer() {
        return fakePlayer;
    }

    public String getSkinName() {
        return skinName;
    }

    public boolean hasDesignatedChest() {
        return hasDesignatedChest;
    }

    public int getDesignatedChestX() {
        return designatedChestX;
    }

    public int getDesignatedChestY() {
        return designatedChestY;
    }

    public int getDesignatedChestZ() {
        return designatedChestZ;
    }

    public boolean isTensuraActive() {
        return tensuraActive;
    }

    public String getTensuraRace() {
        return tensuraRace;
    }

    public String getTensuraRank() {
        return tensuraRank;
    }

    public long getTensuraEp() {
        return tensuraEp;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getRevision() {
        return revision;
    }

    public String getHealthDisplay() {
        return String.format(java.util.Locale.ROOT, "%.0f / %.0f", currentHealth, maxHealth);
    }

    public String getChestDisplay() {
        if (!hasDesignatedChest) {
            return "Nenhum bau marcado";
        }
        return String.format(java.util.Locale.ROOT, "[%d, %d, %d]", designatedChestX, designatedChestY, designatedChestZ);
    }

    public String getPresenceDisplay() {
        if (!spawned) {
            return "Ausente (Nao invocado)";
        }
        return fakePlayer ? "Jogador Oficial (Lan/Server)" : "Entidade Companheira";
    }

    public String getModeDisplay() {
        return mode != null ? mode.name() : "DESCONHECIDO";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanionSnapshot that = (CompanionSnapshot) o;
        return Float.compare(that.currentHealth, currentHealth) == 0 &&
                Float.compare(that.maxHealth, maxHealth) == 0 &&
                spawned == that.spawned &&
                fakePlayer == that.fakePlayer &&
                hasDesignatedChest == that.hasDesignatedChest &&
                designatedChestX == that.designatedChestX &&
                designatedChestY == that.designatedChestY &&
                designatedChestZ == that.designatedChestZ &&
                tensuraActive == that.tensuraActive &&
                tensuraEp == that.tensuraEp &&
                revision == that.revision &&
                Objects.equals(companionUuid, that.companionUuid) &&
                Objects.equals(ownerUuid, that.ownerUuid) &&
                Objects.equals(name, that.name) &&
                mode == that.mode &&
                Objects.equals(skinName, that.skinName) &&
                Objects.equals(tensuraRace, that.tensuraRace) &&
                Objects.equals(tensuraRank, that.tensuraRank) &&
                Objects.equals(lastMessage, that.lastMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companionUuid, ownerUuid, name, mode, currentHealth, maxHealth,
                spawned, fakePlayer, skinName, hasDesignatedChest, designatedChestX,
                designatedChestY, designatedChestZ, tensuraActive, tensuraRace, tensuraRank,
                tensuraEp, lastMessage, revision);
    }

    @Override
    public String toString() {
        return "CompanionSnapshot{" +
                "companionUuid=" + companionUuid +
                ", ownerUuid=" + ownerUuid +
                ", name='" + name + '\'' +
                ", mode=" + mode +
                ", health=" + currentHealth + "/" + maxHealth +
                ", spawned=" + spawned +
                ", fakePlayer=" + fakePlayer +
                ", skinName='" + skinName + '\'' +
                ", designatedChest=" + (hasDesignatedChest ? "[" + designatedChestX + "," + designatedChestY + "," + designatedChestZ + "]" : "none") +
                ", tensuraActive=" + tensuraActive +
                ", tensuraEp=" + tensuraEp +
                '}';
    }
}
