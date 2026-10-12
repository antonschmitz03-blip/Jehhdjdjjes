package com.firemerald.additionalplacements.block.interfaces;

import java.util.List;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.firemerald.additionalplacements.common.IAPPlayer;
import com.firemerald.additionalplacements.config.APConfigs;
import com.firemerald.additionalplacements.generation.GenerationType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

public interface IPlacementBlock<T extends Block> extends ItemLike, IGenerationControl {
	T additionalplacements$getOtherBlock();

	default BlockState additionalplacements$rotateImpl(BlockState blockState, Rotation rotation) {
		return additionalplacements$transform(blockState, rotation::rotate);
	}

	default BlockState additionalplacements$mirrorImpl(BlockState blockState, Mirror mirror) {
		return additionalplacements$transform(blockState, mirror::mirror);
	}

	BlockState additionalplacements$transform(BlockState blockState, Function<Direction, Direction> transform);

	BlockState additionalplacements$getStateForPlacementImpl(BlockPlaceContext context, BlockState currentState);

	BlockState additionalplacements$updateShapeImpl(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos otherPos, BlockState otherState, RandomSource rand);

	default void additionalplacements$appendHoverTextImpl(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (APConfigs.common().showTooltip.get() && additionalplacements$getGenerationType().placementEnabled()) additionalplacements$addPlacementTooltip(stack, context, tooltip, flag);
	}

	void additionalplacements$addPlacementTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag);

	boolean additionalplacements$hasAdditionalStates();

	BlockState additionalplacements$getDefaultAdditionalState(BlockState currentState);

	BlockState additionalplacements$getDefaultVanillaState(BlockState currentState);

	boolean additionalplacements$isThis(BlockState blockState);

	float SQRT_2_INV = 0.70710678118654752440084436210485f;

	Quaternionf[] DIRECTION_TRANSFORMS = new Quaternionf[] {
			new Quaternionf(SQRT_2_INV, 0, 0, SQRT_2_INV), //DOWN
			new Quaternionf(-SQRT_2_INV, 0, 0, SQRT_2_INV), //UP
			new Quaternionf(0, 1, 0, 0), //NORTH
			new Quaternionf(0, 0, 0, 1), //SOUTH
			new Quaternionf(0, -SQRT_2_INV, 0, SQRT_2_INV), //WEST
			new Quaternionf(0, SQRT_2_INV, 0, SQRT_2_INV), //EAST
	};

	default void additionalplacements$renderHighlight(PoseStack pose, MultiBufferSource bufferSource, Player player, BlockHitResult result, LevelRenderState renderState, DeltaTracker delta) {
		BlockPos hit = result.getBlockPos();
		if (additionalplacements$enablePlacement(hit, player.level(), result.getDirection(), player)) {
			pose.pushPose();
			double hitX = hit.getX();
			double hitY = hit.getY();
			double hitZ = hit.getZ();
			switch (result.getDirection()) {
				case WEST:
					hitX = result.getLocation().x - 1.005;
					break;
				case EAST:
					hitX = result.getLocation().x + .005;
					break;
				case DOWN:
					hitY = result.getLocation().y - 1.005;
					break;
				case UP:
					hitY = result.getLocation().y + .005;
					break;
				case NORTH:
					hitZ = result.getLocation().z - 1.005;
					break;
				case SOUTH:
					hitZ = result.getLocation().z + .005;
					break;
				default:
			}
			Vec3 pos = renderState.cameraRenderState.pos;
			pose.translate(hitX - pos.x + .5, hitY - pos.y + .5, hitZ - pos.z + .5);

			boolean highContrast = renderState.blockOutlineRenderState.highContrast();
			float lineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
			float[] previewColor;
			if (highContrast) {
				float[] backgroundColor = APConfigs.client().previewColorHCB();
				if (backgroundColor[3] > 0) additionalplacements$renderPlacementPreview(pose, bufferSource.getBuffer(RenderTypes.secondaryBlockOutline()), player, result, delta, backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3], 7f);
				previewColor = APConfigs.client().previewColorHC();
			} else previewColor = APConfigs.client().previewColor();
			if (previewColor[3] > 0) additionalplacements$renderPlacementPreview(pose, bufferSource.getBuffer(RenderTypes.lines()), player, result, delta, previewColor[0], previewColor[1], previewColor[2], previewColor[3], lineWidth);
			pose.mulPose(DIRECTION_TRANSFORMS[result.getDirection().ordinal()]);
			float[] gridColor;
			if (highContrast) {
				float[] backgroundColor = APConfigs.client().gridColorHCB();
				if (backgroundColor[3] > 0) additionalplacements$renderPlacementHighlight(pose, bufferSource.getBuffer(RenderTypes.secondaryBlockOutline()), player, result, delta, backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3], 7f);
				gridColor = APConfigs.client().gridColorHC();
			} else gridColor = APConfigs.client().gridColor();
			if (gridColor[3] > 0) additionalplacements$renderPlacementHighlight(pose, bufferSource.getBuffer(RenderTypes.lines()), player, result, delta, gridColor[0], gridColor[1], gridColor[2], highContrast ? 0.4f : gridColor[3], lineWidth);
			pose.popPose();
		}
	}

	default void additionalplacements$renderPlacementPreview(PoseStack pose, VertexConsumer vertexConsumer, Player player, BlockHitResult result, DeltaTracker delta, float r, float g, float b, float a, float width) {}

	void additionalplacements$renderPlacementHighlight(PoseStack pose, VertexConsumer vertexConsumer, Player player, BlockHitResult result, DeltaTracker delta, float r, float g, float b, float a, float width);

	default boolean additionalplacements$enablePlacement(@Nullable Player player) {
		return additionalplacements$getGenerationType().placementEnabled() && (!(player instanceof IAPPlayer apPlayer) || apPlayer.additionalplacements$isPlacementEnabled());
	}

	default boolean additionalplacements$enablePlacement(BlockPos hit, Level level, Direction direction, Player player) {
		return additionalplacements$enablePlacement(player);
	}

	GenerationType<?, ?> additionalplacements$getGenerationType();

	@Override
    default boolean additionalplacements$generateAdditionalStates() {
		return true;
	}

	default boolean additionalplacements$canGenerateAdditionalStates() {
		return additionalplacements$generateAdditionalStates() && !additionalplacements$hasAdditionalStates();
	}
}