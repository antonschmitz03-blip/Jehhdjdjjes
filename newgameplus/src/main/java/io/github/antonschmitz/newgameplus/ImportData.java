package io.github.antonschmitz.newgameplus;

import net.minecraft.nbt.CompoundTag;

/**
 * A player picked on the Create World screen: the source world's player data (already upgraded to the
 * current version) plus which parts to bring over.
 */
public record ImportData(String sourceName, CompoundTag player, boolean items, boolean xp, boolean enderChest) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("source", sourceName);
        tag.put("player", player.copy());
        tag.putBoolean("items", items);
        tag.putBoolean("xp", xp);
        tag.putBoolean("ender_chest", enderChest);
        return tag;
    }

    public static ImportData fromTag(CompoundTag tag) {
        return new ImportData(tag.getStringOr("source", "?"), tag.getCompoundOrEmpty("player"),
                tag.getBooleanOr("items", true), tag.getBooleanOr("xp", true), tag.getBooleanOr("ender_chest", false));
    }
}
