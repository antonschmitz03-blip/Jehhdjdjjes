package io.github.antonschmitz.happyghastplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.antonschmitz.happyghastplus.HappyGhastPlus;
import io.github.antonschmitz.happyghastplus.RopeLadders;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the rope ladder hanging from the ghast's right-hand side. It is built from one-block ladder pieces (the
 * rope segment item's model, a hook piece on top) stacked down to the ground, so it turns with the ghast.
 */
public class GhastRopeLayer extends RenderLayer<HappyGhastRenderState, HappyGhastModel> {
    public static final RenderStateDataKey<Float> ROPE = RenderStateDataKey.create(() -> "happyghastplus:ghast_rope");

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState segment = new ItemStackRenderState();
    private final ItemStackRenderState top = new ItemStackRenderState();
    private ItemStack segmentStack;
    private ItemStack topStack;

    public GhastRopeLayer(RenderLayerParent<HappyGhastRenderState, HappyGhastModel> parent, ItemModelResolver itemModelResolver) {
        super(parent);
        this.itemModelResolver = itemModelResolver;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, HappyGhastRenderState state, float yRot, float xRot) {
        Float length = ((FabricRenderState) (Object) state).getData(ROPE);
        if (length == null || length <= 0.0F || state.isBaby || state.isInvisible) {
            return;
        }
        if (segmentStack == null) {
            segmentStack = new ItemStack(HappyGhastPlus.GHAST_ROPE_SEGMENT);
            topStack = new ItemStack(HappyGhastPlus.GHAST_ROPE_TOP);
        }
        itemModelResolver.updateForTopItem(segment, segmentStack, ItemDisplayContext.NONE, null, null, 0);
        itemModelResolver.updateForTopItem(top, topStack, ItemDisplayContext.NONE, null, null, 0);

        // Same model space as GhastChestLayer: y points down from 1.501 blocks above the feet, x is mirrored.
        // The right-hand side is -x. Heights below are measured from the ghast's feet.
        double topHeight = RopeLadders.ROPE_TOP;
        double bottom = -length;
        submitPiece(poseStack, collector, light, state, top, (float) (topHeight - 0.5), 1.0F);
        double cellTop = topHeight - 1.0;
        while (cellTop > bottom + 1.0E-3) {
            float fraction = (float) Math.min(1.0, cellTop - bottom);
            submitPiece(poseStack, collector, light, state, segment, (float) (cellTop - fraction / 2.0), fraction);
            cellTop -= 1.0;
        }
    }

    private static void submitPiece(PoseStack poseStack, SubmitNodeCollector collector, int light, HappyGhastRenderState state,
                                    ItemStackRenderState piece, float centerHeight, float heightScale) {
        poseStack.pushPose();
        poseStack.translate((float) -RopeLadders.ROPE_SIDE, 1.501F - centerHeight, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(1.0F, heightScale, 1.0F);
        piece.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
