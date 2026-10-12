package io.github.antonschmitz.graves.mixin.compat;

import com.tiviacz.travelersbackpack.TravelersBackpack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Traveler's Backpack only drops a worn backpack as an item (instead of placing it as a block somewhere) when it
 * knows a grave mod is installed. Only matters without Trinkets; with Trinkets the backpack drops from its slot.
 */
@Mixin(value = TravelersBackpack.class, remap = false)
public abstract class TravelersBackpackMixin {
    @Inject(method = "isAnyGraveModInstalled", at = @At("HEAD"), cancellable = true)
    private static void simplegraves$graveModInstalled(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
