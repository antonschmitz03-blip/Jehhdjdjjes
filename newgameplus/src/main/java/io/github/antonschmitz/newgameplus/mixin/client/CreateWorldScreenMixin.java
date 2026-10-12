package io.github.antonschmitz.newgameplus.mixin.client;

import io.github.antonschmitz.newgameplus.ImportData;
import io.github.antonschmitz.newgameplus.NewGamePlus;
import io.github.antonschmitz.newgameplus.client.NewGamePlusClient;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only runs once the world is really being created (after any experimental-features warning). */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Inject(method = "createNewWorld", at = @At("HEAD"))
    private void newgameplus$armImport(CallbackInfoReturnable<Boolean> cir) {
        ImportData data = NewGamePlusClient.selection((CreateWorldScreen) (Object) this);
        NewGamePlus.armed = data;
    }
}
