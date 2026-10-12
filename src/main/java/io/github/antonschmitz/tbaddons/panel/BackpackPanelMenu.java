package io.github.antonschmitz.tbaddons.panel;

import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Implemented by every menu (see {@code BackpackPanelMenuMixin}); chests, shulker boxes, furnaces, hoppers, dispensers and brewing stands add the panel. */
public interface BackpackPanelMenu {
    /** Appends the storage slots of the worn backpack, if any, after the player inventory slots. */
    void tbaddons$addBackpackPanel(Player player);

    /** The wrapper behind the panel slots, or null if this menu has no panel. */
    @Nullable
    BackpackWrapper tbaddons$panelWrapper();

    int tbaddons$panelSize();

    /**
     * Shift-click handling for menus with a panel: container slots go to the player inventory, player inventory
     * slots to the container, backpack slots to the container first and then the player inventory.
     * Returns null when the menu has no panel, so the vanilla handling runs.
     */
    @Nullable
    ItemStack tbaddons$quickMoveWithPanel(int slotIndex, int containerSize);
}
