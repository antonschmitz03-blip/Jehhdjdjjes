package io.github.antonschmitz.arsenal.mixin;

import io.github.antonschmitz.arsenal.GaleBowItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Jump is the Gale Bow's air jump, so an elytra must not open on it. Runs on both sides: the client asks before it
 * sends the "start gliding" packet, the server asks before it believes it.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    // require = 0: if a game update renames the method, the elytra rule quietly stops working instead of
    // the game failing to start (GaleMovement also stops a glide the server sees).
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true, require = 0)
    private void vanillaarsenal$noElytraWithGaleBow(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.getMainHandItem().getItem() instanceof GaleBowItem
                || self.getOffhandItem().getItem() instanceof GaleBowItem) {
            cir.setReturnValue(false);
        }
    }
}
