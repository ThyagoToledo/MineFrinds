package com.thyagotoledo.companions.neoforge.tensura;

/**
 * Racas suportadas no ecossistema Tensura Reincarnated e modpack Tensura Neo Otherworld.
 */
public enum TensuraRace {
    HUMAN("Humano", "Human", 500, 100, 50, false),
    SLIME("Slime", "Slime", 300, 150, 20, false),
    GOBLIN("Goblin", "Goblin", 400, 80, 40, true),
    HOBGOBLIN("Hobgoblin", "Hobgoblin", 2500, 500, 300, false),
    OGRE("Ogro", "Ogre", 4000, 800, 600, true),
    KIJIN("Kijin", "Kijin", 15000, 3500, 2500, false),
    DRAGONEWT("Dragonewt", "Dragonewt", 8000, 2000, 1500, false),
    DWARF("Anao", "Dwarf", 1200, 300, 200, false),
    ELF("Elfo", "Elf", 2000, 600, 100, false);

    private final String displayNamePt;
    private final String displayNameEn;
    private final long baseEp;
    private final double baseMagicule;
    private final double baseAura;
    private final boolean canEvolveByNaming;

    TensuraRace(String displayNamePt, String displayNameEn, long baseEp, double baseMagicule, double baseAura, boolean canEvolveByNaming) {
        this.displayNamePt = displayNamePt;
        this.displayNameEn = displayNameEn;
        this.baseEp = baseEp;
        this.baseMagicule = baseMagicule;
        this.baseAura = baseAura;
        this.canEvolveByNaming = canEvolveByNaming;
    }

    public String getDisplayName(String locale) {
        return (locale != null && locale.toLowerCase().startsWith("pt")) ? displayNamePt : displayNameEn;
    }

    public long getBaseEp() {
        return baseEp;
    }

    public double getBaseMagicule() {
        return baseMagicule;
    }

    public double getBaseAura() {
        return baseAura;
    }

    public boolean canEvolveByNaming() {
        return canEvolveByNaming;
    }

    public TensuraRace getEvolution() {
        switch (this) {
            case GOBLIN:
                return HOBGOBLIN;
            case OGRE:
                return KIJIN;
            default:
                return this;
        }
    }
}
