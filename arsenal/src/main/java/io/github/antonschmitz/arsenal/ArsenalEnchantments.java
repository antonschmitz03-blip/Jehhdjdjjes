package io.github.antonschmitz.arsenal;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** The mod's enchantments. They are plain data entries; the weapons read their levels and apply the effects. */
public final class ArsenalEnchantments {
    /** Sling: stone bursts into pellets on impact. */
    public static final ResourceKey<Enchantment> SHRAPNEL = key("shrapnel");
    /** Sling: drawing past full power adds damage. */
    public static final ResourceKey<Enchantment> WHIRL = key("whirl");
    /** Gale Bow: focus lasts longer. */
    public static final ResourceKey<Enchantment> TAILWIND = key("tailwind");
    /** Gale Bow: more air jumps. */
    public static final ResourceKey<Enchantment> SKYBOUND = key("skybound");
    /** Both: faster draw. */
    public static final ResourceKey<Enchantment> STEADY_HAND = key("steady_hand");

    private ArsenalEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, VanillaArsenal.id(name));
    }

    /** Level of an enchantment on the stack, 0 when it has none. */
    public static int level(Entity entity, ItemStack stack, ResourceKey<Enchantment> key) {
        return level(entity.level().registryAccess(), stack, key);
    }

    public static int level(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack, ResourceKey<Enchantment> key) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }
}
