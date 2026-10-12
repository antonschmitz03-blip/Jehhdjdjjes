package com.firemerald.additionalplacements.config.blocklist;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class IDBlocklistEntry extends BlocklistEntryBase {
    public final Identifier id;

    public IDBlocklistEntry(boolean value, Identifier id) {
        super(value);
        this.id = id;
    }

    @Override
    public boolean contains(Block block, Identifier id) {
        return id.equals(this.id);
    }

    @Override
    public String filterString() {
        return id.toString();
    }
}
