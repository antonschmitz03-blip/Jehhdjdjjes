package io.github.antonschmitz.miningdim.client;

import io.github.antonschmitz.miningdim.Knocker;
import io.github.antonschmitz.miningdim.MiningDimension;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class KnockerRenderer extends MobRenderer<Knocker, KnockerRenderState, KnockerModel> {
    private static final String[] SKINS = {"moss", "slate", "clay", "teal"};
    private static final String[] CLOTHES = {"brown", "rust", "blue", "plum", "ash"};
    private static final String[] LAMPS = {"amber", "orange", "pale"};
    private static final Identifier[][] TEXTURES = new Identifier[SKINS.length][CLOTHES.length];
    /** Eyes and cap lamp, drawn like spider eyes so they glow in the dark and shader packs treat them as emissive. */
    private static final RenderType[] GLOW = new RenderType[LAMPS.length];

    static {
        for (int s = 0; s < SKINS.length; s++) {
            for (int c = 0; c < CLOTHES.length; c++) {
                TEXTURES[s][c] = MiningDimension.id("textures/entity/knocker/knocker_" + SKINS[s] + "_" + CLOTHES[c] + ".png");
            }
        }
        for (int l = 0; l < LAMPS.length; l++) {
            GLOW[l] = RenderTypes.eyes(MiningDimension.id("textures/entity/knocker/knocker_glow_" + LAMPS[l] + ".png"));
        }
    }

    public KnockerRenderer(EntityRendererProvider.Context context) {
        super(context, new KnockerModel(context.bakeLayer(KnockerModel.LAYER)), 0.35F);
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, KnockerRenderState state, float yRot, float xRot) {
                if (!state.isInvisible) {
                    collector.order(1).submitModel(this.getParentModel(), state, poseStack, GLOW[state.lamp], lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
                }
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
        state.skin = entity.getSkin();
        state.clothes = entity.getClothes();
        state.lamp = entity.getLamp();
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
    }

    @Override
    public Identifier getTextureLocation(KnockerRenderState state) {
        return TEXTURES[state.skin][state.clothes];
    }
}
