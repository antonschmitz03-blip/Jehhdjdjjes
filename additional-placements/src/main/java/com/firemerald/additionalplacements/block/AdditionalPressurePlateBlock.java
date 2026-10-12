package com.firemerald.additionalplacements.block;

import com.firemerald.additionalplacements.block.interfaces.IPressurePlateBlock;

import com.firemerald.additionalplacements.client.models.definitions.PressurePlateModels;
import com.firemerald.additionalplacements.client.models.definitions.StateModelDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;

public abstract class AdditionalPressurePlateBlock extends AdditionalBasePressurePlateBlock<PressurePlateBlock> implements IPressurePlateBlock<PressurePlateBlock> {
	public static AdditionalPressurePlateBlock of(PressurePlateBlock plate, ResourceKey<Block> id) {
		return plate instanceof BeaconBeamBlock ? ofBeaconBeam(plate, id) : ofNonBeaconBeam(plate, id);
	}

	@ApiStatus.Internal
		public static AdditionalPressurePlateBlock ofNonBeaconBeam(PressurePlateBlock block, ResourceKey<Block> id) {
		return com.firemerald.additionalplacements.block.fabric.AdditionalPressurePlateBlockImpl.ofNonBeaconBeam(block, id);
	}

	@ApiStatus.Internal
		public static AdditionalPressurePlateBlock ofBeaconBeam(PressurePlateBlock block, ResourceKey<Block> id) {
		return com.firemerald.additionalplacements.block.fabric.AdditionalPressurePlateBlockImpl.ofBeaconBeam(block, id);
	}

	protected AdditionalPressurePlateBlock(PressurePlateBlock plate, ResourceKey<Block> id) {
		super(plate, id);
	}

	@Override
	protected int getSignalStrength(Level level, BlockPos pos) {
		return BasePressurePlateBlock.getEntityCount(level, TOUCH_AABBS[level.getBlockState(pos).getValue(AdditionalFloorBlock.PLACING).ordinal() - 1].move(pos), switch (this.parentBlock.type.pressurePlateSensitivity()) {
			case EVERYTHING -> Entity.class;
			case MOBS -> LivingEntity.class;
		}) > 0 ? 15 : 0;
	}

	@Override
	public StateModelDefinition getModelDefinition(BlockState state) {
		return PressurePlateModels.getPressurePlateModel(state);
	}
}