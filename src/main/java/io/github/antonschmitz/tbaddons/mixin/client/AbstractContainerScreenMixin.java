package io.github.antonschmitz.tbaddons.mixin.client;

import io.github.antonschmitz.tbaddons.panel.BackpackPanel;
import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import io.github.antonschmitz.tbaddons.panel.BackpackPanelRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Panel label, and clicks on the panel (it can be taller than the container screen) don't count as outside. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    @Shadow
    @Final
    protected T menu;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void tbaddons$panelLabel(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (this.menu instanceof BackpackPanelMenu panel && panel.tbaddons$panelWrapper() != null) {
            BackpackPanelRenderer.extractLabel(graphics, this.font, panel);
        }
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void tbaddons$insidePanel(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
        if (this.menu instanceof BackpackPanelMenu panel && panel.tbaddons$panelWrapper() != null) {
            int left = xo + BackpackPanel.LEFT;
            if (mx >= left && mx < left + BackpackPanel.WIDTH && my >= yo && my < yo + BackpackPanel.height(panel.tbaddons$panelSize())) {
                cir.setReturnValue(false);
            }
        }
    }
}
