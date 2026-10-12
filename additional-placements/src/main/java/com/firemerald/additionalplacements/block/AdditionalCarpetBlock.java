package com.firemerald.additionalplacements.block;

import com.firemerald.additionalplacements.block.interfaces.ICarpetBlock;

import com.firemerald.additionalplacements.client.models.definitions.CarpetModels;
import com.firemerald.additionalplacements.client.models.definitions.StateModelDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus;

public abstract class AdditionalCarpetBlock extends AdditionalFloorBlock<CarpetBlock> implements ICarpetBlock<CarpetBlock> {
	public static final VoxelShape[] SHAPES = {
			Block.box(0, 15, 0, 16, 16, 16),
			Block.box(0, 0, 0, 16, 16, 1),
			Block.box(0, 0, 15, 16, 16, 16),
			Block.box(0, 0, 0, 1, 16, 16),
			Block.box(15, 0, 0, 16, 16, 16)
	};

	public static AdditionalCarpetBlock of(CarpetBlock carpet, ResourceKey<Block> id) {
		return carpet instanceof BeaconBeamBlock ? ofBeaconBeam(carpet, id) : ofNonBeaconBeam(carpet, id);
	}

	@ApiStatus.Internal
		public static AdditionalCarpetBlock ofNonBeaconBeam(CarpetBlock block, ResourceKey<Block> id) {
		return com.firemerald.additionalplacements.block.fabric.AdditionalCarpetBlockImpl.ofNonBeaconBeam(block, id);
	}

	@ApiStatus.Internal
		public static AdditionalCarpetBlock ofBeaconBeam(CarpetBlock block, ResourceKey<Block> id) {
		return com.firemerald.additionalplacements.block.fabric.AdditionalCarpetBlockImpl.ofBeaconBeam(block, id);
	}

	protected AdditionalCarpetBlock(CarpetBlock carpet, ResourceKey<Block> id) {
		super(carpet, id);
		this.registerDefaultState(copyProperties(getOtherBlockState(), this.stateDefinition.any()).setValue(PLACING, Direction.NORTH));
		((IVanillaCarpetBlock) carpet).additionalplacements$setOtherBlock(this);
	}

	@Override
	public VoxelShape getShapeInternal(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(PLACING).ordinal() - 1];
	}

	@Override
	public String getTagTypeName() {
		return "carpet";
	}

	@Override
	public String getTagTypeNamePlural() {
		return "carpets";
	}

	@Override
	public BlockState additionalplacements$updateShapeImpl(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos otherPos, BlockState otherState, RandomSource rand) {
		return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return !level.isEmptyBlock(pos.relative(state.getValue(PLACING)));
	}

	@Override
	public Identifier getBaseModelPrefix() {
		return CarpetModels.BASE_MODEL_FOLDER;
	}

	@Override
	public StateModelDefinition getModelDefinition(BlockState state) {
		return CarpetModels.getModel(state);
	}

	@Override
	public String[] getAllModels() {
		return CarpetModels.MODELS;
	}
}
