package gg.flowpvp.client.match;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

public final class MatchTracker {
    private MatchTracker() {}

    public static boolean isInMatch() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return false;
        int count = mc.level.players().size();
        return count >= 2 && count <= 4;
    }

    public static String getOpponentName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        for (AbstractClientPlayer player : mc.level.players()) {
            if (!player.getUUID().equals(mc.player.getUUID())) {
                return player.getGameProfile().name();
            }
        }
        return null;
    }
}
