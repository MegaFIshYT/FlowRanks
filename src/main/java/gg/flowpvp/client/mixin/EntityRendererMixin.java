package gg.flowpvp.client.mixin;

import gg.flowpvp.client.nametag.FlowPvpNameTagFormatter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void flowpvp$decoratePlayerNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof AbstractClientPlayer player) {
            cir.setReturnValue(FlowPvpNameTagFormatter.decorate(player, cir.getReturnValue()));
        }
    }
}
