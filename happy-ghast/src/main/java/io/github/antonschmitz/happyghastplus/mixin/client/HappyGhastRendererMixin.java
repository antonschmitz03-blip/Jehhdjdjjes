package io.github.antonschmitz.happyghastplus.mixin.client;

import io.github.antonschmitz.happyghastplus.ArmoredHarnesses;
import io.github.antonschmitz.happyghastplus.GhastChest;
import io.github.antonschmitz.happyghastplus.GhastFeatures;
import io.github.antonschmitz.happyghastplus.GhastNests;
import io.github.antonschmitz.happyghastplus.client.GhastChestLayer;
import io.github.antonschmitz.happyghastplus.client.GhastEngineLayer;
import io.github.antonschmitz.happyghastplus.client.GhastEngineParticles;
import io.github.antonschmitz.happyghastplus.RopeLadders;
import io.github.antonschmitz.happyghastplus.client.GhastEyelidsLayer;
import io.github.antonschmitz.happyghastplus.client.GhastSearchlightLayer;
import io.github.antonschmitz.happyghastplus.client.GhastVisorLayer;
import io.github.antonschmitz.happyghastplus.client.GhastRopeLayer;
import io.github.antonschmitz.happyghastplus.client.SearchlightAim;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the synced chest kind, speed upgrade, sleeping flag and armored harness visor into the render state for the ghast's layers. */
@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/happyghast/HappyGhast;Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;F)V",
            at = @At("TAIL")
    )
    private void happyghastplus$extractChest(HappyGhast ghast, HappyGhastRenderState state, float partialTick, CallbackInfo ci) {
        ((FabricRenderState) (Object) state).setData(GhastChestLayer.CHEST_KIND, ghast.getAttachedOrElse(GhastChest.KIND, GhastChest.NONE));
        ((FabricRenderState) (Object) state).setData(GhastEngineLayer.ENGINES, ghast.getAttachedOrElse(GhastFeatures.SPEED_UPGRADED, false));
        ((FabricRenderState) (Object) state).setData(GhastEngineLayer.THRUST, GhastEngineParticles.isThrusting(ghast));
        ((FabricRenderState) (Object) state).setData(GhastRopeLayer.ROPE, ghast.getAttachedOrElse(RopeLadders.ROPE, 0.0F));
        ((FabricRenderState) (Object) state).setData(GhastEyelidsLayer.SLEEPING, ghast.getAttachedOrElse(GhastNests.SLEEPING, false));
        ((FabricRenderState) (Object) state).setData(GhastSearchlightLayer.AIM, SearchlightAim.of(ghast, partialTick));
        ArmoredHarnesses.Tier tier = ArmoredHarnesses.tierOf(ghast.getItemBySlot(EquipmentSlot.BODY).getItem());
        ((FabricRenderState) (Object) state).setData(GhastVisorLayer.VISOR, tier == null ? null : new GhastVisorLayer.Visor(tier.id(), !ghast.isVehicle()));
    }
}
