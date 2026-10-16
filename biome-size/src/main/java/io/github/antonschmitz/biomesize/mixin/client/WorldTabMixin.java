package io.github.antonschmitz.biomesize.mixin.client;

import io.github.antonschmitz.biomesize.client.BiomeSizeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a "World Shape..." button under the switches of the World tab (rows 0-2 are vanilla's). */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class WorldTabMixin {
    @Shadow
    @Final
    CreateWorldScreen this$0;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void biomesize$addButton(CallbackInfo ci) {
        GridLayout layout = ((GridLayoutTabAccessor) this).biomesize$layout();
        CreateWorldScreen parent = this$0;
        layout.addChild(Button.builder(Component.translatable("biomesize.button"),
                        button -> Minecraft.getInstance().gui.setScreen(new BiomeSizeScreen(parent)))
                .width(310)
                .tooltip(Tooltip.create(Component.translatable("biomesize.button.info")))
                .build(), 3, 0, 1, 2);
    }
}
