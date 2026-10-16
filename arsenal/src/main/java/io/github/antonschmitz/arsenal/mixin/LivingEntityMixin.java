package io.github.antonschmitz.arsenal.mixin;

import io.github.antonschmitz.arsenal.GaleBowItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Landing with a Gale Bow in hand hurts half as much. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    // The only float argument of causeFallDamage is the damage multiplier. require = 0 as in PlayerMixin: a renamed
    // method quietly loses the rule instead of stopping the game.
    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, require = 0)
    private float vanillaarsenal$softLanding(float damageMultiplier) {
        if ((Object) this instanceof Player player
                && (player.getMainHandItem().getItem() instanceof GaleBowItem
                        || player.getOffhandItem().getItem() instanceof GaleBowItem)) {
            return damageMultiplier * 0.5F;
        }
        return damageMultiplier;
    }
}
