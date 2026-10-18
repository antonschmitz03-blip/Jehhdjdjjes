package io.github.antonschmitz.graves.mixin;

import io.github.antonschmitz.graves.DeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Item entities and XP orbs spawned during a player's death go into the grave instead of the world. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void simplegraves$captureDeathDrop(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DeathCapture.capture((ServerLevel) (Object) this, entity)) {
            cir.setReturnValue(false);
        }
    }
}
