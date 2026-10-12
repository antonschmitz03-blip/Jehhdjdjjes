package com.firemerald.additionalplacements.client.models.retextured;

import com.firemerald.additionalplacements.client.models.BlockModelUtils;
import com.firemerald.additionalplacements.client.models.WrappedBlockModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class RetexturedBlockModelPart implements WrappedBlockModelPart {
        public static RetexturedBlockModelPart of(BlockStateModelPart wrapped, List<BlockStateModelPart> originalModel) {
		return com.firemerald.additionalplacements.client.models.retextured.fabric.RetexturedBlockModelPartImpl.of(wrapped, originalModel);
	}

    public final BlockStateModelPart wrapped;
    public final List<BlockStateModelPart> originalModel;

    protected RetexturedBlockModelPart(BlockStateModelPart wrapped, List<BlockStateModelPart> originalModel) {
        this.wrapped = wrapped;
        this.originalModel = originalModel;
    }

    @Override
    public BlockStateModelPart getWrapped() {
        return wrapped;
    }

    @Override
    public BlockStateModelPart getVisual() {
        return originalModel.getFirst();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable Direction direction) {
        return BlockModelUtils.retexturedQuads(originalModel, wrapped, direction);
    }
}
