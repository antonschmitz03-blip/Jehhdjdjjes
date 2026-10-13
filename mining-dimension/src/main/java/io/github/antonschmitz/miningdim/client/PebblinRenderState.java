package io.github.antonschmitz.miningdim.client;

import io.github.antonschmitz.miningdim.Pebblin;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class PebblinRenderState extends LivingEntityRenderState {
    public Pebblin.Ore ore = Pebblin.Ore.NONE;
}
