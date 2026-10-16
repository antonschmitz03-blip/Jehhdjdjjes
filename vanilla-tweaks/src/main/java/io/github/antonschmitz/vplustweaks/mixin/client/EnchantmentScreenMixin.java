package io.github.antonschmitz.vplustweaks.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.antonschmitz.vplustweaks.EnchantingCosts;
import io.github.antonschmitz.vplustweaks.TweaksConfig;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shows the halved level requirement on the enchanting table buttons and tooltips. */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin {
    @ModifyExpressionValue(
            method = {"extractBackground", "extractRenderState"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;costs:[I", opcode = Opcodes.GETFIELD),
            require = 0 // display only: never crash the client over it
    )
    private int[] vplustweaks$showHalvedLevels(int[] costs) {
        return TweaksConfig.enabled(TweaksConfig.CHEAPER_ENCHANTING_TABLE) ? EnchantingCosts.halve(costs) : costs;
    }
}
