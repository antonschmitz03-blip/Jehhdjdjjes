package io.github.antonschmitz.graves;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/** A plain 6-row chest screen over the grave's items. Server-side only; the client uses the vanilla chest screen. */
public class GraveMenu extends ChestMenu {
    private final GraveBlockEntity grave;
    private final Container container;

    GraveMenu(int containerId, Inventory playerInventory, Container container, GraveBlockEntity grave) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.grave = grave;
        this.container = container;
    }

    @Override
    public boolean stillValid(Player player) {
        var pos = grave.getBlockPos();
        return !grave.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        grave.closeMenu(container);
    }
}
