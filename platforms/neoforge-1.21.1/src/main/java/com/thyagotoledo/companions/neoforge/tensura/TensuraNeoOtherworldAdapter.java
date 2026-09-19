package com.thyagotoledo.companions.neoforge.tensura;

import java.util.UUID;

/**
 * Adaptador de compatibilidade para o modpack Tensura Neo Otherworld (NeoForge 1.21.1).
 * Gerencia a integracao com o ecossistema Tensura Reincarnated, protecao de subordinados FTB
 * e regras de area de magiculas.
 */
public class TensuraNeoOtherworldAdapter {

    private final boolean tensuraLoaded;
    private final boolean tensuraTnoLoaded;
    private final boolean ftbTeamsLoaded;

    public TensuraNeoOtherworldAdapter() {
        this(true, true, true);
    }

    public TensuraNeoOtherworldAdapter(boolean tensuraLoaded, boolean tensuraTnoLoaded, boolean ftbTeamsLoaded) {
        this.tensuraLoaded = tensuraLoaded;
        this.tensuraTnoLoaded = tensuraTnoLoaded;
        this.ftbTeamsLoaded = ftbTeamsLoaded;
    }

    public boolean isTensuraLoaded() {
        return tensuraLoaded;
    }

    public boolean isTensuraTnoLoaded() {
        return tensuraTnoLoaded;
    }

    public boolean isFtbTeamsLoaded() {
        return ftbTeamsLoaded;
    }

    /**
     * Verifica se a entidade esta protegida como subordinada/aliada pelo Tensura FTB.
     * Conforme ftb_config.toml: protectSubordinates = true e ftbAllyTensura = true.
     */
    public boolean isSubordinateProtected(UUID companionUuid, UUID ownerUuid) {
        if (companionUuid == null || ownerUuid == null) {
            return false;
        }
        // No modpack Tensura Neo Otherworld, companheiros vinculados ao dono sao considerados subordinados protegidos
        return true;
    }

    /**
     * Valida se o dano recebido deve ser bloqueado por protecao de subordinado em area de claim.
     */
    public boolean shouldCancelSpiritualDamageInClaim(boolean isClaimedNonPvp) {
        return isClaimedNonPvp;
    }

    /**
     * Valida imunidade a veneno de magiculas de area em chunks densos (ex: Floresta de Jura).
     */
    public boolean shouldProtectFromMagiculePoison(TensuraCompanionStats stats, double chunkMagiculeDensity) {
        if (stats == null) {
            return false;
        }
        return stats.isMagiculePoisonImmune();
    }

    /**
     * Conduz a cerimonia de nomeacao concedida pelo mestre.
     */
    public boolean nameCompanion(TensuraCompanionStats stats, String name) {
        if (stats == null || name == null) {
            return false;
        }
        return stats.bestowName(name);
    }

    /**
     * Formata o relatorio estruturado de status Tensura.
     */
    public String formatTensuraStatus(TensuraCompanionStats stats, String locale) {
        if (stats == null) {
            return "Tensura Stats: Indisponivel";
        }
        String raceName = stats.getRace().getDisplayName(locale);
        String rank = stats.getRank();
        long ep = stats.getExistenceValue();
        long mp = Math.round(stats.getMagicule());
        long aura = Math.round(stats.getAura());

        if (locale != null && locale.toLowerCase().startsWith("pt")) {
            return String.format("Relatorio Tensura: Raca %s, Rank %s, EP: %d, Magiculas: %d, Aura: %d.",
                    raceName, rank, ep, mp, aura);
        } else {
            return String.format("Tensura Report: Race %s, Rank %s, EP: %d, Magicules: %d, Aura: %d.",
                    raceName, rank, ep, mp, aura);
        }
    }
}
