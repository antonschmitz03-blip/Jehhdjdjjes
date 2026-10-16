package io.github.antonschmitz.vplustweaks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.antonschmitz.vplustweaks.EnchantingCosts;
import io.github.antonschmitz.vplustweaks.TweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cuts the level cost to a quarter of the Enchanting Infuser. Refunds for removing enchantments are calculated
 * separately by the Infuser (from the enchantments, in XP points), so this can't be used to gain XP.
 */
@Pseudo
@Mixin(targets = "fuzs.enchantinginfuser.common.world.inventory.InfuserMenu", remap = false)
public abstract class InfuserMenuMixin {
    @ModifyReturnValue(method = "calculateEnchantingCost", at = @At("RETURN"))
    private int vplustweaks$quarterCost(int cost) {
        return TweaksConfig.enabled(TweaksConfig.CHEAPER_INFUSER) ? EnchantingCosts.quarter(cost) : cost;
    }
}
