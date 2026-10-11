package io.github.antonschmitz.tbaddons.mixin;

import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shulker boxes show the worn backpack next to the container. */
@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin {
    @Shadow
    @Final
    private Container container;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", at = @At("TAIL"))
    private void tbaddons$addBackpackPanel(int containerId, Inventory inventory, Container container, CallbackInfo ci) {
        ((BackpackPanelMenu) this).tbaddons$addBackpackPanel(inventory.player);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void tbaddons$quickMoveWithPanel(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = ((BackpackPanelMenu) this).tbaddons$quickMoveWithPanel(slotIndex, this.container.getContainerSize());
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
