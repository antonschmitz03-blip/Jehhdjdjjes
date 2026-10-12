package io.github.antonschmitz.vplustweaks.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.item.Items;

/** Draws a double chest (two chests side by side) on top of a happy ghast, towards its back. */
public class GhastChestLayer extends RenderLayer<HappyGhastRenderState, HappyGhastModel> {
    public static final RenderStateDataKey<Boolean> HAS_CHEST = RenderStateDataKey.create(() -> "vplustweaks:ghast_chest");

    // Model space: y points down from 1.501 blocks above the feet, +z is the ghast's back.
    private static final float CHEST_CENTER_HEIGHT = 4.5F;
    private static final float CHEST_BACK_OFFSET = 1.3F;

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState chest = new ItemStackRenderState();
    private final ItemStack chestStack = new ItemStack(Items.CHEST);

    public GhastChestLayer(RenderLayerParent<HappyGhastRenderState, HappyGhastModel> parent, ItemModelResolver itemModelResolver) {
        super(parent);
        this.itemModelResolver = itemModelResolver;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, HappyGhastRenderState state, float yRot, float xRot) {
        Boolean hasChest = ((FabricRenderState) (Object) state).getData(HAS_CHEST);
        if (hasChest == null || !hasChest || state.isBaby || state.isInvisible) {
            return;
        }
        itemModelResolver.updateForTopItem(chest, chestStack, ItemDisplayContext.NONE, null, null, 0);
        for (float x : new float[] {-0.5F, 0.5F}) {
            poseStack.pushPose();
            poseStack.translate(x, 1.501F - CHEST_CENTER_HEIGHT, CHEST_BACK_OFFSET);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            chest.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
