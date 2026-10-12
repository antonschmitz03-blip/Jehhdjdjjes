package com.firemerald.additionalplacements.client.models.definitions;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.block.VerticalSlabBlock;

import com.mojang.math.Quadrant;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

public class SlabModels {
	public static final Identifier BASE_MODEL_FOLDER = AdditionalPlacementsMod.rl("block/slabs/base");
	public static final Identifier COLUMN_MODEL_FOLDER = AdditionalPlacementsMod.rl("block/slabs/column");
	public static final Identifier SIDE_ALL_MODEL_FOLDER = AdditionalPlacementsMod.rl("block/slabs/side_all");

	public static final String[] MODELS = new String[] {
			"/negative",
			"/positive",
			"/double"
	};

	public static final StateModelDefinition[][] MODEL_DEFINITIONS = new StateModelDefinition[2][3];

	static {
		MODEL_DEFINITIONS[0][SlabType.BOTTOM.ordinal()] = new StateModelDefinition("/negative", Quadrant.R270);
		MODEL_DEFINITIONS[0][SlabType.TOP.ordinal()] = new StateModelDefinition("/positive", Quadrant.R270);
		MODEL_DEFINITIONS[0][SlabType.DOUBLE.ordinal()] = new StateModelDefinition("/double", Quadrant.R270);
		MODEL_DEFINITIONS[1][SlabType.BOTTOM.ordinal()] = new StateModelDefinition("/negative", Quadrant.R0);
		MODEL_DEFINITIONS[1][SlabType.TOP.ordinal()] = new StateModelDefinition("/positive", Quadrant.R0);
		MODEL_DEFINITIONS[1][SlabType.DOUBLE.ordinal()] = new StateModelDefinition("/double", Quadrant.R0);
	}

	public static StateModelDefinition getModel(Axis axis, SlabType type) {
		return MODEL_DEFINITIONS[axis == Axis.X ? 0 : 1][type.ordinal()];
	}

	public static StateModelDefinition getModel(BlockState state) {
		return getModel(state.getValue(VerticalSlabBlock.AXIS), state.getValue(SlabBlock.TYPE));
	}
}
