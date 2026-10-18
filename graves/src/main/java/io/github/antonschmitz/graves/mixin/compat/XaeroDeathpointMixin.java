package io.github.antonschmitz.graves.mixin.compat;

import io.github.antonschmitz.graves.client.XaeroWaypoints;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.waypoint.DeathpointHandler;

/** Xaero adds its deathpoint on respawn. Skip it when this death already got a grave waypoint. */
@Mixin(value = DeathpointHandler.class, remap = false)
public abstract class XaeroDeathpointMixin {
    @Inject(method = "createDeathpoint(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    private void simplegraves$skipDeathpoint(Player player, CallbackInfo ci) {
        if (XaeroWaypoints.consumeGraveDeath()) {
            ci.cancel();
        }
    }
}
