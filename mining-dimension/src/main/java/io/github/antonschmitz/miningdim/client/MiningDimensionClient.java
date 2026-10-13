package io.github.antonschmitz.miningdim.client;

import io.github.antonschmitz.miningdim.MiningDimension;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class MiningDimensionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(PebblinModel.LAYER, PebblinModel::createBodyLayer);
        EntityRendererRegistry.register(MiningDimension.PEBBLIN, PebblinRenderer::new);
    }
}
