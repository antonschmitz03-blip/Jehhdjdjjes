package com.firemerald.additionalplacements.client.models.rotated.fabric;

import com.firemerald.additionalplacements.client.fabric.WrappedBlockModelPartImpl;
import com.firemerald.additionalplacements.client.models.rotated.RotatedBlockModelPart;
import com.firemerald.additionalplacements.util.BlockRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;

public class RotatedBlockModelPartImpl extends RotatedBlockModelPart implements WrappedBlockModelPartImpl {
    public static RotatedBlockModelPart of(BlockStateModelPart wrapped, BlockRotation modelRotation, boolean rotatesTexture) {
        return new RotatedBlockModelPartImpl(wrapped, modelRotation, rotatesTexture);
    }

    public RotatedBlockModelPartImpl(BlockStateModelPart wrapped, BlockRotation modelRotation, boolean rotatesTexture) {
        super(wrapped, modelRotation, rotatesTexture);
    }
}
