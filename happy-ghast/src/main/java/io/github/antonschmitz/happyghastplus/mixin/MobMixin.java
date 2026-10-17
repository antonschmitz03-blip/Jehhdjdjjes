package io.github.antonschmitz.happyghastplus.mixin;

import io.github.antonschmitz.happyghastplus.GhastNests;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pauses a happy ghast's AI (brain, goals, look and move control) while it rests on a nest. Otherwise its look
 * control turns it towards nearby players every tick, which GhastNests then turns back: on clients that shows as
 * twitching, and the ghast never counted as settled, so it never fell asleep.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void happyghastplus$restOnNest(CallbackInfo ci) {
        if ((Object) this instanceof HappyGhast ghast && GhastNests.isResting(ghast)) {
            ci.cancel();
        }
    }
}
