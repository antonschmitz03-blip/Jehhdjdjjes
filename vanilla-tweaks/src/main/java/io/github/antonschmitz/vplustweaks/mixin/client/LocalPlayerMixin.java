package io.github.antonschmitz.vplustweaks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.antonschmitz.vplustweaks.client.ElytraKey;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Jumping in the air no longer opens the elytra while the "Open elytra" key is bound. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @WrapOperation(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z")
    )
    private boolean vplustweaks$jumpDoesNotOpenElytra(LocalPlayer player, Operation<Boolean> original) {
        return !ElytraKey.replacesJump() && original.call(player);
    }
}
