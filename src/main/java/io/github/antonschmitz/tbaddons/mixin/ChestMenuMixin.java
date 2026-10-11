package io.github.antonschmitz.tbaddons.mixin;

import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chests, double chests, barrels and ender chests show the worn backpack next to the container. */
@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin {
    @Shadow
    public abstract int getRowCount();

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At("TAIL"))
    private void tbaddons$addBackpackPanel(MenuType<?> menuType, int containerId, Inventory inventory, Container container, int rows, CallbackInfo ci) {
        ((BackpackPanelMenu) this).tbaddons$addBackpackPanel(inventory.player);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void tbaddons$quickMoveWithPanel(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = ((BackpackPanelMenu) this).tbaddons$quickMoveWithPanel(slotIndex, this.getRowCount() * 9);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
