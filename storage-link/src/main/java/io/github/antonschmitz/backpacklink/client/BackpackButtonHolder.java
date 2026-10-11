package io.github.antonschmitz.backpacklink.client;

import com.tom.storagemod.screen.widget.ToggleButton;

/** Added to Tom's terminal screens by AbstractStorageTerminalScreenMixin. */
public interface BackpackButtonHolder {
    ToggleButton backpacklink$button();

    ToggleButton backpacklink$tallModeButton();
}
