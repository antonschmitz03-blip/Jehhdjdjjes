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
 * Draws a small rocket engine on each side of a happy ghast with the speed upgrade. The engine is the speed
 * upgrade item's 3D model (its "none" display context), strapped towards the body.
 */
public class GhastEngineLayer extends RenderLayer<HappyGhastRenderState, HappyGhastModel> {
    public static final RenderStateDataKey<Boolean> ENGINES = RenderStateDataKey.create(() -> "happyghastplus:ghast_engines");

    // Same model space as GhastChestLayer: y points down from 1.501 blocks above the feet, +z is the back.
    private static final float SIDE_OFFSET = 2.2F;
    private static final float HEIGHT = 2.6F;
    private static final float SCALE = 1.25F;

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState engine = new ItemStackRenderState();
    private ItemStack engineStack;

    public GhastEngineLayer(RenderLayerParent<HappyGhastRenderState, HappyGhastModel> parent, ItemModelResolver itemModelResolver) {
        super(parent);
        this.itemModelResolver = itemModelResolver;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, HappyGhastRenderState state, float yRot, float xRot) {
        Boolean engines = ((FabricRenderState) (Object) state).getData(ENGINES);
        if (engines == null || !engines || state.isBaby || state.isInvisible) {
            return;
        }
        if (engineStack == null) {
            engineStack = new ItemStack(HappyGhastPlus.GHAST_SPEED_UPGRADE);
        }
        itemModelResolver.updateForTopItem(engine, engineStack, ItemDisplayContext.NONE, null, null, 0);
        for (int side : new int[] {1, -1}) {
            poseStack.pushPose();
            poseStack.translate(side * SIDE_OFFSET, 1.501F - HEIGHT, 0.0F);
            poseStack.scale(-SCALE, -SCALE, SCALE);
            if (side < 0) {
                // The model's strap points one way; turn it upside down so it faces the body on the other side.
                poseStack.scale(-1.0F, -1.0F, 1.0F); // 180° around z
            }
            engine.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
