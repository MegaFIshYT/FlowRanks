package gg.flowpvp.client.hud;

import gg.flowpvp.client.FlowPvPClient;
import gg.flowpvp.client.match.MatchTracker;
import gg.flowpvp.client.model.RankedPlayerData;
import gg.flowpvp.client.model.RankedTier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class FlowHudOverlay {
    private static final Identifier ICON_FONT = FlowPvPClient.id("icons");

    private FlowHudOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        HudLayout layout = FlowPvPClient.HUD_LAYOUT;
        boolean inMatch = MatchTracker.isInMatch();
        Font font = mc.font;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        for (HudElement element : HudElement.values()) {
            HudLayout.ElementState state = layout.get(element);
            if (!state.visible) continue;
            if (element.matchOnly && !inMatch) continue;

            Component text = buildText(element, mc, inMatch);
            if (text == null) continue;

            int w = font.width(text);
            int x = Math.max(0, Math.min(state.x, screenW - w - 2));
            int y = Math.max(0, Math.min(state.y, screenH - 10));
            graphics.fill(x - 2, y - 2, x + w + 2, y + 10, 0x88000000);
            graphics.drawString(font, text, x, y, 0xFFFFFFFF, false);
        }
    }

    private static Component buildText(HudElement element, Minecraft mc, boolean inMatch) {
        switch (element) {
            case SELECTED_GAMEMODE: {
                var ladder = FlowPvPClient.selectedLadder();
                MutableComponent icon = Component.literal(ladder.iconGlyph)
                        .withStyle(s -> s.withFont(new FontDescription.Resource(ICON_FONT)).withColor(ladder.color & 0xFFFFFF));
                return Component.empty()
                        .append(icon)
                        .append(Component.literal(" " + ladder.displayName).withStyle(s -> s.withColor(ladder.color & 0xFFFFFF)));
            }
            case YOUR_ELO: {
                if (mc.player == null) return null;
                String name = mc.player.getGameProfile().name();
                RankedPlayerData data = completedNow(FlowPvPClient.CACHE.rankedByName(name));
                if (data == null) return null;
                var ladder = FlowPvPClient.selectedLadder();
                int elo = data.ratingFor(ladder);
                int color = RankedTier.fromElo(elo).color & 0xFFFFFF;
                MutableComponent icon = Component.literal(ladder.iconGlyph)
                        .withStyle(s -> s.withFont(new FontDescription.Resource(ICON_FONT)).withColor(0xFFFFFF));
                return Component.empty()
                        .append(Component.literal("You  ").withStyle(s -> s.withColor(0xFFFFFF)))
                        .append(icon)
                        .append(Component.literal(" " + elo).withStyle(s -> s.withColor(color)));
            }
            case OPPONENT_ELO: {
                String opName = MatchTracker.getOpponentName();
                if (opName == null) return null;
                RankedPlayerData data = completedNow(FlowPvPClient.CACHE.rankedByName(opName));
                if (data == null) return null;
                var ladder = FlowPvPClient.selectedLadder();
                int elo = data.ratingFor(ladder);
                int color = RankedTier.fromElo(elo).color & 0xFFFFFF;
                MutableComponent icon = Component.literal(ladder.iconGlyph)
                        .withStyle(s -> s.withFont(new FontDescription.Resource(ICON_FONT)).withColor(0xFFFFFF));
                return Component.empty()
                        .append(Component.literal(opName + "  ").withStyle(s -> s.withColor(0xFFFFFF)))
                        .append(icon)
                        .append(Component.literal(" " + elo).withStyle(s -> s.withColor(color)));
            }
        }
        return null;
    }

    private static RankedPlayerData completedNow(CompletableFuture<RankedPlayerData> future) {
        if (!future.isDone()) return null;
        try {
            return future.getNow(null);
        } catch (CompletionException ignored) {
            return null;
        }
    }
}
