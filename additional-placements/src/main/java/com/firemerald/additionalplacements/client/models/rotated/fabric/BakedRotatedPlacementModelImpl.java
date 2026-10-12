package com.firemerald.additionalplacements.client.models.rotated.fabric;

import com.firemerald.additionalplacements.client.models.fabric.PlacementModelWrapperImpl;
import com.firemerald.additionalplacements.client.models.rotated.BakedRotatedPlacementModel;
import com.firemerald.additionalplacements.util.BlockRotation;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BakedRotatedPlacementModelImpl extends BakedRotatedPlacementModel implements PlacementModelWrapperImpl {
	public static BakedRotatedPlacementModel of(BlockState theirModelState, BlockRotation modelRotation, boolean rotatesTexture) {
		return new BakedRotatedPlacementModelImpl(theirModelState, modelRotation, rotatesTexture);
	}

	protected BakedRotatedPlacementModelImpl(BlockState theirModelState, BlockRotation modelRotation, boolean rotatesTexture) {
		super(theirModelState, modelRotation, rotatesTexture);
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return this;
	}
}
