package io.github.antonschmitz.happyghastplus.mixin.client;

import io.github.antonschmitz.happyghastplus.GhastChest;
import io.github.antonschmitz.happyghastplus.GhastFeatures;
import io.github.antonschmitz.happyghastplus.GhastNests;
import io.github.antonschmitz.happyghastplus.client.GhastChestLayer;
import io.github.antonschmitz.happyghastplus.client.GhastEngineLayer;
import io.github.antonschmitz.happyghastplus.RopeLadders;
import io.github.antonschmitz.happyghastplus.client.GhastEyelidsLayer;
import io.github.antonschmitz.happyghastplus.client.GhastRopeLayer;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the synced chest kind, speed upgrade and sleeping flag into the render state for the ghast's layers. */
@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/happyghast/HappyGhast;Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;F)V",
            at = @At("TAIL")
    )
    private void happyghastplus$extractChest(HappyGhast ghast, HappyGhastRenderState state, float partialTick, CallbackInfo ci) {
        ((FabricRenderState) (Object) state).setData(GhastChestLayer.CHEST_KIND, ghast.getAttachedOrElse(GhastChest.KIND, GhastChest.NONE));
        ((FabricRenderState) (Object) state).setData(GhastEngineLayer.ENGINES, ghast.getAttachedOrElse(GhastFeatures.SPEED_UPGRADED, false));
        ((FabricRenderState) (Object) state).setData(GhastRopeLayer.ROPE, ghast.getAttachedOrElse(RopeLadders.ROPE, 0.0F));
        ((FabricRenderState) (Object) state).setData(GhastEyelidsLayer.SLEEPING, ghast.getAttachedOrElse(GhastNests.SLEEPING, false));
    }
}
