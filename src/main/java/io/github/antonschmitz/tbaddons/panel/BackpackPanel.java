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

    public static int height(int slots) {
        return SLOTS_TOP - 1 + (slots + COLUMNS - 1) / COLUMNS * 18 + 7;
    }
}
