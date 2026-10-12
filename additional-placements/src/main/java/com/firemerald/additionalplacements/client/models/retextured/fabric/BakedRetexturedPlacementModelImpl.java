package com.firemerald.additionalplacements.client.models.retextured.fabric;

import com.firemerald.additionalplacements.client.models.fabric.PlacementModelWrapperImpl;
import com.firemerald.additionalplacements.client.models.retextured.BakedRetexturedPlacementModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BakedRetexturedPlacementModelImpl extends BakedRetexturedPlacementModel implements PlacementModelWrapperImpl {
	public static BakedRetexturedPlacementModel of(BlockStateModel ourModel, BlockState theirModelState) {
		return new BakedRetexturedPlacementModelImpl(ourModel, theirModelState);
	}

	protected BakedRetexturedPlacementModelImpl(BlockStateModel ourModel, BlockState theirModelState) {
		super(ourModel, theirModelState);
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return this;
	}
}
