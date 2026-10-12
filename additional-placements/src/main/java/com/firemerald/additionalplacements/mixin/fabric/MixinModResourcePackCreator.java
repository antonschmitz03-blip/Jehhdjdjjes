package com.firemerald.additionalplacements.mixin.fabric;

import java.util.function.Consumer;

import com.firemerald.additionalplacements.client.resources.APDynamicResources;
import net.fabricmc.fabric.impl.resource.pack.ModResourcePackCreator;
import com.firemerald.additionalplacements.util.PlatformUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.packs.repository.Pack;

@Mixin(ModResourcePackCreator.class)
public class MixinModResourcePackCreator {
    @Inject(method="loadPacks", at = @At("HEAD"))
    public void loadPacks(Consumer<Pack> consumer, CallbackInfo info) {
        if (PlatformUtils.isClient()) additionalplacements$loadClientPacks(consumer);
    }

    @Unique
    private void additionalplacements$loadClientPacks(Consumer<Pack> consumer) {
        consumer.accept(APDynamicResources.PACK);
    }
}
