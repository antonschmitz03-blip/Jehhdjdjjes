package io.github.antonschmitz.tbaddons.mixin;

import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Furnaces, blast furnaces, smokers, hoppers, dispensers, droppers and brewing stands show the worn backpack too.
 * The player comes from the player inventory slots the menu has just added; shift-click stays vanilla.
 */
@Mixin({AbstractFurnaceMenu.class, HopperMenu.class, DispenserMenu.class, BrewingStandMenu.class})
public abstract class MachineMenuMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void tbaddons$addBackpackPanel(CallbackInfo ci) {
        for (Slot slot : ((AbstractContainerMenu) (Object) this).slots) {
            if (slot.container instanceof Inventory inventory) {
                ((BackpackPanelMenu) this).tbaddons$addBackpackPanel(inventory.player);
                return;
            }
        }
    }
}
