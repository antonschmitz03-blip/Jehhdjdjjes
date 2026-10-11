package io.github.antonschmitz.tbaddons.mixin.client;

import io.github.antonschmitz.tbaddons.panel.BackpackPanel;
import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import io.github.antonschmitz.tbaddons.panel.BackpackPanelRenderer;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Widens chest and shulker box screens by the backpack panel (so the screen stays centered and JEI keeps clear
 * of it), keeps the vanilla background at its normal width and draws the panel next to it.
 */
@Mixin({ContainerScreen.class, ShulkerBoxScreen.class})
public abstract class PanelScreenMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void tbaddons$widen(CallbackInfo ci) {
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        if (this.tbaddons$panel() != null) {
            screen.tbaddons$setImageWidth(BackpackPanel.LEFT + BackpackPanel.WIDTH);
        }
    }

    @ModifyArg(method = "extractBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"),
            index = 6)
    private int tbaddons$vanillaWidth(int width) {
        return this.tbaddons$panel() != null ? BackpackPanel.CONTAINER_WIDTH : width;
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void tbaddons$drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        BackpackPanelMenu panel = this.tbaddons$panel();
        if (panel != null) {
            AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
            BackpackPanelRenderer.extractBackground(graphics, screen.tbaddons$getLeftPos(), screen.tbaddons$getTopPos(), panel);
        }
    }

    @Unique
    @Nullable
    private BackpackPanelMenu tbaddons$panel() {
        AbstractContainerMenu menu = ((AbstractContainerScreenAccessor) this).tbaddons$getMenu();
        return menu instanceof BackpackPanelMenu panel && panel.tbaddons$panelWrapper() != null ? panel : null;
    }
}
