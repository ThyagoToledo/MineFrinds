package com.thyagotoledo.companions.neoforge.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistencia oficial e nativa de dados dos companheiros no Minecraft 1.21.1 / NeoForge.
 * Armazena no NBT do mundo as configuracoes de baus designados e registros de estado dos companheiros.
 */
public class CompanionSavedData extends SavedData {

    public static final String DATA_NAME = "companions_data";
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final SavedData.Factory<CompanionSavedData> FACTORY = new SavedData.Factory<>(
            CompanionSavedData::new,
            CompanionSavedData::load,
            null
    );

    public static class CompanionStateRecord {
        private final String name;
        private final String skin;
        private final String mode;
        private final String personality;
        private final long tensuraEp;

        public CompanionStateRecord(String name, String skin, String mode, String personality, long tensuraEp) {
            this.name = name != null ? name : "Companheiro";
            this.skin = skin != null ? skin : "";
            this.mode = mode != null ? mode : "FOLLOW";
            this.personality = personality != null ? personality : "BALANCED";
            this.tensuraEp = Math.max(0L, tensuraEp);
        }

        public String getName() {
            return name;
        }

        public String getSkin() {
            return skin;
        }

        public String getMode() {
            return mode;
        }

        public String getPersonality() {
            return personality;
        }

        public long getTensuraEp() {
            return tensuraEp;
        }
    }

    private final Map<UUID, BlockPos> designatedChests = new ConcurrentHashMap<>();
    private final Map<UUID, CompanionStateRecord> companionRecords = new ConcurrentHashMap<>();

    public CompanionSavedData() {
    }

    public static CompanionSavedData get(ServerLevel level) {
        if (level == null) return null;
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static CompanionSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CompanionSavedData data = new CompanionSavedData();
        if (tag == null) return data;

        int schemaVersion = tag.getInt("schema_version");
        if (schemaVersion < 1) {
            schemaVersion = 1;
        }

        // 1. Carrega baus designados
        if (tag.contains("Chests", 10)) { // 10 = CompoundTag
            CompoundTag chestsTag = tag.getCompound("Chests");
            for (String key : chestsTag.getAllKeys()) {
                try {
                    UUID ownerUuid = UUID.fromString(key);
                    CompoundTag cTag = chestsTag.getCompound(key);
                    int x = cTag.getInt("x");
                    int y = cTag.getInt("y");
                    int z = cTag.getInt("z");
                    data.designatedChests.put(ownerUuid, new BlockPos(x, y, z));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // 2. Carrega estado dos companheiros
        if (tag.contains("Companions", 10)) {
            CompoundTag companionsTag = tag.getCompound("Companions");
            for (String key : companionsTag.getAllKeys()) {
                try {
                    UUID ownerUuid = UUID.fromString(key);
                    CompoundTag compTag = companionsTag.getCompound(key);
                    String name = compTag.getString("name");
                    String skin = compTag.getString("skin");
                    String mode = compTag.getString("mode");
                    String personality = compTag.getString("personality");
                    long tensuraEp = compTag.getLong("tensuraEp");

                    data.companionRecords.put(ownerUuid, new CompanionStateRecord(name, skin, mode, personality, tensuraEp));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag == null) tag = new CompoundTag();

        tag.putInt("schema_version", CURRENT_SCHEMA_VERSION);

        // 1. Salva baus designados
        CompoundTag chestsTag = new CompoundTag();
        for (Map.Entry<UUID, BlockPos> entry : designatedChests.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                CompoundTag cTag = new CompoundTag();
                cTag.putInt("x", entry.getValue().getX());
                cTag.putInt("y", entry.getValue().getY());
                cTag.putInt("z", entry.getValue().getZ());
                chestsTag.put(entry.getKey().toString(), cTag);
            }
        }
        tag.put("Chests", chestsTag);

        // 2. Salva registros dos companheiros
        CompoundTag companionsTag = new CompoundTag();
        for (Map.Entry<UUID, CompanionStateRecord> entry : companionRecords.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                CompoundTag compTag = new CompoundTag();
                compTag.putString("name", entry.getValue().getName());
                compTag.putString("skin", entry.getValue().getSkin());
                compTag.putString("mode", entry.getValue().getMode());
                compTag.putString("personality", entry.getValue().getPersonality());
                compTag.putLong("tensuraEp", entry.getValue().getTensuraEp());
                companionsTag.put(entry.getKey().toString(), compTag);
            }
        }
        tag.put("Companions", companionsTag);

        return tag;
    }

    public void setDesignatedChest(UUID ownerUuid, BlockPos pos) {
        if (ownerUuid == null) return;
        if (pos == null) {
            designatedChests.remove(ownerUuid);
        } else {
            designatedChests.put(ownerUuid, pos.immutable());
        }
        setDirty();
    }

    public BlockPos getDesignatedChest(UUID ownerUuid) {
        if (ownerUuid == null) return null;
        return designatedChests.get(ownerUuid);
    }

    public void saveCompanionRecord(UUID ownerUuid, String name, String skin, String mode, String personality, long tensuraEp) {
        if (ownerUuid == null) return;
        companionRecords.put(ownerUuid, new CompanionStateRecord(name, skin, mode, personality, tensuraEp));
        setDirty();
    }

    public CompanionStateRecord getCompanionRecord(UUID ownerUuid) {
        if (ownerUuid == null) return null;
        return companionRecords.get(ownerUuid);
    }

    public Map<UUID, BlockPos> getAllDesignatedChests() {
        return Collections.unmodifiableMap(designatedChests);
    }

    public Map<UUID, CompanionStateRecord> getAllCompanionRecords() {
        return Collections.unmodifiableMap(companionRecords);
    }
}
