package io.github.antonschmitz.tbaddons;

import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.item.TravelersBackpackItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Checks for an enabled Soulbound Upgrade. Reads the backpack's upgrade slots straight from the item
 * instead of building a {@code BackpackWrapper}, since it runs in the middle of the death logic.
 */
public final class Soulbound {
    private Soulbound() {
    }

    public static boolean isSoulbound(ItemStack backpack) {
        if (!(backpack.getItem() instanceof TravelersBackpackItem)) {
            return false;
        }
        ItemContainerContents upgrades = backpack.get(ModDataComponents.UPGRADES);
        return upgrades != null && upgrades.nonEmptyItemCopyStream().anyMatch(upgrade ->
                upgrade.is(TBAddons.SOULBOUND_UPGRADE) && upgrade.getOrDefault(ModDataComponents.UPGRADE_ENABLED, true));
    }
}
