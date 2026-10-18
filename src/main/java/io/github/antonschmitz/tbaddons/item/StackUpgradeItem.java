package io.github.antonschmitz.tbaddons.item;

import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import com.tiviacz.travelersbackpack.item.upgrade.UpgradeItem;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import org.apache.commons.lang3.function.TriFunction;

public class StackUpgradeItem extends UpgradeItem {
    private final int multiplier;
    private final Class<? extends UpgradeBase<?>> upgradeClass;
    private final BiFunction<UpgradeManager, Integer, ? extends UpgradeBase<?>> factory;

    public StackUpgradeItem(Properties properties, String name, int multiplier, Class<? extends UpgradeBase<?>> upgradeClass,
                            BiFunction<UpgradeManager, Integer, ? extends UpgradeBase<?>> factory) {
        // UpgradeItem resolves this to "item.travelersbackpack.<name>_tooltip" (defined in our lang files).
        super(properties, name);
        this.multiplier = multiplier;
        this.upgradeClass = upgradeClass;
        this.factory = factory;
    }

    public int getMultiplier() {
        return this.multiplier;
    }

    /** Highest stack multiplier among the upgrades installed in the given backpack, 1 if none. */
    public static int getMultiplier(ItemStack backpack) {
        int multiplier = 1;
        ItemContainerContents upgrades = backpack.getOrDefault(ModDataComponents.UPGRADES, ItemContainerContents.EMPTY);
        for (ItemStackTemplate upgrade : upgrades.nonEmptyItems()) {
            if (upgrade.item().value() instanceof StackUpgradeItem stackUpgrade) {
                multiplier = Math.max(multiplier, stackUpgrade.getMultiplier());
            }
        }
        return multiplier;
    }

    @Override
    public boolean requiresEquippedBackpack() {
        return false;
    }

    @Override
    public Class<? extends UpgradeBase<?>> getUpgradeClass() {
        return this.upgradeClass;
    }

    @Override
    public TriFunction<UpgradeManager, Integer, ItemStack, Optional<? extends UpgradeBase<?>>> getUpgrade() {
        return (upgradeManager, dataHolderSlot, provider) -> Optional.of(this.factory.apply(upgradeManager, dataHolderSlot));
    }
}
