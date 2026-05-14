package gg.flowpvp.client.mixin;

import gg.flowpvp.client.FlowPvPClient;
import gg.flowpvp.client.model.Ladder;
import gg.flowpvp.client.model.RankedPlayerData;
import gg.flowpvp.client.model.RankedTier;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    private static final Identifier ICON_FONT = FlowPvPClient.id("icons");

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void flowpvp$decorateTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (!FlowPvPClient.overlayEnabled()) return;

        Ladder ladder = FlowPvPClient.selectedLadder();
        CompletableFuture<RankedPlayerData> future = FlowPvPClient.CACHE.rankedByName(playerInfo.getProfile().name());
        RankedPlayerData profile = completedNow(future);
        if (profile == null) return;

        int rating = profile.ratingFor(ladder);
        int color = RankedTier.fromElo(rating).color & 0xFFFFFF;

        MutableComponent icon = Component.literal(ladder.iconGlyph)
                .withStyle(style -> style.withFont(new FontDescription.Resource(ICON_FONT)).withColor(0xFFFFFF));
        MutableComponent ratingText = Component.literal(" " + rating)
                .withStyle(style -> style.withColor(color));

        cir.setReturnValue(Component.empty()
                .append(cir.getReturnValue())
                .append(Component.literal("  ").withStyle(style -> style.withColor(0x55FFFFFF)))
                .append(icon)
                .append(ratingText));
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
