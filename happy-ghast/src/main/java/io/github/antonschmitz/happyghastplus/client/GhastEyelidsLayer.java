package io.github.antonschmitz.happyghastplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.antonschmitz.happyghastplus.HappyGhastPlus;
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
 * Closes a sleeping ghast's eyes: draws the eyelids item (one flat face-sized quad that covers the open eyes with
 * closed ones and leaves the smile) just in front of the ghast's face.
 */
public class GhastEyelidsLayer extends RenderLayer<HappyGhastRenderState, HappyGhastModel> {
    public static final RenderStateDataKey<Boolean> SLEEPING = RenderStateDataKey.create(() -> "happyghastplus:ghast_sleeping");

    // Same model space as GhastChestLayer: y points down from 1.501 blocks above the feet, -z is the face.
    // Only harnessed ghasts sleep, and a harness squeezes the 4-block body to 3.75 blocks around its centre,
    // which sits 2.004 blocks above the feet. The quad goes a hair in front of the face to avoid z-fighting.
    private static final float FACE_CENTER_HEIGHT = 2.004F;
    private static final float FACE_Z = -1.875F - 0.01F;
    private static final float FACE_SIZE = 3.75F;

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState eyelids = new ItemStackRenderState();
    private ItemStack eyelidsStack;

    public GhastEyelidsLayer(RenderLayerParent<HappyGhastRenderState, HappyGhastModel> parent, ItemModelResolver itemModelResolver) {
        super(parent);
        this.itemModelResolver = itemModelResolver;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, HappyGhastRenderState state, float yRot, float xRot) {
        Boolean sleeping = ((FabricRenderState) (Object) state).getData(SLEEPING);
        if (sleeping == null || !sleeping || state.isBaby || state.isInvisible) {
            return;
        }
        if (eyelidsStack == null) {
            eyelidsStack = new ItemStack(HappyGhastPlus.GHAST_EYELIDS);
        }
        itemModelResolver.updateForTopItem(eyelids, eyelidsStack, ItemDisplayContext.NONE, null, null, 0);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.501F - FACE_CENTER_HEIGHT, FACE_Z);
        // Undo the model space flip so the texture is upright and not mirrored.
        poseStack.scale(-FACE_SIZE, -FACE_SIZE, FACE_SIZE);
        eyelids.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
