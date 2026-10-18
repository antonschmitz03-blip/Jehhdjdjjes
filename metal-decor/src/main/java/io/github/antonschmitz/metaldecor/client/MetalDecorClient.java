package io.github.antonschmitz.metaldecor.client;

import io.github.antonschmitz.metaldecor.MetalDecor;
import net.fabricmc.api.ClientModInitializer;
import io.github.antonschmitz.metaldecor.mixin.client.BlockEntityRenderersInvoker;

public class MetalDecorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderersInvoker.metaldecor$register(MetalDecor.PEDESTAL_ENTITY, PedestalRenderer::new);
    }
}
