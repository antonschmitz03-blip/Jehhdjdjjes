package com.firemerald.additionalplacements.client.block.highlight;

import com.firemerald.additionalplacements.block.interfaces.IPlacementBlock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface IBlockHighlightAction {
    void perform(IPlacementBlock<?> block, Player player, DeltaTracker deltaTracker);
}
