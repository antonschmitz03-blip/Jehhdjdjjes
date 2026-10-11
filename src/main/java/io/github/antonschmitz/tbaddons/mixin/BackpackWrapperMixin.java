package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With Trinkets/Accessories, Traveler's Backpack builds a fresh wrapper for every access unless the backpack
 * screen is open. While a chest with the backpack panel is open, hand out the panel's wrapper instead, so
 * upgrades (pickup, magnet, ...) and the panel never work on two copies of the same storage.
 */
@Mixin(value = BackpackWrapper.class, remap = false)
public abstract class BackpackWrapperMixin {
    @Inject(method = "getBackpackWrapper(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;[I)Lcom/tiviacz/travelersbackpack/inventory/BackpackWrapper;",
            at = @At("HEAD"), cancellable = true)
    private static void tbaddons$usePanelWrapper(Player player, ItemStack backpack, int[] dataLoad, CallbackInfoReturnable<BackpackWrapper> cir) {
        if (player.containerMenu instanceof BackpackPanelMenu menu) {
            BackpackWrapper wrapper = menu.tbaddons$panelWrapper();
            if (wrapper != null && wrapper.getBackpackStack() == backpack) {
                cir.setReturnValue(wrapper);
            }
        }
    }
}
