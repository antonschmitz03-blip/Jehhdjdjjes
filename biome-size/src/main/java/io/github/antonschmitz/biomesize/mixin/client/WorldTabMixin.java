package io.github.antonschmitz.biomesize.mixin.client;

import io.github.antonschmitz.biomesize.client.BiomeSizeClient;
import io.github.antonschmitz.biomesize.client.PercentSlider;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the two sliders under the switches of the World tab (rows 0-2 are vanilla's). */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class WorldTabMixin {
    private static final int WIDTH = 310;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void biomesize$addSliders(CallbackInfo ci) {
        GridLayout layout = ((GridLayoutTabAccessor) this).biomesize$layout();
        layout.addChild(new PercentSlider(WIDTH, "biomesize.slider.biome_size", "biomesize.slider.biome_size.info",
                BiomeSizeClient.biomePercent, percent -> BiomeSizeClient.biomePercent = percent), 3, 0, 1, 2);
        layout.addChild(new PercentSlider(WIDTH, "biomesize.slider.terrain_scale", "biomesize.slider.terrain_scale.info",
                BiomeSizeClient.terrainPercent, percent -> BiomeSizeClient.terrainPercent = percent), 4, 0, 1, 2);
    }
}
