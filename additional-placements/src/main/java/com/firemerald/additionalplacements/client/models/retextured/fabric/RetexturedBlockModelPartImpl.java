package com.firemerald.additionalplacements.client.models.retextured.fabric;

import com.firemerald.additionalplacements.client.fabric.WrappedBlockModelPartImpl;
import com.firemerald.additionalplacements.client.models.retextured.RetexturedBlockModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;

import java.util.List;

public class RetexturedBlockModelPartImpl extends RetexturedBlockModelPart implements WrappedBlockModelPartImpl {
    public static RetexturedBlockModelPart of(BlockStateModelPart wrapped, List<BlockStateModelPart> originalModel) {
        return new RetexturedBlockModelPartImpl(wrapped, originalModel);
    }

    protected RetexturedBlockModelPartImpl(BlockStateModelPart wrapped, List<BlockStateModelPart> originalModel) {
        super(wrapped, originalModel);
    }
}
