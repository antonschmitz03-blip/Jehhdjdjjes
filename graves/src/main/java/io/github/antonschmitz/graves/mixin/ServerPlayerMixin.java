package io.github.antonschmitz.graves.mixin;

import io.github.antonschmitz.graves.DeathCapture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Starts collecting drops before anything is dropped; Fabric's AFTER_DEATH event (late phase) places the grave. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void simplegraves$beginCapture(DamageSource source, CallbackInfo ci) {
        DeathCapture.begin((ServerPlayer) (Object) this);
    }
}
