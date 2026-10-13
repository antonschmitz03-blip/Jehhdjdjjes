package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.Tiers;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A backpack stores its upgrade slot count when it is first used, so backpacks that already exist keep their old
 * count. Raise it to the tier's current count (see {@link TierUpgradeSlotsMixin}) the next time it is read.
 */
@Mixin(BackpackWrapper.class)
public abstract class BackpackUpgradeSlotsMixin {
    @Shadow
    protected ItemStack stack;

    @Inject(method = "getUpgradesSize", at = @At("HEAD"))
    private void tbaddons$raiseStoredUpgradeSlots(CallbackInfoReturnable<Integer> cir) {
        Integer stored = this.stack.get(ModDataComponents.UPGRADE_SLOTS);
        Integer tier = this.stack.get(ModDataComponents.TIER);
        if (stored == null || tier == null) {
            return;
        }
        int expected = Tiers.of(tier).getUpgradeSlots();
        if (stored < expected) {
            this.stack.set(ModDataComponents.UPGRADE_SLOTS, expected);
        }
    }
}
