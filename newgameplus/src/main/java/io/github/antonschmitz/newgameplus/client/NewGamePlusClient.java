package io.github.antonschmitz.newgameplus.client;

import io.github.antonschmitz.newgameplus.ImportData;
import java.lang.ref.WeakReference;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

public class NewGamePlusClient implements ClientModInitializer {
    private static final int BUTTON_WIDTH = 150;

    /** What the player picked on the currently open Create World screen. */
    private static ImportData selection;
    private static WeakReference<CreateWorldScreen> selectionScreen = new WeakReference<>(null);

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof CreateWorldScreen createScreen) {
                addImportButton(createScreen, width, height);
            }
        });
    }

    public static ImportData selection(CreateWorldScreen screen) {
        return selectionScreen.get() == screen ? selection : null;
    }

    static void select(CreateWorldScreen screen, ImportData data) {
        selection = data;
        selectionScreen = new WeakReference<>(screen);
    }

    private static void addImportButton(CreateWorldScreen screen, int width, int height) {
        List<AbstractWidget> widgets = Screens.getWidgets(screen);
        // Sit to the left of the footer buttons when there is room, otherwise just above them.
        int footerLeft = width;
        int footerTop = height - 28;
        for (AbstractWidget widget : widgets) {
            if (widget instanceof Button && widget.getY() > height - 40) {
                footerLeft = Math.min(footerLeft, widget.getX());
                footerTop = widget.getY();
            }
        }
        int buttonWidth = Math.min(BUTTON_WIDTH, footerLeft - 16);
        int x = 8;
        int y = footerTop;
        if (buttonWidth < 100) {
            buttonWidth = BUTTON_WIDTH;
            x = (width - buttonWidth) / 2;
            y = footerTop - 24;
        }
        widgets.add(Button.builder(label(selection(screen)), button -> Screens.getMinecraft(screen)
                        .gui.setScreen(new ImportPlayerScreen(screen)))
                .bounds(x, y, buttonWidth, 20)
                .build());
    }

    static Component label(ImportData data) {
        if (data == null) {
            return Component.translatable("newgameplus.button.import");
        }
        int mobs = data.mobsToImport().size();
        return mobs == 0
                ? Component.translatable("newgameplus.button.importing", data.sourceName())
                : Component.translatable("newgameplus.button.importing_mobs", data.sourceName(), mobs);
    }
}
