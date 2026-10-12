package com.firemerald.additionalplacements.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;

public enum LoadedModsImpl {
    ;
    public static void populatePlatform() {
        for (LoadedModsImpl val : LoadedModsImpl.values()) {
            if (isModLoaded(val.modId)) {
                val.isPresent = true;
                val.whenDetected.run();
            }
        }
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public final String modId;
    public final Runnable whenDetected;
    public boolean isPresent;

    LoadedModsImpl(String modId, Runnable whenDetected) {
        this.modId = modId;
        this.whenDetected = whenDetected;
    }
}
