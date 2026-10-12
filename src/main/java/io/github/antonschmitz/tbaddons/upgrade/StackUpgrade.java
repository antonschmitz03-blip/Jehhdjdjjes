package io.github.antonschmitz.tbaddons.upgrade;

import com.mojang.datafixers.util.Pair;
import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.client.screens.widgets.WidgetBase;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu;
import com.tiviacz.travelersbackpack.inventory.upgrades.Point;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import io.github.antonschmitz.tbaddons.client.SimpleUpgradeWidget;
import io.github.antonschmitz.tbaddons.item.StackUpgradeItem;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.Slot;

/**
 * Marker upgrade for the stack size multiplier. The actual limit is read straight from the backpack's
 * upgrade items (see {@link StackUpgradeItem#getMultiplier}), so it also applies when the upgrade list
 * of a wrapper hasn't been loaded. One subclass per tier so different tiers count as different upgrades.
 */
public abstract class StackUpgrade extends UpgradeBase<StackUpgrade> {
    private final String tooltipKey;

    protected StackUpgrade(UpgradeManager manager, int dataHolderSlot, String tooltipKey) {
        super(manager, dataHolderSlot, new Point(40, 28));
        this.tooltipKey = tooltipKey;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public WidgetBase<?> createWidget(BackpackScreen screen, int x, int y) {
        return new SimpleUpgradeWidget<>(
                screen, this, new Point(screen.getGuiLeft() + x, screen.getGuiTop() + y), new Point(137, 0), this.tooltipKey
        );
    }

    @Override
    public boolean hasTab() {
        return false;
    }

    @Override
    public List<Pair<Integer, Integer>> getUpgradeSlotsPosition(int x, int y) {
        return List.of();
    }

    @Override
    public List<? extends Slot> getUpgradeSlots(BackpackBaseMenu menu, BackpackWrapper wrapper, int x, int y) {
        return List.of();
    }

    public static class Iron extends StackUpgrade {
        public Iron(UpgradeManager manager, int dataHolderSlot) {
            super(manager, dataHolderSlot, "screen.tbaddons.iron_stack_upgrade");
        }
    }

    public static class Gold extends StackUpgrade {
        public Gold(UpgradeManager manager, int dataHolderSlot) {
            super(manager, dataHolderSlot, "screen.tbaddons.gold_stack_upgrade");
        }
    }

    public static class Diamond extends StackUpgrade {
        public Diamond(UpgradeManager manager, int dataHolderSlot) {
            super(manager, dataHolderSlot, "screen.tbaddons.diamond_stack_upgrade");
        }
    }
}
