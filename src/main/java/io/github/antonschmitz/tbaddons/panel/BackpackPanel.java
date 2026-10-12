package io.github.antonschmitz.tbaddons.panel;

/** Layout of the backpack panel shown to the right of chest and shulker box screens (coordinates relative to the screen). */
public final class BackpackPanel {
    /** Width of the vanilla chest/shulker box background. */
    public static final int CONTAINER_WIDTH = 176;
    public static final int LEFT = CONTAINER_WIDTH + 2;
    public static final int COLUMNS = 9;
    public static final int WIDTH = 7 + COLUMNS * 18 + 7;
    private static final int SLOTS_TOP = 18;

    private BackpackPanel() {
    }

    public static int slotX(int index) {
        return LEFT + 8 + (index % COLUMNS) * 18;
    }

    public static int slotY(int index) {
        return SLOTS_TOP + (index / COLUMNS) * 18;
    }

    /** First id of the menu button clicks the panel buttons send (see {@code BackpackPanelMenuMixin}). */
    public static final int BUTTON_ID_BASE = 0x7B50;
    public static final int BUTTON_COUNT = 4;
    public static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_WIDTH = (COLUMNS * 18 - (BUTTON_COUNT - 1) * BUTTON_GAP) / BUTTON_COUNT;

    public static int height(int slots) {
        return slotsBottom(slots) + BUTTON_HEIGHT + 4 + 7;
    }

    private static int slotsBottom(int slots) {
        return SLOTS_TOP - 1 + (slots + COLUMNS - 1) / COLUMNS * 18;
    }

    public static int buttonX(int button) {
        return LEFT + 8 - 1 + button * (BUTTON_WIDTH + BUTTON_GAP);
    }

    public static int buttonY(int slots) {
        return slotsBottom(slots) + 4;
    }

    public static int buttonWidth() {
        return BUTTON_WIDTH;
    }

    /** The button under the mouse (coordinates relative to the screen's top left corner), or -1. */
    public static int buttonAt(double x, double y, int slots) {
        int top = buttonY(slots);
        if (y < top || y >= top + BUTTON_HEIGHT) {
            return -1;
        }
        for (int i = 0; i < BUTTON_COUNT; i++) {
            if (x >= buttonX(i) && x < buttonX(i) + BUTTON_WIDTH) {
                return i;
            }
        }
        return -1;
    }
}
