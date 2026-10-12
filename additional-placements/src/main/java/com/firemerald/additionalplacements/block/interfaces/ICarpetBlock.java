package com.firemerald.additionalplacements.block.interfaces;

import com.firemerald.additionalplacements.block.AdditionalCarpetBlock;
import com.firemerald.additionalplacements.block.AdditionalFloorBlock;
import com.firemerald.additionalplacements.generation.APGenerationTypes;
import com.firemerald.additionalplacements.generation.GenerationType;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface ICarpetBlock<T extends Block> extends IFloorBlock<T> {
	interface IVanillaCarpetBlock extends ICarpetBlock<AdditionalCarpetBlock>, IVanillaBlock<AdditionalCarpetBlock> {}

	@Override
	default BlockState additionalplacements$forPlacing(Direction dir, BlockState blockState) {
    	if (dir == Direction.DOWN) return additionalplacements$getDefaultVanillaState(blockState);
    	else return additionalplacements$getDefaultAdditionalState(blockState).setValue(AdditionalFloorBlock.PLACING, dir);
	}

	@Override
	@Nullable
	default Direction additionalplacements$getPlacing(BlockState blockState) {
		if (blockState.getBlock() instanceof CarpetBlock) return Direction.DOWN;
		else return blockState.getValue(AdditionalFloorBlock.PLACING);
	}

	@Override
	default GenerationType<?, ?> additionalplacements$getGenerationType() {
		return APGenerationTypes.carpet();
	}
}