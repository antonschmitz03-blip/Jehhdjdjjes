package com.firemerald.additionalplacements.client.models.rotated;

import com.firemerald.additionalplacements.client.models.PlacementModelWrapper;
import com.firemerald.additionalplacements.client.models.Unwrapper;
import com.firemerald.additionalplacements.util.BlockRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class BakedRotatedPlacementModel implements PlacementModelWrapper {
		public static BakedRotatedPlacementModel of(BlockState theirModelState, BlockRotation modelRotation, boolean rotatesTexture) {
		return com.firemerald.additionalplacements.client.models.rotated.fabric.BakedRotatedPlacementModelImpl.of(theirModelState, modelRotation, rotatesTexture);
	}

	public final BlockState theirModelState;
	private BlockStateModel theirModel;
	private boolean theirModelMissing = false;
	public final BlockRotation modelRotation;
	public final boolean rotatesTexture;

	protected BakedRotatedPlacementModel(BlockState theirModelState, BlockRotation modelRotation, boolean rotatesTexture) {
		this.theirModelState = theirModelState;
		this.modelRotation = modelRotation;
		this.rotatesTexture = rotatesTexture;
	}

	@Override
	public BlockStateModel getWrappedModel() {
		if (theirModel == null) {
			BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
			BlockStateModel model = modelSet.get(theirModelState);
			if (model == null) {
				model = modelSet.missingModel();
				theirModelMissing = true;
			}
			theirModel = Unwrapper.unwrap(model);
		}
		return theirModel;
	}

	public boolean wasModelMissing() {
		return theirModelMissing;
	}

	@Override
	public BlockStateModel getVisualModel() {
		return getWrappedModel();
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
		BlockStateModel theirModel = getWrappedModel();
		if (wasModelMissing()) theirModel.collectParts(random, output);
		else {
			List<BlockStateModelPart> theirParts = new ArrayList<>();
			theirModel.collectParts(random, theirParts);
			theirParts.forEach(theirPart -> output.add(RotatedBlockModelPart.of(theirPart, modelRotation, rotatesTexture)));
		}
	}
}
