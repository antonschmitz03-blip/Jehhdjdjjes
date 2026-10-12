package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import io.github.antonschmitz.tbaddons.StackLimits;
import io.github.antonschmitz.tbaddons.item.StackUpgradeItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * The anonymous handler created by {@code BackpackWrapper#createHandler}, used for the main storage
 * (dataId 0) and the tool slots (dataId 2). Raises the per-slot limit of the main storage.
 */
@Mixin(targets = "com.tiviacz.travelersbackpack.inventory.BackpackWrapper$1", remap = false)
public abstract class BackpackStorageHandlerMixin extends ItemStackHandler implements StackLimits.MultiplierHolder {
    @Shadow
    @Final
    int val$dataId;

    @Shadow
    @Final
    BackpackWrapper this$0;

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(this.getSlotLimit(slot), StackLimits.limit(stack, this.tbaddons$getMultiplier()));
    }

    @Override
    public int tbaddons$getMultiplier() {
        return this.val$dataId == 0 ? StackUpgradeItem.getMultiplier(this.this$0.getBackpackStack()) : 1;
    }
}
