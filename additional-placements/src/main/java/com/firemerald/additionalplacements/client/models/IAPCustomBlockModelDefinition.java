package com.firemerald.additionalplacements.client.models;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.Map;
import java.util.function.Supplier;

public interface IAPCustomBlockModelDefinition {
    Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(StateDefinition<Block, BlockState> states, Supplier<String> sourceSupplier);

    Identifier id();

    MapCodec<? extends IAPCustomBlockModelDefinition> codec();
}
