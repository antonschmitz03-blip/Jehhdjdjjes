package com.firemerald.additionalplacements.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.firemerald.additionalplacements.block.AdditionalCarpetBlock;
import com.firemerald.additionalplacements.block.interfaces.ICarpetBlock.IVanillaCarpetBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(CarpetBlock.class)
public abstract class MixinCarpetBlock extends Block implements IVanillaCarpetBlock {
	private MixinCarpetBlock(Properties properties) {
		super(properties);
	}

	@Unique
    private AdditionalCarpetBlock additionalplacements$carpet;

	@Unique
    private CarpetBlock additionalplacements$asCarpet() {
		return (CarpetBlock) (Object) this;
	}

	@Override
	public void additionalplacements$setOtherBlock(AdditionalCarpetBlock carpet) {
		this.additionalplacements$carpet = carpet;
	}

	@Override
	public AdditionalCarpetBlock additionalplacements$getOtherBlock() {
		return additionalplacements$carpet;
	}

	@Override
	public boolean additionalplacements$hasAdditionalStates() {
		return additionalplacements$carpet != null;
	}

	@Override
	public Direction additionalplacements$getPlacing(BlockState blockState) {
		return Direction.DOWN;
	}

	@Override
	public boolean additionalplacements$isThis(BlockState blockState) {
		return blockState.is(additionalplacements$asCarpet()) || blockState.is(additionalplacements$carpet);
	}

	@Override
	public BlockState additionalplacements$getDefaultVanillaState(BlockState currentState) {
		return currentState.is(additionalplacements$asCarpet()) ? currentState : additionalplacements$carpet.copyProperties(currentState, additionalplacements$asCarpet().defaultBlockState());
	}

	@Override
	public BlockState additionalplacements$getDefaultAdditionalState(BlockState currentState) {
		return currentState.is(additionalplacements$carpet) ? currentState : additionalplacements$carpet.copyProperties(currentState, additionalplacements$carpet.defaultBlockState());
	}

	@ModifyReturnValue(at = @At("RETURN"), remap = false, require = 0, method = {
			BlockMethods.GET_STATE_FOR_PLACEMENT_NAME,
			BlockMethods.GET_STATE_FOR_PLACEMENT_OBF_NAME
	})
	private BlockState getStateForPlacement(BlockState original, @Local(argsOnly = true) BlockPlaceContext context) {
		if (this.additionalplacements$hasAdditionalStates() && additionalplacements$enablePlacement(context.getClickedPos(), context.getLevel(), context.getClickedFace(), context.getPlayer())) return additionalplacements$getStateForPlacementImpl(context, original);
		else return original;
	}

	@Override
	@Unique(silent = true)
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
		BlockState superRet = super.getStateForPlacement(context);
		if (this.additionalplacements$hasAdditionalStates() && additionalplacements$enablePlacement(context.getClickedPos(), context.getLevel(), context.getClickedFace(), context.getPlayer())) return additionalplacements$getStateForPlacementImpl(context, superRet);
		else return superRet;
	}

	@Inject(at = @At("HEAD"), remap = false, cancellable = true, require = 0, method = {
			BlockMethods.ROTATE_NAME,
			BlockMethods.ROTATE_OBF_NAME
	})
	private void rotate(BlockState blockState, Rotation rotation, CallbackInfoReturnable<BlockState> ci) {
		if (this.additionalplacements$hasAdditionalStates()) ci.setReturnValue(additionalplacements$rotateImpl(blockState, rotation));
	}

	@Override
	@Unique(silent = true)
	public @NotNull BlockState rotate(@NotNull BlockState blockState, @NotNull Rotation rotation) {
		if (this.additionalplacements$hasAdditionalStates()) return additionalplacements$rotateImpl(blockState, rotation);
		else return super.rotate(blockState, rotation);
	}

	@Inject(at = @At("HEAD"), remap = false, cancellable = true, require = 0, method = {
			BlockMethods.MIRROR_NAME,
			BlockMethods.MIRROR_OBF_NAME
	})
	private void mirror(BlockState blockState, Mirror mirror, CallbackInfoReturnable<BlockState> ci) {
		if (this.additionalplacements$hasAdditionalStates()) ci.setReturnValue(additionalplacements$mirrorImpl(blockState, mirror));
	}

	@Override
	@Unique(silent = true)
	public @NotNull BlockState mirror(@NotNull BlockState blockState, @NotNull Mirror mirror) {
		if (this.additionalplacements$hasAdditionalStates()) return additionalplacements$mirrorImpl(blockState, mirror);
		else return super.mirror(blockState, mirror);
	}

	@Override
	public BlockState additionalplacements$updateShapeImpl(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos otherPos, BlockState otherState, RandomSource rand) {
		return super.updateShape(state, level, tickAccess, pos, direction, otherPos, otherState, rand);
	}
}