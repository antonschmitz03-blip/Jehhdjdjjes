package io.github.antonschmitz.backpacklink.mixin.client;

import com.tom.storagemod.screen.AbstractStorageTerminalScreen;
import com.tom.storagemod.screen.widget.ToggleButton;
import io.github.antonschmitz.backpacklink.BackpackLink;
import io.github.antonschmitz.backpacklink.client.BackpackButtonHolder;
import io.github.antonschmitz.backpacklink.client.BackpackLinkClient;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the backpack toggle below Tom's own buttons on the left of every terminal screen. */
@Mixin(AbstractStorageTerminalScreen.class)
abstract class AbstractStorageTerminalScreenMixin implements BackpackButtonHolder {
    @Shadow
    protected ToggleButton buttonTallMode;

    @Unique
    private ToggleButton backpacklink$button;

    // init() starts with clearWidgets(), so the button is added again on every re-init (e.g. tall mode).
    @Inject(method = "init()V", at = @At("TAIL"))
    private void backpacklink$addButton(CallbackInfo ci) {
        ToggleButton button = ToggleButton.builder(buttonTallMode.getX(), buttonTallMode.getY() + 18)
                .name(Component.translatable("narrator.backpacklink.toggle"))
                .iconOff(BackpackLink.id("icons/backpack_off"))
                .iconOn(BackpackLink.id("icons/backpack_on"))
                .build(state -> {
                    backpacklink$button.setState(state);
                    BackpackLinkClient.setEnabled(state);
                });
        button.setTooltip(
                Tooltip.create(Component.translatable("tooltip.backpacklink.toggle_off")),
                Tooltip.create(Component.translatable("tooltip.backpacklink.toggle_on")));
        button.setState(BackpackLinkClient.enabled());
        backpacklink$button = ((ScreenInvoker) this).backpacklink$addRenderableWidget(button);
    }

    @Override
    public ToggleButton backpacklink$button() {
        return backpacklink$button;
    }

    @Override
    public ToggleButton backpacklink$tallModeButton() {
        return buttonTallMode;
    }
}
