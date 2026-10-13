package io.github.antonschmitz.tbaddons.panel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws the backpack panel in the vanilla container style (light grey frame with bevelled slots). */
@Environment(EnvType.CLIENT)
public final class BackpackPanelRenderer {
    private static final int BACKGROUND = 0xFFC6C6C6;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_INNER = 0xFF8B8B8B;
    private static final int LABEL = -12566464;

    private BackpackPanelRenderer() {
    }

    public static void extractBackground(GuiGraphicsExtractor graphics, int leftPos, int topPos, BackpackPanelMenu panel) {
        int x0 = leftPos + BackpackPanel.LEFT;
        int y0 = topPos;
        int x1 = x0 + BackpackPanel.WIDTH;
        int y1 = y0 + BackpackPanel.height(panel.tbaddons$panelSize());

        graphics.fill(x0 + 1, y0, x1 - 1, y1, OUTLINE);
        graphics.fill(x0, y0 + 1, x1, y1 - 1, OUTLINE);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, BACKGROUND);
        graphics.fill(x0 + 1, y0 + 1, x1 - 2, y0 + 3, HIGHLIGHT);
        graphics.fill(x0 + 1, y0 + 1, x0 + 3, y1 - 2, HIGHLIGHT);
        graphics.fill(x0 + 2, y1 - 3, x1 - 1, y1 - 1, SHADOW);
        graphics.fill(x1 - 3, y0 + 2, x1 - 1, y1 - 1, SHADOW);

        for (int i = 0; i < panel.tbaddons$panelSize(); i++) {
            int sx = leftPos + BackpackPanel.slotX(i) - 1;
            int sy = topPos + BackpackPanel.slotY(i) - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, HIGHLIGHT);
            graphics.fill(sx, sy, sx + 17, sy + 17, SLOT_SHADOW);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_INNER);
        }
    }

    /** Called with the pose already translated to the screen's top left corner. */
    public static void extractLabel(GuiGraphicsExtractor graphics, Font font, BackpackPanelMenu panel) {
        graphics.text(font, panel.tbaddons$panelWrapper().getBackpackStack().getHoverName(), BackpackPanel.LEFT + 8, 6, LABEL, false);
    }
}
