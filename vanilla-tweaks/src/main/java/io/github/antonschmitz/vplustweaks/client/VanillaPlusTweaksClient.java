package io.github.antonschmitz.vplustweaks.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;

public class VanillaPlusTweaksClient implements ClientModInitializer {
    @Override
    @SuppressWarnings("unchecked")
    public void onInitializeClient() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof HappyGhastRenderer) {
                helper.register(new GhastChestLayer(
                        (RenderLayerParent<HappyGhastRenderState, HappyGhastModel>) (Object) renderer,
                        context.getItemModelResolver()));
            }
        });
    }
}
