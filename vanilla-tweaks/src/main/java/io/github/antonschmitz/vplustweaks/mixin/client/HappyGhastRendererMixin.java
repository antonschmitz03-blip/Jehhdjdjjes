package io.github.antonschmitz.vplustweaks.mixin.client;

import io.github.antonschmitz.vplustweaks.GhastChest;
import io.github.antonschmitz.vplustweaks.client.GhastChestLayer;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the synced chest kind into the render state for {@link GhastChestLayer}. */
@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/happyghast/HappyGhast;Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;F)V",
            at = @At("TAIL")
    )
    private void vplustweaks$extractChest(HappyGhast ghast, HappyGhastRenderState state, float partialTick, CallbackInfo ci) {
        ((FabricRenderState) (Object) state).setData(GhastChestLayer.CHEST_KIND, ghast.getAttachedOrElse(GhastChest.KIND, GhastChest.NONE));
    }
}
