package io.github.antonschmitz.tbaddons.client;

import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.client.screens.widgets.UpgradeWidgetBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.Point;
import io.github.antonschmitz.tbaddons.upgrade.ToolSwapperUpgrade;

public class ToolSwapperWidget extends UpgradeWidgetBase<ToolSwapperUpgrade> {
    public ToolSwapperWidget(BackpackScreen screen, ToolSwapperUpgrade upgrade, Point pos, Point tabUv, String upgradeIconTooltip) {
        super(screen, upgrade, pos, tabUv, upgradeIconTooltip);
    }
}
