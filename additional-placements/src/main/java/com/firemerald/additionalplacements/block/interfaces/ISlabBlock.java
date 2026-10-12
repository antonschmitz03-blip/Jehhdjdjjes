package com.firemerald.additionalplacements.block.interfaces;

import java.util.List;
import java.util.function.Function;

import com.firemerald.additionalplacements.client.BlockHighlightHelper;
import com.firemerald.additionalplacements.compat.LoadedMods;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.firemerald.additionalplacements.block.VerticalSlabBlock;
import com.firemerald.additionalplacements.generation.APGenerationTypes;
import com.firemerald.additionalplacements.generation.GenerationType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

public interface ISlabBlock<T extends Block> extends IPlacementBlock<T>, IPaneConnectable {
	interface IVanillaSlabBlock extends ISlabBlock<VerticalSlabBlock>, IVanillaBlock<VerticalSlabBlock> {
		@Override
		default boolean additionalplacements$enablePlacement(BlockPos pos, Level level, Direction direction, @Nullable Player player) {
			if (ISlabBlock.super.additionalplacements$enablePlacement(pos, level, direction, player)) {
				if (LoadedMods.DOUBLESLABS.isPresent) {
					BlockState blockState = level.getBlockState(pos);
					if (blockState.getBlock() instanceof SlabBlock) {
						if (
								(blockState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM && direction == Direction.UP) ||
								(blockState.getValue(SlabBlock.TYPE) == SlabType.TOP && direction == Direction.DOWN)
						) return false;
					}
				}
				return true;
			} else return false;
		}

		@Override
        default Direction additionalplacements$getPlacing(BlockState blockState) {
			SlabType type = blockState.getValue(SlabBlock.TYPE);
			return type == SlabType.DOUBLE ? null : type == SlabType.TOP ? Direction.UP : Direction.DOWN;
		}
	}

	@Override
    default BlockState additionalplacements$transform(BlockState blockState, Function<Direction, Direction> transform) {
		Direction placing = additionalplacements$getPlacing(blockState);
		return placing == null ? blockState : additionalplacements$forPlacing(transform.apply(placing), blockState);
	}

	default boolean additionalplacements$canBeReplacedImpl(BlockState state, BlockPlaceContext context) {
		ItemStack itemstack = context.getItemInHand();
		if (itemstack.is(this.asItem())) {
			if (context.replacingClickedOnBlock()) {
				Direction placing = additionalplacements$getPlacing(state);
				if (placing != null) {
					Direction direction = additionalplacements$getPlacingDirection(context);
					return direction.getAxis() == placing.getAxis() && context.getClickedFace().getOpposite() == placing;
				}
				else return false;
			}
			else return true;
		}
		else return false;
	}

	@Override
    default BlockState additionalplacements$getStateForPlacementImpl(BlockPlaceContext context, BlockState currentState) {
		BlockPos blockPos = context.getClickedPos();
		BlockState blockState = context.getLevel().getBlockState(blockPos);
        if (additionalplacements$isThis(blockState)) return blockState.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        else return additionalplacements$forPlacing(additionalplacements$getPlacingDirection(context), currentState);
	}

	default BlockState additionalplacements$forPlacing(Direction dir, BlockState blockState) {
		return (dir.getAxis() == Axis.Y ?
				additionalplacements$getDefaultVanillaState(blockState) :
				(additionalplacements$getDefaultAdditionalState(blockState).setValue(VerticalSlabBlock.AXIS, dir.getAxis())))
				.setValue(SlabBlock.TYPE, dir.getAxisDirection() == AxisDirection.POSITIVE ? SlabType.TOP : SlabType.BOTTOM);
	}

	@Nullable
    Direction additionalplacements$getPlacing(BlockState blockState);

	default Direction additionalplacements$getPlacingDirection(BlockPlaceContext context) {
		double hitX = context.getClickLocation().x - context.getClickedPos().getX() - .5;
		double hitY = context.getClickLocation().y - context.getClickedPos().getY() - .5;
		double hitZ = context.getClickLocation().z - context.getClickedPos().getZ() - .5;
        switch (context.getClickedFace().getAxis()) {
			case X:
				if (Math.abs(hitY) <= .25f && Math.abs(hitZ) <= .25f) return context.getClickedFace().getOpposite();
				else if (hitZ > hitY) {
					if (hitZ > -hitY) return Direction.SOUTH;
					else return Direction.DOWN;
				}
				else {
					if (hitY > -hitZ) return Direction.UP;
					else return Direction.NORTH;
				}
			case Y:
				if (Math.abs(hitX) <= .25f && Math.abs(hitZ) <= .25f) return context.getClickedFace().getOpposite();
				else if (hitX > hitZ) {
					if (hitX > -hitZ) return Direction.EAST;
					else return Direction.NORTH;
				}
				else {
					if (hitZ > -hitX) return Direction.SOUTH;
					else return Direction.WEST;
				}
			case Z:
				if (Math.abs(hitX) <= .25f && Math.abs(hitY) <= .25f) return context.getClickedFace().getOpposite();
				else if (hitX > hitY) {
					if (hitX > -hitY) return Direction.EAST;
					else return Direction.DOWN;
				}
				else {
					if (hitY > -hitX) return Direction.UP;
					else return Direction.WEST;
				}
			default:
				return null;
        }
	}

	float OUTER_EDGE = .5f;
	float INNER_EDGE = .25f;

	@Override
	default void additionalplacements$renderPlacementHighlight(PoseStack pose, VertexConsumer vertexConsumer, Player player, BlockHitResult result, DeltaTracker delta, float r, float g, float b, float a, float width) {
		PoseStack.Pose lastPose = pose.last();

		//outer box
		BlockHighlightHelper.lineCenteredSquare(vertexConsumer, lastPose, -OUTER_EDGE, r, g, b, a,
				OUTER_EDGE,
				width);

		//inner box
		BlockHighlightHelper.lineCenteredSquare(vertexConsumer, lastPose, -OUTER_EDGE, r, g, b, a,
				INNER_EDGE,
				width);

		//diagonals
		BlockHighlightHelper.lineAxisDiagonal(vertexConsumer, lastPose, -OUTER_EDGE, r, g, b, a,
				INNER_EDGE, OUTER_EDGE,
				width);
	}

	@Override
    default GenerationType<?, ?> additionalplacements$getGenerationType() {
		return APGenerationTypes.slab();
	}

    @Override
    default void additionalplacements$addPlacementTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.additionalplacements.vertical_placement"));
	}

	Axis additionalplacements$getAxis(BlockState state);

	@Override
	default boolean additionalplacements$paneConnectOverride(BlockState ourState, Axis paneAxis, Direction connectDir) {
		SlabType type = ourState.getValue(BlockStateProperties.SLAB_TYPE);
		if (type == SlabType.DOUBLE) return true;
		else {
			Axis axis = additionalplacements$getAxis(ourState);
			if (axis == paneAxis) return false;
			else if (axis != connectDir.getAxis()) return true; //connect from sides
			else return (connectDir.getAxisDirection() == AxisDirection.POSITIVE) ^ (type == SlabType.TOP); //connect from back
		}
	}
}