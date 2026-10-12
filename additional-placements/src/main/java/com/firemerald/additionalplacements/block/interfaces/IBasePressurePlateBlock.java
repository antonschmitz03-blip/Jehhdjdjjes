package com.firemerald.additionalplacements.block.interfaces;

import com.firemerald.additionalplacements.block.AdditionalBasePressurePlateBlock;
import com.firemerald.additionalplacements.block.AdditionalFloorBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IBasePressurePlateBlock<T extends Block> extends IFloorBlock<T> {
	interface IVanillaBasePressurePlateBlock<T extends AdditionalBasePressurePlateBlock<?>> extends IBasePressurePlateBlock<T>, IVanillaBlock<T> {}

	@Override
	default BlockState additionalplacements$forPlacing(Direction dir, BlockState blockState) {
    	if (dir == Direction.DOWN) return additionalplacements$getDefaultVanillaState(blockState);
    	else return additionalplacements$getDefaultAdditionalState(blockState).setValue(AdditionalFloorBlock.PLACING, dir);
	}

	@Override
	@Nullable
	default Direction additionalplacements$getPlacing(BlockState blockState) {
		if (blockState.getBlock() instanceof PressurePlateBlock) return Direction.DOWN;
		else return blockState.getValue(AdditionalFloorBlock.PLACING);
	}
}