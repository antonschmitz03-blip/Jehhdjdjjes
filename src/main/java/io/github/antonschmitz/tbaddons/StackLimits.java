package io.github.antonschmitz.tbaddons;

import net.minecraft.world.item.ItemStack;

public final class StackLimits {
    private StackLimits() {
    }

    /** Max stack size of {@code stack} scaled by the multiplier; unstackable items stay unstackable. */
    public static int scale(int maxStackSize, int multiplier) {
        return maxStackSize > 1 ? maxStackSize * multiplier : maxStackSize;
    }

    public static int limit(ItemStack stack, int multiplier) {
        return scale(stack.getMaxStackSize(), multiplier);
    }

    /** Implemented on the backpack storage handler; returns the active stack multiplier. */
    public interface MultiplierHolder {
        int tbaddons$getMultiplier();
    }
}
