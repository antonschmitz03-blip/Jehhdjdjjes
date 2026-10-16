package io.github.antonschmitz.vplustweaks.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Any enchantments can be combined (anvil, enchanting table, Enchanting Infuser, loot), except Riptide with
 * Loyalty or Channeling: a Riptide trident launches you instead of being thrown, so those two would never work.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void vplustweaks$allowCombinations(Holder<Enchantment> first, Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        // An enchantment is never "compatible" with itself, and the Riptide clash keeps the vanilla answer (false).
        if (first.equals(second) || vplustweaks$riptideClash(first, second) || vplustweaks$riptideClash(second, first)) {
            return;
        }
        cir.setReturnValue(true);
    }

    private static boolean vplustweaks$riptideClash(Holder<Enchantment> riptide, Holder<Enchantment> other) {
        return riptide.is(Enchantments.RIPTIDE) && (other.is(Enchantments.LOYALTY) || other.is(Enchantments.CHANNELING));
    }
}
