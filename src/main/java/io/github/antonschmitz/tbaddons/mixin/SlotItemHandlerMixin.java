package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import com.tiviacz.travelersbackpack.inventory.menu.slot.SlotItemHandler;
import io.github.antonschmitz.tbaddons.StackLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = SlotItemHandler.class, remap = false)
public abstract class SlotItemHandlerMixin {
    @Shadow
    public abstract ItemStackHandler getItemHandler();

    /** {@code getMaxStackSize(ItemStack)} probes the handler with a stack of {@code maxInput}; scale that probe. */
    @ModifyVariable(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("STORE"), ordinal = 0)
    private int tbaddons$scaleMaxInput(int maxInput) {
        if (this.getItemHandler() instanceof StackLimits.MultiplierHolder holder) {
            return StackLimits.scale(maxInput, holder.tbaddons$getMultiplier());
        }
        return maxInput;
    }
}
