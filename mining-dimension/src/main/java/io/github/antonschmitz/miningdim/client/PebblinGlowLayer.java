package io.github.antonschmitz.miningdim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.antonschmitz.miningdim.MiningDimension;
import io.github.antonschmitz.miningdim.Pebblin;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the ore crystals full-bright with the same render type as spider and enderman eyes,
 * which shader packs (Iris) treat as emissive.
 */
public class PebblinGlowLayer extends RenderLayer<PebblinRenderState, PebblinModel> {
    private static final Map<Pebblin.Ore, RenderType> GLOW = new EnumMap<>(Pebblin.Ore.class);

    static {
        for (Pebblin.Ore ore : Pebblin.Ore.values()) {
            if (ore != Pebblin.Ore.NONE) {
                GLOW.put(ore, RenderTypes.eyes(MiningDimension.id("textures/entity/pebblin/pebblin_" + ore.name + "_glow.png")));
            }
        }
    }

    public PebblinGlowLayer(RenderLayerParent<PebblinRenderState, PebblinModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, PebblinRenderState state, float yRot, float xRot) {
        RenderType glow = GLOW.get(state.ore);
        if (glow != null && !state.isInvisible) {
            collector.order(1).submitModel(this.getParentModel(), state, poseStack, glow, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
