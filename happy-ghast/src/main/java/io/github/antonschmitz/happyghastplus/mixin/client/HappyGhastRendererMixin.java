package io.github.antonschmitz.happyghastplus.mixin.client;

import io.github.antonschmitz.happyghastplus.GhastChest;
import io.github.antonschmitz.happyghastplus.GhastFeatures;
import io.github.antonschmitz.happyghastplus.client.GhastChestLayer;
import io.github.antonschmitz.happyghastplus.client.GhastEngineLayer;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the synced chest kind and speed upgrade into the render state for {@link GhastChestLayer}. */
@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/happyghast/HappyGhast;Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;F)V",
            at = @At("TAIL")
    )
    private void happyghastplus$extractChest(HappyGhast ghast, HappyGhastRenderState state, float partialTick, CallbackInfo ci) {
        ((FabricRenderState) (Object) state).setData(GhastChestLayer.CHEST_KIND, ghast.getAttachedOrElse(GhastChest.KIND, GhastChest.NONE));
        ((FabricRenderState) (Object) state).setData(GhastEngineLayer.ENGINES, ghast.getAttachedOrElse(GhastFeatures.SPEED_UPGRADED, false));
    }
}
