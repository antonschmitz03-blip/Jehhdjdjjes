package io.github.antonschmitz.vplustweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.antonschmitz.vplustweaks.EnchantingCosts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Halves the level cost of the Enchanting Infuser. Refunds for removing enchantments are calculated
 * separately by the Infuser (from the enchantments, in XP points), so this can't be used to gain XP.
 */
@Pseudo
@Mixin(targets = "fuzs.enchantinginfuser.common.world.inventory.InfuserMenu", remap = false)
public abstract class InfuserMenuMixin {
    @ModifyReturnValue(method = "calculateEnchantingCost", at = @At("RETURN"))
    private int vplustweaks$halveCost(int cost) {
        return EnchantingCosts.halve(cost);
    }
}
