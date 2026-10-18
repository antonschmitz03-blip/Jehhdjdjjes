package io.github.antonschmitz.diamondmachines;

import net.minecraft.world.level.block.Block;

/**
 * A diamond machine that should count as the Create block it upgrades. Create checks for its machines with
 * {@code state.is(AllBlocks.X)} in many places (crushing wheel pairing, contraption attachment, mechanical arms),
 * so {@link io.github.antonschmitz.diamondmachines.mixin.BlockStateBaseMixin} makes those checks also accept us.
 */
public interface DiamondVariant {
    Block baseBlock();
}
