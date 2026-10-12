package io.github.antonschmitz.arsenal;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a sling can throw. Damage is for a fully drawn sling and scales with the draw.
 * Real slings throw a stone at 30-100 m/s, which hits harder than an arrow, so any stone block beats a bow shot.
 */
public record SlingAmmo(float damage, int pellets, float spread, float knockback) {
    public static final TagKey<Item> STONE = TagKey.create(Registries.ITEM, VanillaArsenal.id("sling_ammo"));
    public static final TagKey<Item> DEEPSLATE = TagKey.create(Registries.ITEM, VanillaArsenal.id("sling_ammo_deepslate"));

    /** Any stone block: 10 damage (5 hearts), a fully drawn bow does 6 to 9. */
    public static final SlingAmmo STONE_BLOCK = new SlingAmmo(10.0F, 1, 0.3F, 0.6F);
    /** Deepslate and all its variants: slightly denser. */
    public static final SlingAmmo DEEPSLATE_BLOCK = new SlingAmmo(12.0F, 1, 0.3F, 0.7F);
    /** Gravel: three small pellets in a spread. */
    public static final SlingAmmo GRAVEL = new SlingAmmo(4.0F, 3, 3.0F, 0.2F);
    /** Snowball: no damage, shoves mobs around. */
    public static final SlingAmmo SNOWBALL = new SlingAmmo(0.0F, 1, 0.3F, 1.6F);

    /** The ammo a stack counts as, or null when it can't be thrown. */
    public static SlingAmmo of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(DEEPSLATE)) {
            return DEEPSLATE_BLOCK;
        }
        if (stack.is(STONE)) {
            return STONE_BLOCK;
        }
        if (stack.is(Items.GRAVEL)) {
            return GRAVEL;
        }
        if (stack.is(Items.SNOWBALL)) {
            return SNOWBALL;
        }
        return null;
    }
}
