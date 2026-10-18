package io.github.antonschmitz.metaldecor.client;

import io.github.antonschmitz.metaldecor.MetalDecor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class MetalDecorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(MetalDecor.PEDESTAL_ENTITY, PedestalRenderer::new);
    }
}
