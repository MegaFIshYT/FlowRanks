package gg.flowpvp.client.model;

import java.util.Arrays;
import java.util.List;

public enum Ladder {
    GLOBAL("GLOBAL", "Global", "*", "\uE000", "overall", 0xFFBC73DB),
    SWORD("SWORD", "Sword", "\u2694", "\uE001", "sword", 0xFFE6D15A),
    AXE("AXE", "Axe", "\u25C6", "\uE002", "axe", 0xFFC97B46),
    UHC("UHC", "UHC", "\u2665", "\uE003", "uhc", 0xFFE35656),
    VANILLA("VANILLA", "Vanilla", "\u2726", "\uE004", "vanilla", 0xFF80C7FF),
    MACE("MACE", "Mace", "\u2739", "\uE005", "mace", 0xFFFF31FF),
    DIAMOND_POT("DIAMOND_POT", "Pot", "\u2697", "\uE006", "pot", 0xFF6DE6E8),
    NETHERITE_OP("NETHERITE_OP", "NethOP", "\u26E8", "\uE007", "nethop", 0xFF68DFFF),
    SMP("SMP", "SMP", "\u2302", "\uE008", "smp", 0xFF7CE38B),
    DIAMOND_SMP("DIAMOND_SMP", "DiamondSMP", "\u25C7", "\uE009", "diamond_smp", 0xFF64DDE5);

    private static final List<Ladder> HUD_ROTATION = Arrays.stream(values())
            .filter(ladder -> ladder != GLOBAL)
            .toList();

    public final String apiName;
    public final String displayName;
    public final String icon;
    public final String iconGlyph;
    public final String iconTexture;
    public final int color;

    Ladder(String apiName, String displayName, String icon, String iconGlyph, String iconTexture, int color) {
        this.apiName = apiName;
        this.displayName = displayName;
        this.icon = icon;
        this.iconGlyph = iconGlyph;
        this.iconTexture = iconTexture;
        this.color = color;
    }

    public static Ladder fromApi(String value) {
        if (value == null) {
            return GLOBAL;
        }

        String normalized = value.trim().toUpperCase().replace('-', '_');
        if ("POT".equals(normalized)) {
            normalized = "DIAMOND_POT";
        } else if ("NETHOP".equals(normalized) || "NETHERITEOP".equals(normalized)) {
            normalized = "NETHERITE_OP";
        } else if ("NETHERITE_SMP".equals(normalized)) {
            normalized = "SMP";
        }

        for (Ladder ladder : values()) {
            if (ladder.apiName.equals(normalized) || ladder.name().equals(normalized)) {
                return ladder;
            }
        }

        return GLOBAL;
    }

    public Ladder nextForHud() {
        if (this == GLOBAL) {
            return HUD_ROTATION.get(0);
        }

        int index = HUD_ROTATION.indexOf(this);
        return HUD_ROTATION.get((index + 1) % HUD_ROTATION.size());
    }

    public boolean hasRankedStats() {
        return this != GLOBAL;
    }
}
