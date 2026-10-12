package io.github.antonschmitz.graves;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/**
 * One stored item and the slot it came from: {@code inv:<index>} for the player inventory (incl. armor and
 * offhand), {@code trinket:<slot>@<index>} for Trinkets, {@code backpack} for Traveler's Backpack's own worn
 * slot, or empty when the origin is unknown.
 */
public record GraveEntry(String slot, ItemStack stack) {
    public static final Codec<GraveEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("slot", "").forGetter(GraveEntry::slot),
            ItemStack.CODEC.fieldOf("item").forGetter(GraveEntry::stack)
    ).apply(instance, GraveEntry::new));
}
