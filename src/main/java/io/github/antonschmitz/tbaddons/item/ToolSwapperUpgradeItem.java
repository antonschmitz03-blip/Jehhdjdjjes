package io.github.antonschmitz.tbaddons.item;

import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.item.upgrade.UpgradeItem;
import io.github.antonschmitz.tbaddons.upgrade.ToolSwapperUpgrade;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;

public class ToolSwapperUpgradeItem extends UpgradeItem {
    public ToolSwapperUpgradeItem(Properties properties) {
        // UpgradeItem resolves this to "item.travelersbackpack.tool_swapper_upgrade_tooltip" (defined in our lang files).
        super(properties, "tool_swapper_upgrade");
    }

    @Override
    public boolean hasBlockFunctionality() {
        return false;
    }

    @Override
    public Class<? extends UpgradeBase<?>> getUpgradeClass() {
        return ToolSwapperUpgrade.class;
    }

    @Override
    public TriFunction<UpgradeManager, Integer, ItemStack, Optional<? extends UpgradeBase<?>>> getUpgrade() {
        return (upgradeManager, dataHolderSlot, provider) -> Optional.of(new ToolSwapperUpgrade(upgradeManager, dataHolderSlot));
    }
}
