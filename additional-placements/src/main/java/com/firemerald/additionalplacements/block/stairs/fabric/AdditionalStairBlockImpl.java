package com.firemerald.additionalplacements.block.stairs.fabric;

import com.firemerald.additionalplacements.block.interfaces.IAdditionalBeaconBeamBlock;
import com.firemerald.additionalplacements.block.stairs.AdditionalStairBlock;
import com.firemerald.additionalplacements.block.stairs.StairConnectionsType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;

public class AdditionalStairBlockImpl extends AdditionalStairBlock {
    private AdditionalStairBlockImpl(StairBlock block, ResourceKey<Block> id, StairConnectionsType connectionsType) {
        super(block, id, connectionsType);
    }

    private static class BeaconBeamAdditionalCarpetBlockImpl extends AdditionalStairBlockImpl implements IAdditionalBeaconBeamBlock<StairBlock> {
        BeaconBeamAdditionalCarpetBlockImpl(StairBlock block, ResourceKey<Block> id, StairConnectionsType connectionsType) {
            super(block, id, connectionsType);
        }
    }

    public static AdditionalStairBlock ofNonBeaconBeam(StairBlock block, ResourceKey<Block> id, StairConnectionsType connectionsType) {
        return new AdditionalStairBlockImpl(block, id, connectionsType);
    }

    public static AdditionalStairBlock ofBeaconBeam(StairBlock block, ResourceKey<Block> id, StairConnectionsType connectionsType) {
        return new BeaconBeamAdditionalCarpetBlockImpl(block, id, connectionsType);
    }
}
