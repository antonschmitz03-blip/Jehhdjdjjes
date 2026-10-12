package com.firemerald.additionalplacements.client.models;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import org.jetbrains.annotations.NotNull;

public interface PlacementModelWrapper extends BlockStateModel {
    BlockStateModel getWrappedModel();

    BlockStateModel getVisualModel();

    @Override
    default @NotNull Material.Baked particleMaterial() {
        return getVisualModel().particleMaterial();
    }

    @Override
    default @BakedQuad.MaterialFlags int materialFlags() {
        return getVisualModel().materialFlags();
    }
}
