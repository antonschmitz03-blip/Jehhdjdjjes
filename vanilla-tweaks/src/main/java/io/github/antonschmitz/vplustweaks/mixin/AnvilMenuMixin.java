package io.github.antonschmitz.vplustweaks.mixin;

import io.github.antonschmitz.vplustweaks.TweaksConfig;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Removes the "Too Expensive!" cap (40 levels) from anvil results. The real cost still has to be paid. */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    // createResult compares against 40 three times; the third is `cost >= 40 -> no result`.
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
    private int vplustweaks$removeTooExpensiveLimit(int limit) {
        return TweaksConfig.enabled(TweaksConfig.NO_ANVIL_LIMIT) ? Integer.MAX_VALUE : limit;
    }
}
