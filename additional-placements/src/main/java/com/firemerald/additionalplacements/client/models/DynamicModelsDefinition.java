package com.firemerald.additionalplacements.client.models;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class DynamicModelsDefinition implements IAPCustomBlockModelDefinition {
    public static final Identifier ID = AdditionalPlacementsMod.rl("dynamic_models");
    public static final MapCodec<DynamicModelsDefinition> CODEC = MapCodec.unit(DynamicModelsDefinition::of);

        public static DynamicModelsDefinition of() {
		return com.firemerald.additionalplacements.client.models.fabric.DynamicModelsDefinitionImpl.of();
	}

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(StateDefinition<Block, BlockState> states, Supplier<String> sourceSupplier) {
        if (states.getOwner() instanceof AdditionalPlacementBlock<?> block) {
            UnbakedDynamicModel unbakedRoot = new UnbakedDynamicModel(block);
            return states.getPossibleStates().stream().collect(Collectors.toMap(
                    Function.identity(),
                    state -> unbakedRoot
            ));
        } else return Map.of();
    }
}
