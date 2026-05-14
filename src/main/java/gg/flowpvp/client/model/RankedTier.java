package gg.flowpvp.client.model;

import java.util.Arrays;
import java.util.Comparator;

public enum RankedTier {
    COAL_I("COAL_I", "Coal I", 0, 0xFF4B4B4B),
    COAL_II("COAL_II", "Coal II", 100, 0xFF4B4B4B),
    COAL_III("COAL_III", "Coal III", 200, 0xFF4B4B4B),
    COPPER_I("COPPER_I", "Copper I", 300, 0xFFB87333),
    COPPER_II("COPPER_II", "Copper II", 400, 0xFFB87333),
    COPPER_III("COPPER_III", "Copper III", 500, 0xFFB87333),
    IRON_I("IRON_I", "Iron I", 600, 0xFF9AA0A6),
    IRON_II("IRON_II", "Iron II", 700, 0xFF9AA0A6),
    IRON_III("IRON_III", "Iron III", 800, 0xFF9AA0A6),
    GOLD_I("GOLD_I", "Gold I", 900, 0xFFFFD700),
    GOLD_II("GOLD_II", "Gold II", 1025, 0xFFFFD700),
    GOLD_III("GOLD_III", "Gold III", 1125, 0xFFFFD700),
    EMERALD_I("EMERALD_I", "Emerald I", 1275, 0xFF50C878),
    EMERALD_II("EMERALD_II", "Emerald II", 1400, 0xFF50C878),
    EMERALD_III("EMERALD_III", "Emerald III", 1525, 0xFF50C878),
    DIAMOND_I("DIAMOND_I", "Diamond I", 1650, 0xFF3FE0FF),
    DIAMOND_II("DIAMOND_II", "Diamond II", 1770, 0xFF3FE0FF),
    DIAMOND_III("DIAMOND_III", "Diamond III", 1900, 0xFF3FE0FF),
    NETHERITE("NETHERITE", "Netherite", 2175, 0xFF6B4B36),
    GRANDMASTER("GRANDMASTER", "Grandmaster", Integer.MAX_VALUE, 0xFFFF5DFF);

    public final String id;
    public final String displayName;
    public final int threshold;
    public final int color;

    RankedTier(String id, String displayName, int threshold, int color) {
        this.id = id;
        this.displayName = displayName;
        this.threshold = threshold;
        this.color = color;
    }

    public static RankedTier fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        for (RankedTier tier : values()) {
            if (tier.id.equalsIgnoreCase(id)) {
                return tier;
            }
        }

        return null;
    }

    public static RankedTier fromElo(int elo) {
        return Arrays.stream(values())
                .filter(tier -> tier != GRANDMASTER)
                .filter(tier -> elo >= tier.threshold)
                .max(Comparator.comparingInt(tier -> tier.threshold))
                .orElse(COAL_I);
    }
}
