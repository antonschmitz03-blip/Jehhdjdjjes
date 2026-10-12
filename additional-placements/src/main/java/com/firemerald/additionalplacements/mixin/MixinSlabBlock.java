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

import com.firemerald.additionalplacements.block.VerticalSlabBlock;
import com.firemerald.additionalplacements.block.interfaces.ISlabBlock.IVanillaSlabBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SlabBlock.class)
public abstract class MixinSlabBlock extends Block implements IVanillaSlabBlock {
	private MixinSlabBlock(Properties properties) {
		super(properties);
	}

	@Unique
    private VerticalSlabBlock additionalplacements$slab;

	@Unique
    private SlabBlock additionalplacements$asSlab() {
		return (SlabBlock) (Object) this;
	}

	@Override
	public void additionalplacements$setOtherBlock(VerticalSlabBlock slab) {
		this.additionalplacements$slab = slab;
	}

	@Override
	public VerticalSlabBlock additionalplacements$getOtherBlock() {
		return additionalplacements$slab;
	}

	@Override
	public boolean additionalplacements$hasAdditionalStates() {
		return additionalplacements$slab != null;
	}

	@Override
	public Direction additionalplacements$getPlacing(BlockState blockState) {
        return switch (blockState.getValue(SlabBlock.TYPE)) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            default -> null;
        };
	}

	@Override
	public boolean additionalplacements$isThis(BlockState blockState) {
		return blockState.is(additionalplacements$asSlab()) || blockState.is(additionalplacements$slab);
	}

	@Override
	public BlockState additionalplacements$getDefaultVanillaState(BlockState currentState) {
		return currentState.is(additionalplacements$asSlab()) ? currentState : additionalplacements$slab.copyProperties(currentState, additionalplacements$asSlab().defaultBlockState());
	}

	@Override
	public BlockState additionalplacements$getDefaultAdditionalState(BlockState currentState) {
		return currentState.is(additionalplacements$slab) ? currentState : additionalplacements$slab.copyProperties(currentState, additionalplacements$slab.defaultBlockState());
	}

	@ModifyReturnValue(method = BlockMethods.GET_STATE_FOR_PLACEMENT_NAME, at = @At("RETURN"))
	private BlockState getStateForPlacement(BlockState original, @Local(argsOnly = true) BlockPlaceContext context) {
		if (this.additionalplacements$hasAdditionalStates() && additionalplacements$enablePlacement(context.getClickedPos(), context.getLevel(), context.getClickedFace(), context.getPlayer())) return additionalplacements$getStateForPlacementImpl(context, original);
		else return original;
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

	@Inject(method = "canBeReplaced", at = @At("HEAD"), cancellable = true)
	private void canBeReplaced(BlockState state, BlockPlaceContext context, CallbackInfoReturnable<Boolean> ci) {
		if (this.additionalplacements$hasAdditionalStates() && additionalplacements$enablePlacement(context.getClickedPos(), context.getLevel(), context.getClickedFace(), context.getPlayer())) ci.setReturnValue(additionalplacements$canBeReplacedImpl(state, context));
	}

	@Override
	public BlockState additionalplacements$updateShapeImpl(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos otherPos, BlockState otherState, RandomSource rand) {
		return super.updateShape(state, level, tickAccess, pos, direction, otherPos, otherState, rand);
	}

	@Override
	public Direction.Axis additionalplacements$getAxis(BlockState state) {
		return Direction.Axis.Y;
	}
}