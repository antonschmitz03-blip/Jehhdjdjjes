package io.github.antonschmitz.tbaddons.mixin.client;

import io.github.antonschmitz.tbaddons.panel.BackpackPanelMenu;
import io.github.antonschmitz.tbaddons.panel.BackpackPanelRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the backpack panel next to furnace, hopper, dispenser and brewing stand screens (all 176 wide, so no widening). */
@Mixin({AbstractFurnaceScreen.class, HopperScreen.class, DispenserScreen.class, BrewingStandScreen.class})
public abstract class MachineScreenMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void tbaddons$drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        AbstractContainerMenu menu = screen.tbaddons$getMenu();
        if (menu instanceof BackpackPanelMenu panel && panel.tbaddons$panelWrapper() != null) {
            BackpackPanelRenderer.extractBackground(graphics, screen.tbaddons$getLeftPos(), screen.tbaddons$getTopPos(), panel, mouseX, mouseY);
        }
    }
}
