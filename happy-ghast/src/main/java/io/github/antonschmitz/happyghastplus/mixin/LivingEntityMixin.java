package io.github.antonschmitz.happyghastplus.mixin;

import io.github.antonschmitz.happyghastplus.RopeLadders;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets players climb a happy ghast's rope ladder, which is drawn by the ghast and has no blocks. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void happyghastplus$climbRope(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && !player.isSpectator() && RopeLadders.isOnRope(player)) {
            cir.setReturnValue(true);
        }
    }
}
