package com.firemerald.additionalplacements.config.blocklist;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public record InvalidBlocklistEntry(String key) implements IBlocklistEntry {

    @Override
    public BlocklistResult apply(Block block, Identifier id) {
        return BlocklistResult.DEFAULT;
    }

    @Override
    public @NotNull String toString() {
        return key;
    }
}
