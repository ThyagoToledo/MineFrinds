package com.thyagotoledo.companions.neoforge.tensura;

/**
 * Estatisticas de poder, energia espiritual e atributos no padrao Tensura Neo Otherworld.
 * Segue as regras do modpack (EP, Magiculas, Aura, SHP e formulas de ganho por 10.000 EP).
 */
public class TensuraCompanionStats {

    private TensuraRace race;
    private long existenceValue;
    private double magicule;
    private double maxMagicule;
    private double aura;
    private double maxAura;
    private double spiritualHealth;
    private boolean named;
    private String bestowedName;
    private boolean magiculePoisonImmunity;

    public TensuraCompanionStats() {
        this(TensuraRace.HUMAN);
    }

    public TensuraCompanionStats(TensuraRace race) {
        this.race = race != null ? race : TensuraRace.HUMAN;
        this.existenceValue = this.race.getBaseEp();
        this.maxMagicule = this.race.getBaseMagicule();
        this.magicule = this.maxMagicule;
        this.maxAura = this.race.getBaseAura();
        this.aura = this.maxAura;
        this.spiritualHealth = 100.0 + getBonusSpiritualHealth();
        this.named = false;
        this.bestowedName = null;
        this.magiculePoisonImmunity = true; // Imune a veneno de magiculas de area para protecao do companheiro
    }

    public double getBonusHealth() {
        return (existenceValue / 10000.0) * 1.0;
    }

    public double getBonusAttack() {
        return (existenceValue / 10000.0) * 0.5;
    }

    public double getBonusArmor() {
        return (existenceValue / 10000.0) * 0.5;
    }

    public double getBonusSpiritualHealth() {
        return (existenceValue / 10000.0) * 5.0;
    }

    public String getRank() {
        if (existenceValue < 1000) return "F";
        if (existenceValue < 5000) return "E";
        if (existenceValue < 10000) return "D";
        if (existenceValue < 25000) return "C";
        if (existenceValue <= 50000) return "B";
        if (existenceValue < 100000) return "A";
        if (existenceValue < 400000) return "Special A";
        if (existenceValue < 800000) return "Disaster";
        return "Catastrophe";
    }

    /**
     * Executa a cerimonia de nomeacao no universo Tensura.
     * Concede um salto substancial de EP, desperta o potencial magico e evolui a raca quando aplicavel.
     */
    public boolean bestowName(String name) {
        if (name == null || name.trim().isEmpty() || this.named) {
            return false;
        }
        this.bestowedName = name.trim();
        this.named = true;

        if (this.race.canEvolveByNaming()) {
            this.race = this.race.getEvolution();
        }

        // Salto de EP por nomeacao
        this.existenceValue = Math.max(this.existenceValue * 3, this.race.getBaseEp());
        this.maxMagicule = Math.max(this.maxMagicule * 2.5, this.race.getBaseMagicule());
        this.magicule = this.maxMagicule;
        this.maxAura = Math.max(this.maxAura * 2.5, this.race.getBaseAura());
        this.aura = this.maxAura;
        this.spiritualHealth = 100.0 + getBonusSpiritualHealth();
        return true;
    }

    public TensuraRace getRace() {
        return race;
    }

    public void setRace(TensuraRace race) {
        if (race != null) {
            this.race = race;
        }
    }

    public long getExistenceValue() {
        return existenceValue;
    }

    public void setExistenceValue(long existenceValue) {
        this.existenceValue = Math.max(0, existenceValue);
    }

    public double getMagicule() {
        return magicule;
    }

    public void setMagicule(double magicule) {
        this.magicule = Math.max(0, Math.min(magicule, maxMagicule));
    }

    public double getMaxMagicule() {
        return maxMagicule;
    }

    public double getAura() {
        return aura;
    }

    public void setAura(double aura) {
        this.aura = Math.max(0, Math.min(aura, maxAura));
    }

    public double getMaxAura() {
        return maxAura;
    }

    public double getSpiritualHealth() {
        return spiritualHealth;
    }

    public boolean isNamed() {
        return named;
    }

    public String getBestowedName() {
        return bestowedName;
    }

    public boolean isMagiculePoisonImmune() {
        return magiculePoisonImmunity;
    }

    public void setMagiculePoisonImmune(boolean magiculePoisonImmunity) {
        this.magiculePoisonImmunity = magiculePoisonImmunity;
    }
}
