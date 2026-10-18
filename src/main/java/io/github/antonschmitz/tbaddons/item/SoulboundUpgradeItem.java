package io.github.antonschmitz.tbaddons.item;

import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.item.upgrade.UpgradeItem;
import io.github.antonschmitz.tbaddons.upgrade.SoulboundUpgrade;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;

public class SoulboundUpgradeItem extends UpgradeItem {
    public SoulboundUpgradeItem(Properties properties) {
        // UpgradeItem resolves this to "item.travelersbackpack.soulbound_upgrade_tooltip" (defined in our lang files).
        super(properties, "soulbound_upgrade");
    }

    @Override
    public boolean hasBlockFunctionality() {
        return false;
    }

    @Override
    public Class<? extends UpgradeBase<?>> getUpgradeClass() {
        return SoulboundUpgrade.class;
    }

    @Override
    public TriFunction<UpgradeManager, Integer, ItemStack, Optional<? extends UpgradeBase<?>>> getUpgrade() {
        return (upgradeManager, dataHolderSlot, provider) -> Optional.of(new SoulboundUpgrade(upgradeManager, dataHolderSlot));
    }
}
