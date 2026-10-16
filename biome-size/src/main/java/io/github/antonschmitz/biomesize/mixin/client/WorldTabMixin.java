package io.github.antonschmitz.biomesize.mixin.client;

import io.github.antonschmitz.biomesize.BiomeSize;
import io.github.antonschmitz.biomesize.client.BiomeSizeClient;
import io.github.antonschmitz.biomesize.client.PercentSlider;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds four sliders in two rows under the switches of the World tab (rows 0-2 are vanilla's). */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class WorldTabMixin {
    private static final int WIDTH = 150;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void biomesize$addSliders(CallbackInfo ci) {
        GridLayout layout = ((GridLayoutTabAccessor) this).biomesize$layout();
        layout.addChild(new PercentSlider(WIDTH, BiomeSize.MIN_SIZE, BiomeSize.MAX_SIZE,
                "biomesize.slider.biome_size", "biomesize.slider.biome_size.info",
                BiomeSizeClient.biomePercent, p -> BiomeSizeClient.biomePercent = p), 3, 0);
        layout.addChild(new PercentSlider(WIDTH, BiomeSize.MIN_SIZE, BiomeSize.MAX_SIZE,
                "biomesize.slider.terrain_scale", "biomesize.slider.terrain_scale.info",
                BiomeSizeClient.terrainPercent, p -> BiomeSizeClient.terrainPercent = p), 3, 1);
        layout.addChild(new PercentSlider(WIDTH, BiomeSize.MIN_DRAMATIC, BiomeSize.MAX_DRAMATIC,
                "biomesize.slider.dramatic", "biomesize.slider.dramatic.info",
                BiomeSizeClient.dramaticPercent, p -> BiomeSizeClient.dramaticPercent = p), 4, 0);
        layout.addChild(new PercentSlider(WIDTH, BiomeSize.MIN_CAVE, BiomeSize.MAX_CAVE,
                "biomesize.slider.cave_size", "biomesize.slider.cave_size.info",
                BiomeSizeClient.cavePercent, p -> BiomeSizeClient.cavePercent = p), 4, 1);
    }
}
