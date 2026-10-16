package io.github.antonschmitz.vplustweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.antonschmitz.vplustweaks.EnchantingCosts;
import io.github.antonschmitz.vplustweaks.TweaksConfig;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Halves the level requirement of the enchanting table. Only the checks in clickMenuButton see the
 * halved costs; the enchanting itself (in a lambda) still uses the full cost, so enchantments stay as
 * strong as in vanilla.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @ModifyExpressionValue(
            method = "clickMenuButton",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;costs:[I", opcode = Opcodes.GETFIELD)
    )
    private int[] vplustweaks$halveRequiredLevels(int[] costs) {
        return TweaksConfig.enabled(TweaksConfig.CHEAPER_ENCHANTING_TABLE) ? EnchantingCosts.halve(costs) : costs;
    }
}
