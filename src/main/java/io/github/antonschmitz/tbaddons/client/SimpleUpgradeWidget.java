package io.github.antonschmitz.tbaddons.client;

import com.tiviacz.travelersbackpack.client.screens.BackpackScreen;
import com.tiviacz.travelersbackpack.client.screens.widgets.UpgradeWidgetBase;
import com.tiviacz.travelersbackpack.inventory.upgrades.Point;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;

/** Tab-less widget: just the upgrade icon (plus the on/off toggle for upgrades implementing IEnable). */
public class SimpleUpgradeWidget<U extends UpgradeBase<?>> extends UpgradeWidgetBase<U> {
    public SimpleUpgradeWidget(BackpackScreen screen, U upgrade, Point pos, Point tabUv, String upgradeIconTooltip) {
        super(screen, upgrade, pos, tabUv, upgradeIconTooltip);
    }
}
