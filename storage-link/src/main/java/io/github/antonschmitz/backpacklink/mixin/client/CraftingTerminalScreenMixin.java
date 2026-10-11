package io.github.antonschmitz.backpacklink.mixin.client;

import com.tom.storagemod.screen.CraftingTerminalScreen;
import com.tom.storagemod.screen.widget.ToggleButton;
import io.github.antonschmitz.backpacklink.client.BackpackButtonHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The crafting terminal moves its buttons around (extra button, recipe book), so keep ours below the last one. */
@Mixin(CraftingTerminalScreen.class)
abstract class CraftingTerminalScreenMixin {
    @Inject(method = "setButtonsPos()V", at = @At("TAIL"))
    private void backpacklink$moveButton(CallbackInfo ci) {
        BackpackButtonHolder holder = (BackpackButtonHolder) (Object) this;
        ToggleButton button = holder.backpacklink$button();
        ToggleButton last = holder.backpacklink$tallModeButton();
        if (button != null && last != null) {
            button.setX(last.getX());
            button.setY(last.getY() + 18);
        }
    }
}
