package io.github.antonschmitz.miningdim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.antonschmitz.miningdim.MiningDimension;
import io.github.antonschmitz.miningdim.Pebblin;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class PebblinRenderer extends MobRenderer<Pebblin, PebblinRenderState, PebblinModel> {
    private static final Map<Pebblin.Ore, Identifier> TEXTURES = new EnumMap<>(Pebblin.Ore.class);

    static {
        for (Pebblin.Ore ore : Pebblin.Ore.values()) {
            TEXTURES.put(ore, MiningDimension.id("textures/entity/pebblin/pebblin_" + ore.name + ".png"));
        }
    }

    public PebblinRenderer(EntityRendererProvider.Context context) {
        super(context, new PebblinModel(context.bakeLayer(PebblinModel.LAYER)), 0.35F);
    }

    @Override
    public PebblinRenderState createRenderState() {
        return new PebblinRenderState();
    }

    @Override
    public void extractRenderState(Pebblin entity, PebblinRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ore = entity.getOre();
    }

    @Override
    public Identifier getTextureLocation(PebblinRenderState state) {
        return TEXTURES.get(state.ore);
    }

    @Override
    protected void scale(PebblinRenderState state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.6F, 0.6F, 0.6F);
        }
    }
}
