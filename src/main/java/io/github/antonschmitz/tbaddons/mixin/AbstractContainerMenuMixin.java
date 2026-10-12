package io.github.antonschmitz.tbaddons.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla moves a slot's whole stack to the cursor or hotbar when swapping. For oversized stacks (only
 * possible in upgraded backpack storage) that would put more than a normal stack into the player's
 * inventory, so those clicks are limited to one normal stack or ignored.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void tbaddons$guardOversizedStacks(int slotIndex, int buttonNum, ContainerInput input, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotIndex);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || stack.getCount() <= stack.getMaxStackSize()) {
            return;
        }

        if (input == ContainerInput.SWAP && (buttonNum >= 0 && buttonNum < 9 || buttonNum == 40)) {
            Inventory inventory = player.getInventory();
            if (inventory.getItem(buttonNum).isEmpty() && slot.mayPickup(player)) {
                inventory.setItem(buttonNum, slot.safeTake(stack.getMaxStackSize(), Integer.MAX_VALUE, player));
            }
            ci.cancel();
        } else if (input == ContainerInput.PICKUP && !menu.getCarried().isEmpty()
                && !ItemStack.isSameItemSameComponents(stack, menu.getCarried())) {
            ci.cancel();
        }
    }
}
