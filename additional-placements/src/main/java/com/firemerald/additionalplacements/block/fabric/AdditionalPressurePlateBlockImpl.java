package com.firemerald.additionalplacements.block.fabric;

import com.firemerald.additionalplacements.block.AdditionalPressurePlateBlock;
import com.firemerald.additionalplacements.block.interfaces.IAdditionalBeaconBeamBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;

public class AdditionalPressurePlateBlockImpl extends AdditionalPressurePlateBlock {
    private AdditionalPressurePlateBlockImpl(PressurePlateBlock block, ResourceKey<Block> id) {
        super(block, id);
    }

    private static class BeaconBeamAdditionalPressurePlateBlockImpl extends AdditionalPressurePlateBlockImpl implements IAdditionalBeaconBeamBlock<PressurePlateBlock> {
        BeaconBeamAdditionalPressurePlateBlockImpl(PressurePlateBlock block, ResourceKey<Block> id) {
            super(block, id);
        }
    }

    public static AdditionalPressurePlateBlock ofNonBeaconBeam(PressurePlateBlock block, ResourceKey<Block> id) {
        return new AdditionalPressurePlateBlockImpl(block, id);
    }

    public static AdditionalPressurePlateBlock ofBeaconBeam(PressurePlateBlock block, ResourceKey<Block> id) {
        return new BeaconBeamAdditionalPressurePlateBlockImpl(block, id);
    }
}
