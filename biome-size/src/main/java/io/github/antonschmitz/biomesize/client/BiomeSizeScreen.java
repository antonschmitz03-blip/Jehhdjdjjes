package io.github.antonschmitz.biomesize.client;

import io.github.antonschmitz.biomesize.BiomeSize;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** All Biome Size sliders, opened from the World tab of Create World. */
public class BiomeSizeScreen extends Screen {
    private static final int WIDTH = 150;
    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public BiomeSizeScreen(Screen parent) {
        super(Component.translatable("biomesize.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout.addToHeader(new StringWidget(getTitle(), font));
        GridLayout grid = new GridLayout().columnSpacing(10).rowSpacing(8);
        GridLayout.RowHelper rows = grid.createRowHelper(2);
        rows.addChild(slider(BiomeSize.MIN_SIZE, BiomeSize.MAX_SIZE, "biome_size", BiomeSizeClient.biomePercent, p -> BiomeSizeClient.biomePercent = p));
        rows.addChild(slider(BiomeSize.MIN_SIZE, BiomeSize.MAX_SIZE, "terrain_scale", BiomeSizeClient.terrainPercent, p -> BiomeSizeClient.terrainPercent = p));
        rows.addChild(slider(BiomeSize.MIN_DRAMATIC, BiomeSize.MAX_DRAMATIC, "dramatic", BiomeSizeClient.dramaticPercent, p -> BiomeSizeClient.dramaticPercent = p));
        rows.addChild(slider(BiomeSize.MIN_MOUNTAINS, BiomeSize.MAX_MOUNTAINS, "mountains", BiomeSizeClient.mountainPercent, p -> BiomeSizeClient.mountainPercent = p));
        rows.addChild(slider(BiomeSize.MIN_CAVE, BiomeSize.MAX_CAVE, "cave_size", BiomeSizeClient.cavePercent, p -> BiomeSizeClient.cavePercent = p));
        rows.addChild(slider(BiomeSize.MIN_FLUID, BiomeSize.MAX_FLUID, "water", BiomeSizeClient.waterPercent, p -> BiomeSizeClient.waterPercent = p));
        rows.addChild(slider(BiomeSize.MIN_FLUID, BiomeSize.MAX_FLUID, "lava", BiomeSizeClient.lavaPercent, p -> BiomeSizeClient.lavaPercent = p));
        layout.addToContents(grid);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    private static PercentSlider slider(int min, int max, String key, int value, java.util.function.IntConsumer onChange) {
        return new PercentSlider(WIDTH, min, max, "biomesize.slider." + key, "biomesize.slider." + key + ".info", value, onChange);
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
