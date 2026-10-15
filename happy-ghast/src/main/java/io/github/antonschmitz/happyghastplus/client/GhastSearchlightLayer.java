package io.github.antonschmitz.happyghastplus.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Draws the searchlight on top of the harness: the base turns to the aim's yaw, the head tilts to its pitch. Both are
 * the searchlight item's 3D models (display context "none"), picked by custom model data.
 */
public class GhastSearchlightLayer extends RenderLayer<HappyGhastRenderState, HappyGhastModel> {
    public static final RenderStateDataKey<SearchlightAim> AIM = RenderStateDataKey.create(() -> "happyghastplus:ghast_searchlight");

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState base = new ItemStackRenderState();
    private final ItemStackRenderState head = new ItemStackRenderState();
    private ItemStack baseStack;
    private ItemStack headStack;
    private ItemStack litHeadStack;

    public GhastSearchlightLayer(RenderLayerParent<HappyGhastRenderState, HappyGhastModel> parent, ItemModelResolver itemModelResolver) {
        super(parent);
        this.itemModelResolver = itemModelResolver;
    }

    private static ItemStack part(String name) {
        ItemStack stack = new ItemStack(HappyGhastPlus.GHAST_SEARCHLIGHT);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(name), List.of()));
        return stack;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, HappyGhastRenderState state, float yRot, float xRot) {
        SearchlightAim aim = ((FabricRenderState) (Object) state).getData(AIM);
        if (aim == null || aim == SearchlightAim.NONE || state.isBaby || state.isInvisible) {
            return;
        }
        if (baseStack == null) {
            baseStack = part("base");
            headStack = part("head");
            litHeadStack = part("head_lit");
        }
        itemModelResolver.updateForTopItem(base, baseStack, ItemDisplayContext.NONE, null, null, 0);
        itemModelResolver.updateForTopItem(head, aim.on() ? litHeadStack : headStack, ItemDisplayContext.NONE, null, null, 0);

        poseStack.pushPose();
        // Model space has y pointing down from 1.501 blocks above the feet and the face at -z; flip back to y up.
        poseStack.translate(0.0F, 1.501F - SearchlightAim.MOUNT_HEIGHT, -SearchlightAim.MOUNT_FORWARD);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // Now +x is the ghast's right and -z its front; a positive yaw turns the lamp to the right.
        poseStack.mulPose(Axis.YP.rotationDegrees(-aim.yaw()));

        poseStack.pushPose();
        poseStack.scale(SearchlightAim.SCALE, SearchlightAim.SCALE, SearchlightAim.SCALE);
        poseStack.translate(0.0F, 0.5F, 0.0F); // item models are centred; stand the base on its foot
        base.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();

        poseStack.translate(0.0F, SearchlightAim.PIVOT, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-aim.pitch())); // positive pitch tips the lens (-z) down
        poseStack.scale(SearchlightAim.SCALE, SearchlightAim.SCALE, SearchlightAim.SCALE);
        head.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
