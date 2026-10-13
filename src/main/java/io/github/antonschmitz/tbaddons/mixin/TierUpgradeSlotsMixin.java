package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.inventory.Tiers;
import io.github.antonschmitz.tbaddons.UpgradeSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every backpack tier gets {@link UpgradeSlots#EXTRA} more upgrade slots (on top of the configured count). */
@Mixin(Tiers.Tier.class)
public abstract class TierUpgradeSlotsMixin {
    @Inject(method = "getUpgradeSlots", at = @At("RETURN"), cancellable = true)
    private void tbaddons$extraUpgradeSlots(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() + UpgradeSlots.EXTRA);
    }
}
