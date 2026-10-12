package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import com.tiviacz.travelersbackpack.inventory.menu.slot.BackpackSlotItemHandler;
import com.tiviacz.travelersbackpack.inventory.sorter.ContainerSorter;
import io.github.antonschmitz.tbaddons.panel.BackpackPanel;
import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class BackpackPanelMenuMixin implements BackpackPanelMenu {
    @Unique
    private static final int PLAYER_INVENTORY_SIZE = 36;

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Unique
    private static final int[] TYPES = {ContainerSorter.SORT_BACKPACK, ContainerSorter.QUICK_STACK,
            ContainerSorter.TRANSFER_TO_BACKPACK, ContainerSorter.TRANSFER_TO_PLAYER};

    @Unique
    @Nullable
    private BackpackWrapper tbaddons$panelWrapper;

    @Unique
    private int tbaddons$panelStart = -1;

    @Unique
    private int tbaddons$panelSize;

    @Shadow
    protected abstract Slot addSlot(Slot slot);

    @Shadow
    protected abstract boolean moveItemStackTo(ItemStack itemStack, int startSlot, int endSlot, boolean backwards);

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void tbaddons$panelButton(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        int button = id - BackpackPanel.BUTTON_ID_BASE;
        if (this.tbaddons$panelWrapper == null || button < 0 || button >= BackpackPanel.BUTTON_COUNT) {
            return;
        }
        ContainerSorter.selectSort(this.tbaddons$panelWrapper, player, TYPES[button], false);
        cir.setReturnValue(true);
    }

    @Override
    public void tbaddons$addBackpackPanel(Player player) {
        if (this.tbaddons$panelStart != -1 || player.isSpectator() || !AttachmentUtils.isWearingBackpack(player)) {
            return;
        }
        BackpackWrapper wrapper = AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.LOAD_ALL.get());
        if (wrapper == null || wrapper.getStorage() == null) {
            return;
        }
        ItemStackHandler storage = wrapper.getStorage();
        this.tbaddons$panelWrapper = wrapper;
        this.tbaddons$panelStart = this.slots.size();
        this.tbaddons$panelSize = storage.getSlots();
        for (int i = 0; i < storage.getSlots(); i++) {
            this.addSlot(new BackpackSlotItemHandler(storage, i, BackpackPanel.slotX(i), BackpackPanel.slotY(i)));
        }
    }

    @Override
    public @Nullable BackpackWrapper tbaddons$panelWrapper() {
        return this.tbaddons$panelWrapper;
    }

    @Override
    public int tbaddons$panelSize() {
        return this.tbaddons$panelWrapper == null ? 0 : this.tbaddons$panelSize;
    }

    @Override
    public @Nullable ItemStack tbaddons$quickMoveWithPanel(int slotIndex, int containerSize) {
        if (this.tbaddons$panelWrapper == null) {
            return null;
        }
        int playerEnd = containerSize + PLAYER_INVENTORY_SIZE;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < containerSize) {
            if (!this.moveItemStackTo(stack, containerSize, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < playerEnd) {
            if (!this.moveItemStackTo(stack, 0, containerSize, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = this.moveItemStackTo(stack, 0, containerSize, false);
            moved |= !stack.isEmpty() && this.moveItemStackTo(stack, containerSize, playerEnd, true);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return clicked;
    }
}
