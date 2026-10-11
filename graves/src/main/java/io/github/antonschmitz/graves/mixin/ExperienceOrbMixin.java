package io.github.antonschmitz.graves.mixin;

import io.github.antonschmitz.graves.DeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Catches death XP before vanilla can merge it into orbs that already exist nearby. */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "awardWithDirection", at = @At("HEAD"), cancellable = true)
    private static void simplegraves$captureDeathXp(ServerLevel level, Vec3 pos, Vec3 direction, int amount, CallbackInfo ci) {
        if (DeathCapture.captureXp(level, amount)) {
            ci.cancel();
        }
    }
}
