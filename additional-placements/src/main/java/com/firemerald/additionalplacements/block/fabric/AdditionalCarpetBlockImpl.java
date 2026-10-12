package com.firemerald.additionalplacements.block.fabric;

import com.firemerald.additionalplacements.block.AdditionalCarpetBlock;
import com.firemerald.additionalplacements.block.interfaces.IAdditionalBeaconBeamBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;

public class AdditionalCarpetBlockImpl extends AdditionalCarpetBlock {
    private AdditionalCarpetBlockImpl(CarpetBlock block, ResourceKey<Block> id) {
        super(block, id);
    }

    private static class BeaconBeamAdditionalCarpetBlockImpl extends AdditionalCarpetBlockImpl implements IAdditionalBeaconBeamBlock<CarpetBlock> {
        BeaconBeamAdditionalCarpetBlockImpl(CarpetBlock block, ResourceKey<Block> id) {
            super(block, id);
        }
    }

    public static AdditionalCarpetBlock ofNonBeaconBeam(CarpetBlock block, ResourceKey<Block> id) {
        return new AdditionalCarpetBlockImpl(block, id);
    }

    public static AdditionalCarpetBlock ofBeaconBeam(CarpetBlock block, ResourceKey<Block> id) {
        return new BeaconBeamAdditionalCarpetBlockImpl(block, id);
    }
}
