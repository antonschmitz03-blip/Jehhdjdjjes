package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.sorter.ContainerSorter;
import com.tiviacz.travelersbackpack.inventory.sorter.SortSelector;
import io.github.antonschmitz.tbaddons.StackLimits;
import io.github.antonschmitz.tbaddons.item.StackUpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The sort button merges stacks up to the normal max stack size. With a Stack Upgrade installed, merge up to
 * the upgraded limit instead (e.g. 128 with the Iron Stack Upgrade), so sorting leaves no half-full stacks.
 */
@Mixin(value = ContainerSorter.class, remap = false)
public abstract class ContainerSorterMixin {
    /** Multiplier of the backpack being sorted; sorting only runs on the server thread. */
    @Unique
    private static int tbaddons$multiplier = 1;

    @Inject(method = "sortBackpack", at = @At("HEAD"))
    private static void tbaddons$startSort(BackpackWrapper wrapper, Player player, SortSelector.SortType type, boolean shiftPressed, CallbackInfo ci) {
        tbaddons$multiplier = StackUpgradeItem.getMultiplier(wrapper.getBackpackStack());
    }

    @Inject(method = "sortBackpack", at = @At("RETURN"))
    private static void tbaddons$endSort(BackpackWrapper wrapper, Player player, SortSelector.SortType type, boolean shiftPressed, CallbackInfo ci) {
        tbaddons$multiplier = 1;
    }

    @Redirect(method = {"addStackWithMerge", "combineStacks", "canMergeItems"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int tbaddons$upgradedMaxStackSize(ItemStack stack) {
        return StackLimits.limit(stack, tbaddons$multiplier);
    }
}
