package io.github.antonschmitz.happyghastplus.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Draws an armored harness's visor: one flat face-wide quad (the visor item, its texture picked by tier through
 * custom model data) just in front of the goggles, posed like vanilla's HappyGhastHarnessModel poses them. While
 * ridden the goggles sit over the eyes (model y 14, no tilt); with no rider they are up on the forehead (model y 9)
 * and tipped back 45 degrees, and the visor goes with them.
 */
public class GhastVisorLayer extends RenderLayer<HappyGhastRenderState, HappyGhastModel> {
    public record Visor(String tier, boolean up) {
    }

    public static final RenderStateDataKey<Visor> VISOR = RenderStateDataKey.create(() -> "happyghastplus:ghast_visor");

    // Same model space as GhastEyelidsLayer: y points down from 1.501 blocks above the feet, -z is the face. A
    // harnessed ghast's 16-unit body is drawn 3.75 blocks wide around a centre 2.004 blocks above the feet, and the
    // body's centre is model y 16.
    private static final float UNIT = 3.75F / 16.0F;
    private static final float BODY_CENTER_HEIGHT = 2.004F;
    private static final float GOGGLES_Y_DOWN = 14.0F;
    private static final float GOGGLES_Y_UP = 9.0F;
    private static final float GOGGLES_Z = -5.5F;
    /** The goggles box is 5 deep around its pivot and inflated by 0.15; the visor goes a hair in front of it. */
    private static final float VISOR_Z = -2.65F * UNIT - 0.01F;
    /** The visor texture is the face's size; its centre is 2 units below the goggles' centre. */
    private static final float VISOR_DROP = 2.0F * UNIT;
    private static final float VISOR_SIZE = 16.0F * UNIT;

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState visor = new ItemStackRenderState();
    private final Map<String, ItemStack> stacks = new HashMap<>();

    public GhastVisorLayer(RenderLayerParent<HappyGhastRenderState, HappyGhastModel> parent, ItemModelResolver itemModelResolver) {
        super(parent);
        this.itemModelResolver = itemModelResolver;
    }

    private ItemStack stack(String tier) {
        return stacks.computeIfAbsent(tier, t -> {
            ItemStack stack = new ItemStack(HappyGhastPlus.GHAST_VISOR);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(t), List.of()));
            return stack;
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, HappyGhastRenderState state, float yRot, float xRot) {
        Visor data = ((FabricRenderState) (Object) state).getData(VISOR);
        if (data == null || state.isBaby || state.isInvisible) {
            return;
        }
        itemModelResolver.updateForTopItem(visor, stack(data.tier()), ItemDisplayContext.NONE, null, null, 0);
        poseStack.pushPose();
        float gogglesY = data.up() ? GOGGLES_Y_UP : GOGGLES_Y_DOWN;
        poseStack.translate(0.0F, 1.501F - BODY_CENTER_HEIGHT + (gogglesY - 16.0F) * UNIT, GOGGLES_Z * UNIT);
        if (data.up()) {
            poseStack.rotateDegrees(Axis.XP, -45.0F);
        }
        poseStack.translate(0.0F, VISOR_DROP, VISOR_Z);
        // Undo the model space flip so the texture is upright and not mirrored.
        poseStack.scale(-VISOR_SIZE, -VISOR_SIZE, VISOR_SIZE);
        visor.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
