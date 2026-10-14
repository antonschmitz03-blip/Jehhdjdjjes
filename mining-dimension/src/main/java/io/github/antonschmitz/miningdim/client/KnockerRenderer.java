package io.github.antonschmitz.miningdim.client;

import io.github.antonschmitz.miningdim.Knocker;
import io.github.antonschmitz.miningdim.MiningDimension;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class KnockerRenderer extends MobRenderer<Knocker, KnockerRenderState, KnockerModel> {
    private static final Identifier TEXTURE = MiningDimension.id("textures/entity/knocker/knocker.png");
    /** Eyes and cap lamp, drawn like spider eyes so they glow in the dark and shader packs treat them as emissive. */
    private static final RenderType GLOW = RenderTypes.eyes(MiningDimension.id("textures/entity/knocker/knocker_glow.png"));

    public KnockerRenderer(EntityRendererProvider.Context context) {
        super(context, new KnockerModel(context.bakeLayer(KnockerModel.LAYER)), 0.35F);
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public KnockerRenderState createRenderState() {
        return new KnockerRenderState();
    }

    @Override
    public void extractRenderState(Knocker entity, KnockerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
    }

    @Override
    public Identifier getTextureLocation(KnockerRenderState state) {
        return TEXTURE;
    }
}
