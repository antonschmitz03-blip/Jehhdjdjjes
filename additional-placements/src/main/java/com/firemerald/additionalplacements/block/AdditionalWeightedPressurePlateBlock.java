package com.firemerald.additionalplacements.block;

import com.firemerald.additionalplacements.block.interfaces.IWeightedPressurePlateBlock;

import com.firemerald.additionalplacements.client.models.definitions.PressurePlateModels;
import com.firemerald.additionalplacements.client.models.definitions.StateModelDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.ApiStatus;

public abstract class AdditionalWeightedPressurePlateBlock extends AdditionalBasePressurePlateBlock<WeightedPressurePlateBlock> implements IWeightedPressurePlateBlock<WeightedPressurePlateBlock> {
	public static AdditionalWeightedPressurePlateBlock of(WeightedPressurePlateBlock plate, ResourceKey<Block> id) {
		return plate instanceof BeaconBeamBlock ? ofBeaconBeam(plate, id) : ofNonBeaconBeam(plate, id);
	}

	@ApiStatus.Internal
		public static AdditionalWeightedPressurePlateBlock ofNonBeaconBeam(WeightedPressurePlateBlock block, ResourceKey<Block> id) {
		return com.firemerald.additionalplacements.block.fabric.AdditionalWeightedPressurePlateBlockImpl.ofNonBeaconBeam(block, id);
	}

	@ApiStatus.Internal
		public static AdditionalWeightedPressurePlateBlock ofBeaconBeam(WeightedPressurePlateBlock block, ResourceKey<Block> id) {
		return com.firemerald.additionalplacements.block.fabric.AdditionalWeightedPressurePlateBlockImpl.ofBeaconBeam(block, id);
	}

	protected AdditionalWeightedPressurePlateBlock(WeightedPressurePlateBlock plate, ResourceKey<Block> id) {
		super(plate, id);
	}

	@Override
	protected int getSignalStrength(Level level, BlockPos pos) {
		AABB aabb = TOUCH_AABBS[level.getBlockState(pos).getValue(AdditionalFloorBlock.PLACING).ordinal() - 1].move(pos);
		int i = Math.min(level.getEntitiesOfClass(Entity.class, aabb).size(), parentBlock.maxWeight);
		if (i > 0) return Mth.ceil(15 * (float) Math.min(parentBlock.maxWeight, i) / parentBlock.maxWeight);
		else return 0;
	}

	@Override
	public StateModelDefinition getModelDefinition(BlockState state) {
		return PressurePlateModels.getWeightedPressurePlateModel(state);
	}
}