package com.firemerald.additionalplacements.client.models.retextured;

import com.firemerald.additionalplacements.client.models.PlacementModelWrapper;
import com.firemerald.additionalplacements.client.models.Unwrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class BakedRetexturedPlacementModel implements PlacementModelWrapper {
		public static BakedRetexturedPlacementModel of(BlockStateModel ourModel, BlockState theirModelState) {
		return com.firemerald.additionalplacements.client.models.retextured.fabric.BakedRetexturedPlacementModelImpl.of(ourModel, theirModelState);
	}

	private BlockStateModel ourModel;
	private boolean ourModelMissing = false, theirModelMissing = false;
	private final BlockState theirModelState;
	private BlockStateModel theirModel;

	protected BakedRetexturedPlacementModel(BlockStateModel ourModel, BlockState theirModelState) {
		this.ourModel = ourModel;
		this.theirModelState = theirModelState;
	}

	@Override
	public BlockStateModel getWrappedModel() {
		if (ourModel == null) {
			ourModel = Unwrapper.unwrap(Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel());
			ourModelMissing = true;
		}
		return ourModel;
	}

	@Override
	public BlockStateModel getVisualModel() {
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
		return ourModelMissing || theirModelMissing;
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
		BlockStateModel ourModel = getWrappedModel();
		BlockStateModel theirModel = getVisualModel();
		if (wasModelMissing()) ourModel.collectParts(random, output);
		else {
			List<BlockStateModelPart> theirParts = new ArrayList<>();
			theirModel.collectParts(random, theirParts);
			if (!theirParts.isEmpty()) {
				List<BlockStateModelPart> wrappedParts = new ArrayList<>();
				ourModel.collectParts(random, wrappedParts);
				wrappedParts.forEach(ourPart -> output.add(RetexturedBlockModelPart.of(ourPart, theirParts)));
			}
		}
	}
}
