package com.firemerald.additionalplacements.client.models.fabric;

import com.firemerald.additionalplacements.client.models.BlockModelUtils;
import com.firemerald.additionalplacements.client.models.PlacementModelWrapper;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface PlacementModelWrapperImpl extends PlacementModelWrapper, FabricBlockStateModel {
    @Override
    @Nullable
    Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random);

    @Override
    default Material.Baked particleMaterial(BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        return getVisualModel().particleMaterial(blockView, pos, BlockModelUtils.getModeledState(state));
    }
}
