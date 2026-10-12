package com.firemerald.additionalplacements.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AdditionalPlacementLiquidBlock<T extends Block & BucketPickup & LiquidBlockContainer> extends AdditionalPlacementBlock<T> implements BucketPickup, LiquidBlockContainer {
	public AdditionalPlacementLiquidBlock(T parentBlock, ResourceKey<Block> id) {
		super(parentBlock, id);
	}

	@Override
	public @NotNull ItemStack pickupBlock(@Nullable LivingEntity owner, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState blockState) {
		ItemStack ret = this.additionalplacements$getOtherBlock().pickupBlock(owner, level, pos, this.getModelState(blockState));
		level.setBlock(pos, this.copyProperties(level.getBlockState(pos), blockState), 3);
		return ret;
	}

	@Override
	public @NotNull Optional<SoundEvent> getPickupSound() {
		return this.additionalplacements$getOtherBlock().getPickupSound();
	}

	@Override
	public boolean canPlaceLiquid(@Nullable LivingEntity owner, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState blockState, @NotNull Fluid fluid) {
		return this.additionalplacements$getOtherBlock().canPlaceLiquid(owner, level, pos, getModelState(blockState), fluid);
	}

	@Override
	public boolean placeLiquid(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState blockState, @NotNull FluidState fluidState) {
		boolean flag = this.additionalplacements$getOtherBlock().placeLiquid(level, pos, getModelState(blockState), fluidState);
		level.setBlock(pos, this.copyProperties(level.getBlockState(pos), blockState), 3);
		return flag;
	}

	@Override
	public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull LevelReader level, @NotNull ScheduledTickAccess tickAccess, @NotNull BlockPos pos, @NotNull Direction direction, @NotNull BlockPos otherPos, @NotNull BlockState otherState, @NotNull RandomSource rand) {
		FluidState fluid = level.getFluidState(pos);
		if (!fluid.isEmpty()) tickAccess.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
		return super.updateShape(state, level, tickAccess, pos, direction, otherPos, otherState, rand);
	}
}
