package io.github.antonschmitz.vplustweaks.mixin.client;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Shows the real level cost instead of "Too Expensive!" (display only, see AnvilMenuMixin). */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    // Display only: if this ever stops matching, the game still runs and just shows the old text.
    @ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40), require = 0)
    private int vplustweaks$removeTooExpensiveText(int limit) {
        return Integer.MAX_VALUE;
    }
}
