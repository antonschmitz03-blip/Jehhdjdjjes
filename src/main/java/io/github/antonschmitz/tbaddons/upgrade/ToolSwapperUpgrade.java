package io.github.antonschmitz.tbaddons.upgrade;

import com.mojang.datafixers.util.Pair;
import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.client.screens.widgets.WidgetBase;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.UpgradeManager;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu;
import com.tiviacz.travelersbackpack.inventory.upgrades.IEnable;
import com.tiviacz.travelersbackpack.inventory.upgrades.Point;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;
import io.github.antonschmitz.tbaddons.client.ToolSwapperWidget;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.Slot;

/**
 * Marker upgrade with an on/off toggle; the swapping itself happens in
 * {@link io.github.antonschmitz.tbaddons.ToolSwapHandler}.
 */
public class ToolSwapperUpgrade extends UpgradeBase<ToolSwapperUpgrade> implements IEnable {
    public ToolSwapperUpgrade(UpgradeManager manager, int dataHolderSlot) {
        super(manager, dataHolderSlot, new Point(40, 28));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public WidgetBase<?> createWidget(BackpackScreen screen, int x, int y) {
        return new ToolSwapperWidget(
                screen, this, new Point(screen.getGuiLeft() + x, screen.getGuiTop() + y), new Point(137, 0), "screen.tbaddons.tool_swapper_upgrade"
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
}
