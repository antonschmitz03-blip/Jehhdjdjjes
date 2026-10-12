package com.firemerald.additionalplacements.block.stairs;

import java.util.Optional;
import java.util.function.Consumer;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.block.AdditionalPlacementLiquidBlock;
import com.firemerald.additionalplacements.block.interfaces.ISimpleRotationBlock;
import com.firemerald.additionalplacements.block.interfaces.IStairBlock;
import com.firemerald.additionalplacements.block.interfaces.IStateFixer;
import com.firemerald.additionalplacements.block.stairs.common.CommonStairShapeState;
import com.firemerald.additionalplacements.block.stairs.v1.V1StairPlacing;
import com.firemerald.additionalplacements.block.stairs.v1.V1StairShape;
import com.firemerald.additionalplacements.block.stairs.v1.V1StairShapeState;
import com.firemerald.additionalplacements.block.stairs.v2.V2StairFacing;
import com.firemerald.additionalplacements.block.stairs.v2.V2StairShape;
import com.firemerald.additionalplacements.block.stairs.v2.V2StairShapeState;
import com.firemerald.additionalplacements.block.stairs.vanilla.VanillaStairShapeState;
import com.firemerald.additionalplacements.client.models.definitions.StairModels;
import com.firemerald.additionalplacements.client.models.definitions.StateModelDefinition;
import com.firemerald.additionalplacements.config.APConfigs;
import com.firemerald.additionalplacements.util.BlockRotation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public abstract class AdditionalStairBlock extends AdditionalPlacementLiquidBlock<StairBlock> implements IStairBlock<StairBlock>, ISimpleRotationBlock, IStateFixer {
	private static StairConnectionsType connectionsTypeStatic;

	public static AdditionalStairBlock of(StairBlock stairs, ResourceKey<Block> id, StairConnectionsType connectionsType) {
		connectionsTypeStatic = connectionsType;
		AdditionalStairBlock ret = stairs instanceof BeaconBeamBlock ? ofBeaconBeam(stairs, id, connectionsType) : ofNonBeaconBeam(stairs, id, connectionsType);
		connectionsTypeStatic = null;
		return ret;
	}

	@ApiStatus.Internal
		public static AdditionalStairBlock ofNonBeaconBeam(StairBlock block, ResourceKey<Block> id, StairConnectionsType connectionsType) {
		return com.firemerald.additionalplacements.block.stairs.fabric.AdditionalStairBlockImpl.ofNonBeaconBeam(block, id, connectionsType);
	}

	@ApiStatus.Internal
		public static AdditionalStairBlock ofBeaconBeam(StairBlock block, ResourceKey<Block> id, StairConnectionsType connectionsType) {
		return com.firemerald.additionalplacements.block.stairs.fabric.AdditionalStairBlockImpl.ofBeaconBeam(block, id, connectionsType);
	}

	public final StairConnectionsType connectionsType;
	public boolean rotateLogic = false, rotateModel = false, rotateTex = false;

	protected AdditionalStairBlock(StairBlock stairs, ResourceKey<Block> id, StairConnectionsType connectionsType) {
		super(stairs, id);
		this.connectionsType = connectionsType;
		this.registerDefaultState(copyProperties(getOtherBlockState(), this.stateDefinition.any()).setValue(connectionsType, connectionsType.defaultShapeState));
		((IVanillaStairBlock) stairs).additionalplacements$setOtherBlock(this);
	}

	@Override
	public boolean isValidProperty(Property<?> prop) {
		return prop != StairBlock.FACING && prop != StairBlock.HALF && prop != StairBlock.SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
		builder.add(connectionsTypeStatic);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState additionalplacements$getDefaultVanillaState(BlockState currentState) {
		return currentState.is(parentBlock) ? currentState : copyProperties(currentState, parentBlock.defaultBlockState());
	}

	@Override
	public BlockState additionalplacements$getDefaultAdditionalState(BlockState currentState) {
		return currentState.is(this) ? currentState : copyProperties(currentState, this.defaultBlockState());
	}

	@Override
	public String getTagTypeName() {
		return "stair";
	}

	@Override
	public String getTagTypeNamePlural() {
		return "stairs";
	}

	@Override
	public BlockState withUnrotatedPlacement(BlockState worldState, BlockState modelState) {
		VanillaStairShapeState modelShapeState = additionalplacements$getShapeState(worldState).model();
		return modelState
				.setValue(StairBlock.FACING, modelShapeState.facing)
				.setValue(StairBlock.HALF, modelShapeState.half)
				.setValue(StairBlock.SHAPE, modelShapeState.shape);
	}

	public boolean canRotate(BlockState state) {
		return this.additionalplacements$getShapeState(state).isRotatedModel();
	}

	@Override
	public BlockRotation getRotation(BlockState state) {
		return this.additionalplacements$getShapeState(state).modelRotation();
	}

	@Override
	public boolean rotatesLogic(BlockState state) {
		return rotateLogic && canRotate(state);
	}

	@Override
	public boolean rotatesTexture(BlockState state) {
		return rotateTex && canRotate(state);
	}

	@Override
	public boolean rotatesModel(BlockState state) {
		return rotateModel && canRotate(state);
	}

	@Override
	public void setLogicRotation(boolean useLogicRotation) {
		this.rotateLogic = useLogicRotation;
	}

	@Override
	public void setModelRotation(boolean useTexRotation, boolean useModelRotation) {
		this.rotateTex = useTexRotation;
		this.rotateModel = useModelRotation;
	}

	@Override
	public VoxelShape getShapeInternal(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		CommonStairShapeState shapeState = this.additionalplacements$getShapeState(state);
		return shapeState.shape.getVoxelShape(shapeState.facing);
	}

	@Override
	public Identifier getBaseModelPrefix() {
		return StairModels.BASE_MODEL_FOLDER;
	}

	@Override
	public StateModelDefinition getModelDefinition(BlockState state) {
		return StairModels.getModelDefinition(additionalplacements$getShapeState(state));
	}

	@Override
	public String[] getAllModels() {
		return StairModels.MODELS;
	}

	@Override
	public CompoundTag fix(CompoundTag properties, Consumer<Block> changeBlock) {
		CommonStairShapeState commonShape = getOldPropertyShapeState(properties);
		if (commonShape != null) applyState(commonShape, properties, changeBlock);
		return properties;
	}

	public CommonStairShapeState getOldPropertyShapeState(CompoundTag properties) {
		if (APConfigs.common().fixStates.get()) {
			if (IStateFixer.contains(properties, connectionsType)) {
                AdditionalPlacementsMod.LOGGER.debug("{} Potentially fixing V3 stair block state: {}", this, properties);
				@SuppressWarnings("OptionalGetWithoutIsPresent")
				String shapeStateName = IStateFixer.getPropertyString(properties, connectionsType).get();
				if (!connectionsType.isValid(shapeStateName)) {
                    AdditionalPlacementsMod.LOGGER.debug("{} Fixing V3 stair block state", this);
					return CommonStairShapeState.get(shapeStateName);
				}
			} else if (APConfigs.common().fixOldStates.get()) {
				if (properties.contains("shape")) {
					if (properties.contains("facing")) { //potentially V2
                        AdditionalPlacementsMod.LOGGER.debug("{} Potentially fixing potential V2 stair block state: {}", this, properties);
						Optional<V2StairFacing> facing = properties.getString("facing").map(V2StairFacing::get);
						Optional<V2StairShape> shape = properties.getString("shape").map(V2StairShape::get);
						if (facing.isPresent() && shape.isPresent()) { //V2
                            AdditionalPlacementsMod.LOGGER.debug("{} Fixing V2 stair block state", this);
							properties.remove("facing");
							properties.remove("shape");
							return V2StairShapeState.toCommon(facing.get(), shape.get());
						}
					} else if (properties.contains("placing")) { //potentially V1
                        AdditionalPlacementsMod.LOGGER.debug("{} Potentially fixing potential V1 stair block state: {}", this, properties);
						Optional<V1StairPlacing> placing = properties.getString("placing").map(V1StairPlacing::get);
						Optional<V1StairShape> shape = properties.getString("shape").map(V1StairShape::get);
						if (placing.isPresent() && shape.isPresent()) { //V1
                            AdditionalPlacementsMod.LOGGER.debug("{} Fixing V1 stair block state", this);
							properties.remove("placing");
							properties.remove("shape");
							return V1StairShapeState.toCommon(placing.get(), shape.get());
						}
					}
				}
			}
		}
		return null;
	}

	public void applyState(CommonStairShapeState commonShapeState, CompoundTag properties, Consumer<Block> changeBlock) {
		if (!connectionsType.allowFlipped && commonShapeState.isComplexFlipped) commonShapeState = commonShapeState.flipped();
		VanillaStairShapeState vanillaShapeState = commonShapeState.vanilla();
		if (vanillaShapeState == null && !connectionsType.isValid(commonShapeState)) { //not valid
			commonShapeState = commonShapeState.closestVanillaShape;
			vanillaShapeState = commonShapeState.vanilla();
		}
		if (vanillaShapeState != null) { //make vanilla
			changeBlock.accept(additionalplacements$getOtherBlock());
			IStateFixer.setProperty(properties, StairBlock.FACING, vanillaShapeState.facing);
			IStateFixer.setProperty(properties, StairBlock.HALF, vanillaShapeState.half);
			IStateFixer.setProperty(properties, StairBlock.SHAPE, vanillaShapeState.shape);
		} else {
			IStateFixer.setProperty(properties, connectionsType, commonShapeState);
		}
	}

	@Override
	public BlockState additionalplacements$getBlockStateInternal(CommonStairShapeState commonShapeState, BlockState currentState) {
		if (!connectionsType.allowFlipped && commonShapeState.isComplexFlipped) return additionalplacements$getBlockState(commonShapeState.flipped(), currentState);
		else {
			if (!connectionsType.isValid(commonShapeState)) return additionalplacements$getBlockState(commonShapeState.closestVanillaShape, currentState);
			return additionalplacements$getDefaultAdditionalState(currentState).setValue(connectionsType, commonShapeState);
		}
	}

	@Override
	public CommonStairShapeState additionalplacements$getShapeState(BlockState blockState) {
		return blockState.getValue(connectionsType);
	}

	@Override
	public StairConnectionsType additionalplacements$connectionsType() {
		return connectionsType;
	}
}
