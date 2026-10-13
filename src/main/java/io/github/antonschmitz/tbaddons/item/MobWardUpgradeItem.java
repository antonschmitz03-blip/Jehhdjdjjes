package io.github.antonschmitz.tbaddons.item;

import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.item.upgrade.UpgradeItem;
import io.github.antonschmitz.tbaddons.upgrade.MobWardUpgrade;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;

public class MobWardUpgradeItem extends UpgradeItem {
    public MobWardUpgradeItem(Properties properties) {
        // UpgradeItem resolves this to "item.travelersbackpack.mob_ward_upgrade_tooltip" (defined in our lang files).
        super(properties, "mob_ward_upgrade");
    }

    @Override
    public boolean hasBlockFunctionality() {
        return false;
    }

    @Override
    public Class<? extends UpgradeBase<?>> getUpgradeClass() {
        return MobWardUpgrade.class;
    }

    @Override
    public TriFunction<UpgradeManager, Integer, ItemStack, Optional<? extends UpgradeBase<?>>> getUpgrade() {
        return (upgradeManager, dataHolderSlot, provider) -> Optional.of(new MobWardUpgrade(upgradeManager, dataHolderSlot));
    }
}
