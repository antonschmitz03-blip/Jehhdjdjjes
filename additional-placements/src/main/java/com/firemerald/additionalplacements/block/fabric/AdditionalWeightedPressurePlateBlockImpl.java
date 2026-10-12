package com.firemerald.additionalplacements.block.fabric;

import com.firemerald.additionalplacements.block.AdditionalWeightedPressurePlateBlock;
import com.firemerald.additionalplacements.block.interfaces.IAdditionalBeaconBeamBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;

public class AdditionalWeightedPressurePlateBlockImpl extends AdditionalWeightedPressurePlateBlock {
    private AdditionalWeightedPressurePlateBlockImpl(WeightedPressurePlateBlock block, ResourceKey<Block> id) {
        super(block, id);
    }

    private static class BeaconBeamAdditionalWeightedPressurePlateBlockImpl extends AdditionalWeightedPressurePlateBlockImpl implements IAdditionalBeaconBeamBlock<WeightedPressurePlateBlock> {
        BeaconBeamAdditionalWeightedPressurePlateBlockImpl(WeightedPressurePlateBlock block, ResourceKey<Block> id) {
            super(block, id);
        }
    }

    public static AdditionalWeightedPressurePlateBlock ofNonBeaconBeam(WeightedPressurePlateBlock block, ResourceKey<Block> id) {
        return new AdditionalWeightedPressurePlateBlockImpl(block, id);
    }

    public static AdditionalWeightedPressurePlateBlock ofBeaconBeam(WeightedPressurePlateBlock block, ResourceKey<Block> id) {
        return new BeaconBeamAdditionalWeightedPressurePlateBlockImpl(block, id);
    }
}
