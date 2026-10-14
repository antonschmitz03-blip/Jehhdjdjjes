package io.github.antonschmitz.miningdim.client;

import io.github.antonschmitz.miningdim.MiningDimension;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class MiningDimensionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(PebblinModel.LAYER, PebblinModel::createBodyLayer);
        EntityRendererRegistry.register(MiningDimension.PEBBLIN, PebblinRenderer::new);
        ModelLayerRegistry.registerModelLayer(KnockerModel.LAYER, KnockerModel::createBodyLayer);
        EntityRendererRegistry.register(MiningDimension.KNOCKER, KnockerRenderer::new);
        EntityRendererRegistry.register(MiningDimension.KNOCKER_PEBBLE, ThrownItemRenderer::new);
    }
}
