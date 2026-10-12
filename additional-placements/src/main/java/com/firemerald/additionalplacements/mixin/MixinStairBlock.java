package com.firemerald.additionalplacements.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.firemerald.additionalplacements.block.interfaces.IStairBlock.IVanillaStairBlock;
import com.firemerald.additionalplacements.block.stairs.AdditionalStairBlock;
import com.firemerald.additionalplacements.block.stairs.StairConnectionsType;
import com.firemerald.additionalplacements.block.stairs.common.CommonStairShapeState;
import com.firemerald.additionalplacements.block.stairs.vanilla.VanillaStairShapeState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(StairBlock.class)
public abstract class MixinStairBlock implements IVanillaStairBlock {
	@Unique
    private AdditionalStairBlock additionalplacements$stairs;
	@Final
	@Shadow
	protected BlockState baseState;

	@Unique
    private StairBlock additionalplacements$asStair() {
		return (StairBlock) (Object) this;
	}

	@Override
	public void additionalplacements$setOtherBlock(AdditionalStairBlock stairs) {
		this.additionalplacements$stairs = stairs;
	}

	@Override
	public AdditionalStairBlock additionalplacements$getOtherBlock() {
		return additionalplacements$stairs;
	}

	@Override
	public boolean additionalplacements$hasAdditionalStates() {
		return additionalplacements$stairs != null;
	}

	@Override
	public boolean additionalplacements$isThis(BlockState blockState) {
		return blockState.is(additionalplacements$asStair()) || blockState.is(additionalplacements$stairs);
	}

	@Override
	public BlockState additionalplacements$getDefaultVanillaState(BlockState currentState) {
		return currentState.is(additionalplacements$asStair()) ? currentState : additionalplacements$stairs.copyProperties(currentState, additionalplacements$asStair().defaultBlockState());
	}

	@Override
	public BlockState additionalplacements$getDefaultAdditionalState(BlockState currentState) {
		return currentState.is(additionalplacements$stairs) ? currentState : additionalplacements$stairs.copyProperties(currentState, additionalplacements$stairs.defaultBlockState());
	}

	@ModifyReturnValue(method = BlockMethods.GET_STATE_FOR_PLACEMENT_NAME, at = @At("RETURN"))
	private BlockState getStateForPlacement(BlockState original, @Local(argsOnly = true) BlockPlaceContext context) {
		if (this.additionalplacements$hasAdditionalStates() && additionalplacements$enablePlacement(context.getClickedPos(), context.getLevel(), context.getClickedFace(), context.getPlayer())) return additionalplacements$getStateForPlacementImpl(context, original);
		else return original;
	}

	@Inject(method = BlockMethods.ROTATE_NAME, at = @At("HEAD"), cancellable = true)
	private void rotate(BlockState blockState, Rotation rotation, CallbackInfoReturnable<BlockState> ci) {
		if (this.additionalplacements$hasAdditionalStates()) ci.setReturnValue(additionalplacements$rotateImpl(blockState, rotation));
	}

	@Inject(method = BlockMethods.MIRROR_NAME, at = @At("HEAD"), cancellable = true)
	private void mirror(BlockState blockState, Mirror mirror, CallbackInfoReturnable<BlockState> ci) {
		if (this.additionalplacements$hasAdditionalStates()) ci.setReturnValue(additionalplacements$mirrorImpl(blockState, mirror));
	}

	@Inject(method = BlockMethods.UPDATE_SHAPE_NAME, at = @At("HEAD"), cancellable = true)
	private void updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos otherPos, BlockState otherState, RandomSource rand, CallbackInfoReturnable<BlockState> ci) {
		if (this.additionalplacements$hasAdditionalStates()) ci.setReturnValue(additionalplacements$updateShapeImpl(state, level, tickAccess, pos, direction, otherPos, otherState, rand));
	}

	@Override
	public BlockState additionalplacements$getModelStateImpl() {
		return baseState;
	}

	@Override
	public BlockState additionalplacements$getBlockStateInternal(CommonStairShapeState shapeState, BlockState currentState) {
		return additionalplacements$stairs.additionalplacements$getBlockStateInternal(shapeState, currentState);
	}

	@Override
	public CommonStairShapeState additionalplacements$getShapeState(BlockState blockState) {
		return VanillaStairShapeState.toCommon(
				blockState.getValue(StairBlock.FACING),
				blockState.getValue(StairBlock.HALF),
				blockState.getValue(StairBlock.SHAPE));
	}

    @Override
	public StairConnectionsType additionalplacements$connectionsType() {
		return additionalplacements$stairs.additionalplacements$connectionsType();
	}
}