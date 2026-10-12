package com.firemerald.additionalplacements.block.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public interface IBasePressurePlateBlockExtensions {
	 void additionalplacements$playOnSoundPublic(LevelAccessor level, BlockPos pos);

	 void additionalplacements$playOffSoundPublic(LevelAccessor level, BlockPos pos);

	 int additionalplacements$getSignalForStatePublic(BlockState state);

	 BlockState additionalplacements$setSignalForStatePublic(BlockState state, int strength);

	 int additionalplacements$getPressedTimePublic();
}