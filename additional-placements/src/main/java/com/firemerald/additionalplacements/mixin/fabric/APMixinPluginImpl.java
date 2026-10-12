package com.firemerald.additionalplacements.mixin.fabric;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class APMixinPluginImpl {
    public static List<String> getPlatformMixins() {
        List<String> mixins = new ArrayList<>();
        mixins.add("fabric.MixinModResourcePackCreator");
        //the model dispatcher only exists on the client
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) mixins.add("fabric.MixinBlockStateModelDispatcher");
        return mixins;
    }
}
