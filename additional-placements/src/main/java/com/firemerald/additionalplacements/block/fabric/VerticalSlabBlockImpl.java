package com.firemerald.additionalplacements.block.fabric;

import com.firemerald.additionalplacements.block.VerticalSlabBlock;
import com.firemerald.additionalplacements.block.interfaces.IAdditionalBeaconBeamBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;

public class VerticalSlabBlockImpl extends VerticalSlabBlock {
    private VerticalSlabBlockImpl(SlabBlock block, ResourceKey<Block> id) {
        super(block, id);
    }

    private static class BeaconBeamVerticalSlabBlockImpl extends VerticalSlabBlockImpl implements IAdditionalBeaconBeamBlock<SlabBlock> {
        BeaconBeamVerticalSlabBlockImpl(SlabBlock block, ResourceKey<Block> id) {
            super(block, id);
        }
    }

    public static VerticalSlabBlock ofNonBeaconBeam(SlabBlock block, ResourceKey<Block> id) {
        return new VerticalSlabBlockImpl(block, id);
    }

    public static VerticalSlabBlock ofBeaconBeam(SlabBlock block, ResourceKey<Block> id) {
        return new BeaconBeamVerticalSlabBlockImpl(block, id);
    }
}
