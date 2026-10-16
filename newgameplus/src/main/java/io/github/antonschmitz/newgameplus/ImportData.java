package io.github.antonschmitz.newgameplus;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * A player picked on the Create World screen: the source world's player data (already upgraded to the
 * current version), which parts to bring over, and the tamed mobs found in that world.
 */
public record ImportData(String sourceName, CompoundTag player, boolean items, boolean xp, boolean enderChest,
                         boolean mobsEnabled, List<CompoundTag> mobs) {
    /** Mobs that actually come along (none when the option is off). */
    public List<CompoundTag> mobsToImport() {
        return mobsEnabled ? mobs : List.of();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("source", sourceName);
        tag.put("player", player.copy());
        tag.putBoolean("items", items);
        tag.putBoolean("xp", xp);
        tag.putBoolean("ender_chest", enderChest);
        tag.putBoolean("mobs_enabled", mobsEnabled);
        ListTag list = new ListTag();
        for (CompoundTag mob : mobsToImport()) {
            list.add(mob.copy());
        }
        tag.put("mobs", list);
        return tag;
    }

    public static ImportData fromTag(CompoundTag tag) {
        List<CompoundTag> mobs = new ArrayList<>();
        ListTag list = tag.getListOrEmpty("mobs");
        for (int i = 0; i < list.size(); i++) {
            list.getCompound(i).ifPresent(mobs::add);
        }
        return new ImportData(tag.getStringOr("source", "?"), tag.getCompoundOrEmpty("player"),
                tag.getBooleanOr("items", true), tag.getBooleanOr("xp", true), tag.getBooleanOr("ender_chest", false),
                tag.getBooleanOr("mobs_enabled", false), mobs);
    }
}
