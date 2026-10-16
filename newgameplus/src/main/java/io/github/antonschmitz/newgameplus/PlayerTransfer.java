package io.github.antonschmitz.newgameplus;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;

/** Decides which parts of a saved player record come along into the new world. */
public final class PlayerTransfer {
    /** Inventory, armor, offhand and the selected hotbar slot. */
    static final Set<String> ITEM_KEYS = Set.of("Inventory", "equipment", "SelectedItemSlot");
    static final Set<String> XP_KEYS = Set.of("XpLevel", "XpP", "XpTotal", "XpSeed", "Score");
    static final Set<String> ENDER_KEYS = Set.of("EnderItems");
    /** Parrots sitting on the player's shoulders. */
    static final Set<String> SHOULDER_KEYS = Set.of("ShoulderEntityLeft", "ShoulderEntityRight");
    /**
     * Player data that mods keep on the player: Trinkets slots ("trinkets", or "cardinal_components" in old
     * saves) and Fabric attachments, which hold the worn Traveler's Backpack. Namespaced keys count too.
     * Everything else (position, dimension, spawn, health, effects, game mode...) stays behind.
     */
    static final Set<String> MOD_KEYS = Set.of("trinkets", "cardinal_components", "fabric:attachments");

    private PlayerTransfer() {
    }

    static boolean carries(String key, ImportData data) {
        if (ITEM_KEYS.contains(key) || MOD_KEYS.contains(key) || key.contains(":")) {
            return data.items();
        }
        if (XP_KEYS.contains(key)) {
            return data.xp();
        }
        if (SHOULDER_KEYS.contains(key)) {
            return data.mobsEnabled();
        }
        return ENDER_KEYS.contains(key) && data.enderChest();
    }

    /** Copies the carried parts of {@code data} over {@code target} (the new world's player record). */
    public static CompoundTag merge(CompoundTag target, ImportData data) {
        CompoundTag merged = target.copy();
        if (data.items()) {
            // The source decides what is in these slots, even when it has nothing there.
            ITEM_KEYS.forEach(merged::remove);
            MOD_KEYS.forEach(merged::remove);
        }
        if (data.mobsEnabled()) {
            SHOULDER_KEYS.forEach(merged::remove);
        }
        for (String key : data.player().keySet()) {
            if (carries(key, data)) {
                merged.put(key, data.player().get(key).copy());
            }
        }
        return merged;
    }
}
