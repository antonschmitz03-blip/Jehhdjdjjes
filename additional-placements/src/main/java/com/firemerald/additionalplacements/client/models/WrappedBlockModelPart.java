package com.firemerald.additionalplacements.client.models;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import org.jspecify.annotations.NonNull;

public interface WrappedBlockModelPart extends BlockStateModelPart {
    BlockStateModelPart getWrapped();

    BlockStateModelPart getVisual();

    @Override
    default boolean useAmbientOcclusion() {
        return getVisual().useAmbientOcclusion();
    }

    @Override
    default Material.@NonNull Baked particleMaterial() {
        return getVisual().particleMaterial();
    }

    @BakedQuad.MaterialFlags
    @Override
    default int materialFlags() {
        return getVisual().materialFlags();
    }
}
