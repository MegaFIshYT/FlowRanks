package gg.flowpvp.client.nametag;

import gg.flowpvp.client.FlowPvPClient;
import gg.flowpvp.client.model.Ladder;
import gg.flowpvp.client.model.RankedPlayerData;
import gg.flowpvp.client.model.RankedTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class FlowPvpNameTagFormatter {
    private static final Identifier ICON_FONT = FlowPvPClient.id("icons");

    private FlowPvpNameTagFormatter() {
    }

    public static Component decorate(AbstractClientPlayer player, Component original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (original == null || !FlowPvPClient.overlayEnabled() || minecraft.player == null || player == minecraft.player) {
            return original;
        }

        Ladder ladder = FlowPvPClient.selectedLadder();
        CompletableFuture<RankedPlayerData> future = FlowPvPClient.CACHE.rankedByName(player.getName().getString());
        RankedPlayerData profile = completedValue(future);
        if (profile == null) {
            return original;
        }

        int ratingValue = profile.ratingFor(ladder);
        int color = RankedTier.fromElo(ratingValue).color & 0xFFFFFF;
        MutableComponent icon = Component.literal(ladder.iconGlyph).withStyle(style -> style.withFont(new FontDescription.Resource(ICON_FONT)).withColor(0xFFFFFF));
        MutableComponent rating = Component.literal(" " + ratingValue + " ").withStyle(style -> style.withColor(color));

        return Component.empty()
                .append(icon)
                .append(rating)
                .append(player.getName().copy());
    }

    private static RankedPlayerData completedValue(CompletableFuture<RankedPlayerData> future) {
        if (!future.isDone()) {
            return null;
        }

        try {
            return future.getNow(null);
        } catch (CompletionException ignored) {
            return null;
        }
    }
}
