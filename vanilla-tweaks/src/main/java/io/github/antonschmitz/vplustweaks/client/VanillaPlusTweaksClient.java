package io.github.antonschmitz.vplustweaks.client;

import io.github.antonschmitz.vplustweaks.VanillaPlusTweaks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;

public class VanillaPlusTweaksClient implements ClientModInitializer {
    @Override
    @SuppressWarnings("unchecked")
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlock(VanillaPlusTweaks.ROPE_LADDER, ChunkSectionLayer.CUTOUT);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof HappyGhastRenderer) {
                helper.register(new GhastChestLayer(
                        (RenderLayerParent<HappyGhastRenderState, HappyGhastModel>) (Object) renderer,
                        context.getItemModelResolver()));
                helper.register(new GhastEngineLayer(
                        (RenderLayerParent<HappyGhastRenderState, HappyGhastModel>) (Object) renderer,
                        context.getItemModelResolver()));
            }
        });
    }
}
